---
inclusion: manual
---

# 控制器（Reconciler）开发指南

本文档介绍如何在 Halo 插件中编写控制器来处理自定义模型的业务逻辑。

## 概述

控制器是 Halo 的核心组件，负责协调自定义模型对象的期望状态（spec）和实际状态（status）。

**控制循环：**
1. **观察** - 监听自定义模型对象的变化
2. **比较** - 对比期望状态与当前状态
3. **操作** - 执行必要的操作使实际状态趋向期望状态
4. **重复** - 持续循环直到状态一致

## 基本结构

### 实现 Reconciler 接口

```java
import run.halo.app.extension.ExtensionClient;
import run.halo.app.extension.controller.Controller;
import run.halo.app.extension.controller.ControllerBuilder;
import run.halo.app.extension.controller.Reconciler;

@Component
@RequiredArgsConstructor
public class PersonReconciler implements Reconciler<Reconciler.Request> {

    private final ExtensionClient client;

    @Override
    public Result reconcile(Request request) {
        // 调谐逻辑
        return Result.doNotRetry();
    }

    @Override
    public Controller setupWith(ControllerBuilder builder) {
        return builder
            .extension(new Person())
            .build();
    }
}
```

### Reconciler 接口

```java
public interface Reconciler<R> {
    
    // 调谐方法，处理业务逻辑
    Result reconcile(R request);
    
    // 构建控制器
    Controller setupWith(ControllerBuilder builder);
    
    // 请求对象，包含变更对象的名称
    record Request(String name) {}
    
    // 返回结果
    record Result(boolean reEnqueue, Duration retryAfter) {
        // 不重试
        public static Result doNotRetry() {
            return new Result(false, null);
        }
        // 指定时间后重试
        public static Result requeue(Duration retryAfter) {
            return new Result(true, retryAfter);
        }
    }
}
```

## ControllerBuilder 配置

```java
@Override
public Controller setupWith(ControllerBuilder builder) {
    return builder
        .extension(new Person())           // 必需：要观察的自定义模型
        .minDelay(Duration.ofMillis(5))    // 最小重试间隔，默认 5ms
        .maxDelay(Duration.ofSeconds(1000)) // 最大重试间隔，默认 1000s
        .workerCount(1)                    // 工作线程数，默认 1
        .syncAllOnStart(true)              // 启动时同步所有对象，默认 true
        .syncAllListOptions(listOptions)   // 启动时同步的查询条件
        .onAddMatcher(matcher)             // 添加事件匹配器
        .onUpdateMatcher(matcher)          // 更新事件匹配器
        .onDeleteMatcher(matcher)          // 删除事件匹配器
        .build();
}
```

### ControllerBuilder 属性说明

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `extension` | `Extension` | 必需 | 要观察的自定义模型对象 |
| `minDelay` | `Duration` | 5ms | 最小重试间隔 |
| `maxDelay` | `Duration` | 1000s | 最大重试间隔 |
| `workerCount` | `int` | 1 | 工作线程数 |
| `syncAllOnStart` | `boolean` | true | 启动时是否同步所有对象 |
| `syncAllListOptions` | `ListOptions` | null | 启动时同步的查询条件 |
| `onAddMatcher` | `ExtensionMatcher` | null | 添加事件匹配器 |
| `onUpdateMatcher` | `ExtensionMatcher` | null | 更新事件匹配器 |
| `onDeleteMatcher` | `ExtensionMatcher` | null | 删除事件匹配器 |

## ExtensionMatcher

用于过滤需要处理的事件：

```java
@FunctionalInterface
public interface ExtensionMatcher {
    boolean match(Extension extension);
}
```

### 示例：只处理特定状态的对象

```java
@Override
public Controller setupWith(ControllerBuilder builder) {
    // 只处理 visible 为 PUBLIC 的文章
    ExtensionMatcher matcher = extension -> {
        var post = (Post) extension;
        return VisibleEnum.PUBLIC.equals(post.getSpec().getVisible());
    };
    
    return builder
        .extension(new Post())
        .onAddMatcher(matcher)
        .onUpdateMatcher(matcher)
        .onDeleteMatcher(matcher)
        .build();
}
```

### 示例：控制启动时同步范围

```java
@Override
public Controller setupWith(ControllerBuilder builder) {
    return builder
        .extension(new Post())
        .syncAllOnStart(true)
        .syncAllListOptions(ListOptions.builder()
            .fieldQuery(Queries.equal("spec.owner", "admin"))
            .build())
        .build();
}
```

## Reconcile 返回值

| 返回值 | 说明 |
|--------|------|
| `Result.doNotRetry()` | 成功，不重试 |
| `Result.requeue(Duration)` | 指定时间后重试 |
| `new Result(true, null)` | 使用默认策略重试 |
| `null` | 等同于 `doNotRetry()` |

### 异常处理

当 `reconcile()` 抛出异常时：
- 异常会被记录到日志
- 请求会重新入队等待重试
- 重试间隔会逐渐增加，直到达到 `maxDelay`

## 完整示例

### 基本 Reconciler

```java
@Component
@RequiredArgsConstructor
public class PersonReconciler implements Reconciler<Reconciler.Request> {

    private static final String FINALIZER_NAME = "person.my-plugin.halo.run/finalizer";
    
    private final ExtensionClient client;

    @Override
    public Result reconcile(Request request) {
        client.fetch(Person.class, request.name())
            .ifPresent(person -> {
                // 1. 检查是否被删除
                if (ExtensionUtil.isDeleted(person)) {
                    if (ExtensionUtil.removeFinalizers(person.getMetadata(), 
                            Set.of(FINALIZER_NAME))) {
                        // 执行清理操作
                        cleanupResources(person);
                        client.update(person);
                    }
                    return;
                }
                
                // 2. 添加 Finalizer
                ExtensionUtil.addFinalizers(person.getMetadata(), Set.of(FINALIZER_NAME));
                
                // 3. 初始化 Status
                if (person.getStatus() == null) {
                    person.setStatus(new Person.Status());
                }
                var status = person.getStatus();
                
                // 4. 业务逻辑处理
                processBusinessLogic(person);
                
                // 5. 更新状态
                status.setPhase("Ready");
                status.setObservedVersion(person.getMetadata().getVersion() + 1);
                
                // 6. 保存更新
                client.update(person);
            });
        
        return Result.doNotRetry();
    }

    @Override
    public Controller setupWith(ControllerBuilder builder) {
        return builder
            .extension(new Person())
            .build();
    }
    
    private void cleanupResources(Person person) {
        // 清理外部资源
    }
    
    private void processBusinessLogic(Person person) {
        // 处理业务逻辑
    }
}
```

## Finalizers（终结器）

Finalizers 用于实现异步预删除钩子，确保在对象被真正删除前完成清理工作。

### 工作原理

1. 用户删除对象时，`metadata.deletionTimestamp` 被设置
2. 对象进入 `DELETING` 状态，但不会被真正删除
3. 控制器检测到删除，执行清理操作
4. 清理完成后移除 Finalizer
5. 所有 Finalizers 被移除后，对象才会被真正删除

### 使用模式

```java
private static final String FINALIZER_NAME = "my-plugin.halo.run/finalizer";

@Override
public Result reconcile(Request request) {
    client.fetch(MyResource.class, request.name())
        .ifPresent(resource -> {
            // 1. 检查是否被删除
            if (ExtensionUtil.isDeleted(resource)) {
                // 2. 移除 Finalizer 并执行清理
                if (ExtensionUtil.removeFinalizers(resource.getMetadata(), 
                        Set.of(FINALIZER_NAME))) {
                    cleanupExternalResources(resource);
                    client.update(resource);
                }
                return;  // 提前返回，不执行后续逻辑
            }
            
            // 3. 添加 Finalizer（在正常处理逻辑之前）
            ExtensionUtil.addFinalizers(resource.getMetadata(), Set.of(FINALIZER_NAME));
            
            // 4. 正常业务逻辑
            // ...
            
            client.update(resource);
        });
    
    return Result.doNotRetry();
}
```

### Finalizer 命名规范

建议使用 `{group}/finalizer` 格式，避免与其他插件冲突：

```java
// 推荐
private static final String FINALIZER_NAME = "my-plugin.halo.run/finalizer";

// 或更具体的名称
private static final String FINALIZER_NAME = "my-plugin.halo.run/cleanup-storage";
```

### ExtensionUtil 工具方法

```java
// 检查对象是否被删除
ExtensionUtil.isDeleted(extension)

// 添加 Finalizers，返回是否有变更
ExtensionUtil.addFinalizers(metadata, Set.of("finalizer-name"))

// 移除 Finalizers，返回是否有变更
ExtensionUtil.removeFinalizers(metadata, Set.of("finalizer-name"))

// 查询未删除的对象
ExtensionUtil.notDeleting()  // 返回 Query

// 默认排序
ExtensionUtil.defaultSort()  // 按创建时间降序，名称升序
```

## Condition（状态条件）

用于记录对象的状态信息，便于在 UI 展示。

### Condition 结构

```java
@Data
@Builder
public class Condition {
    private String type;           // 条件类型，如 "Ready", "Invalid"
    private ConditionStatus status; // 状态：TRUE, FALSE, UNKNOWN
    private Instant lastTransitionTime; // 最后转换时间
    private String message;        // 详细信息
    private String reason;         // 原因，驼峰命名
}

public enum ConditionStatus {
    TRUE, FALSE, UNKNOWN
}
```

### 使用 ConditionList

```java
@Override
public Result reconcile(Request request) {
    client.fetch(Person.class, request.name())
        .ifPresent(person -> {
            var status = person.getStatusOrDefault();
            
            // 获取或初始化 ConditionList
            if (status.getConditions() == null) {
                status.setConditions(new ConditionList());
            }
            
            // 校验并添加条件
            if (StringUtils.isBlank(person.getSpec().getName())) {
                status.getConditions().addAndEvictFIFO(Condition.builder()
                    .type("Invalid")
                    .status(ConditionStatus.TRUE)
                    .reason("InvalidName")
                    .message("Name cannot be empty")
                    .lastTransitionTime(Instant.now())
                    .build());
            } else {
                status.getConditions().addAndEvictFIFO(Condition.builder()
                    .type("Ready")
                    .status(ConditionStatus.TRUE)
                    .reason("Validated")
                    .message("All validations passed")
                    .lastTransitionTime(Instant.now())
                    .build());
            }
            
            client.update(person);
        });
    
    return Result.doNotRetry();
}
```

### ConditionList 方法

| 方法 | 说明 |
|------|------|
| `add(Condition)` | 添加条件到末尾 |
| `addFirst(Condition)` | 添加条件到开头 |
| `addAndEvictFIFO(Condition)` | 添加并自动淘汰旧条件（默认保留 20 条） |
| `addAndEvictFIFO(Condition, threshold)` | 添加并指定淘汰阈值 |
| `peek()` / `peekFirst()` | 获取第一个条件 |
| `remove(Condition)` | 移除指定条件 |
| `clear()` | 清空所有条件 |

## 自定义模型生命周期

```
                              +---- object
                              |     updated
                              v        |
                       +----------+    |
                       |          +----+
      object --------->|  ACTIVE  |
      created          |          +-----------+
         |             +---+------+           |
         |                 |                  |
         |                 |                  |
+--------+---+             |                  |
|            |     object deleted             |
|            |<--- without finalizers         |
|            |                           object deleted
| NOT_EXIST  |                           with finalizers
|            |                                |
|            |<--- finalizers removed         |
|            |             |                  |
+------------+             |                  |
                           |                  |
                       +---+------+           |
                       |          |           |
                       | DELETING |<----------+
                       |          |
                       +----------+
```

**状态说明：**
- `NOT_EXIST` - 对象不存在
- `ACTIVE` - 对象存在且未被删除
- `DELETING` - 对象已被标记删除，等待 Finalizers 清理

## 最佳实践

### 1. 幂等性

确保 `reconcile()` 方法是幂等的，多次执行结果一致：

```java
// 好的做法：检查状态再更新
if (status.getPhase() == null) {
    status.setPhase("Pending");
}

// 避免：每次都设置新值
status.setPhase("Pending");  // 可能导致不必要的更新
```

### 2. 一个控制器一个 Finalizer

```java
// 推荐：每个控制器最多一个 Finalizer
private static final String FINALIZER_NAME = "my-plugin.halo.run/finalizer";
```

### 3. 及时返回

删除逻辑处理完后立即返回，避免执行后续逻辑：

```java
if (ExtensionUtil.isDeleted(resource)) {
    // 处理删除...
    return;  // 重要：提前返回
}
// 正常逻辑...
```

### 4. 更新 observedVersion

用于追踪处理进度：

```java
status.setObservedVersion(resource.getMetadata().getVersion() + 1);
```

### 5. 合理使用 syncAllListOptions

缩小启动时同步范围，提高性能：

```java
.syncAllListOptions(ListOptions.builder()
    .fieldQuery(Queries.equal("spec.enabled", true))
    .build())
```

### 6. 错误处理

使用 Condition 记录错误，而不是抛出异常：

```java
try {
    processExternalService(resource);
    status.getConditions().addAndEvictFIFO(Condition.builder()
        .type("ExternalServiceReady")
        .status(ConditionStatus.TRUE)
        .build());
} catch (Exception e) {
    status.getConditions().addAndEvictFIFO(Condition.builder()
        .type("ExternalServiceReady")
        .status(ConditionStatus.FALSE)
        .reason("ServiceError")
        .message(e.getMessage())
        .build());
    // 返回重试
    return Result.requeue(Duration.ofSeconds(30));
}
```

## 参考

- [Kubernetes Controller 设计模式](https://kubernetes.io/docs/concepts/architecture/controller/)
- [Halo 控制器概述](https://docs.halo.run/developer-guide/core/framework#controller)
