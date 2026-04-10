---
inclusion: manual
---

# 自定义模型交互指南

本文档介绍如何在 Halo 插件中与自定义模型（Extension）进行交互，包括 CRUD 操作、查询、索引等。

## 概述

Halo 提供两个客户端用于与自定义模型交互：

| 客户端 | 类型 | 适用场景 |
|--------|------|----------|
| `ReactiveExtensionClient` | 响应式（Mono/Flux） | WebFlux 环境，推荐使用 |
| `ExtensionClient` | 阻塞式 | 后台任务、Reconciler |

> **重要**：非阻塞线程中不能调用阻塞式方法，推荐优先使用 `ReactiveExtensionClient`。

## 基本 CRUD 操作

### 注入客户端

```java
@Service
@RequiredArgsConstructor
public class PersonService {
    private final ReactiveExtensionClient client;
    // 或
    private final ExtensionClient blockingClient;
}
```

### 创建

```java
// 响应式
public Mono<Person> createPerson(Person person) {
    return client.create(person);
}

// 阻塞式
public void createPerson(Person person) {
    blockingClient.create(person);
}
```

### 查询单个

```java
// fetch - 返回 Optional/Mono.empty()（不存在时）
public Mono<Person> getPerson(String name) {
    return client.fetch(Person.class, name);
}

// get - 不存在时抛出异常
public Mono<Person> getPersonOrThrow(String name) {
    return client.get(Person.class, name);
}
```

### 更新

```java
public Mono<Person> updatePerson(Person person) {
    return client.update(person);
}

// 先查询再更新的模式
public Mono<Person> updatePersonName(String name, String newName) {
    return client.fetch(Person.class, name)
        .flatMap(person -> {
            person.getSpec().setName(newName);
            return client.update(person);
        });
}
```

### 删除

```java
public Mono<Person> deletePerson(Person person) {
    return client.delete(person);
}

// 根据名称删除
public Mono<Person> deletePersonByName(String name) {
    return client.fetch(Person.class, name)
        .flatMap(client::delete);
}
```

## 列表查询

### 查询方法

| 方法 | 说明 |
|------|------|
| `listBy` | 分页查询，返回 `ListResult<E>` |
| `listAll` | 查询所有，返回 `Flux<E>` |
| `listAllNames` | 查询所有名称，返回 `Flux<String>` |
| `countBy` | 统计数量 |

### ListOptions 构建

`ListOptions` 包含两部分：
- `LabelSelector` - 标签查询
- `FieldSelector` - 字段查询（需要索引支持）

```java
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.index.query.Queries;

// 基本构建
ListOptions options = ListOptions.builder()
    .labelSelector()
        .eq("category", "tech")
        .exists("featured")
    .end()
    .fieldQuery(Queries.equal("spec.status", "published"))
    .build();

// 组合查询
ListOptions options = ListOptions.builder()
    .fieldQuery(Queries.and(
        Queries.equal("spec.author", "admin"),
        Queries.greaterThan("spec.views", 100)
    ))
    .build();
```

### Queries 工具类（2.22.0+）

> **注意**：`QueryFactory` 在 2.22.0 已废弃，请使用 `Queries` 类。

```java
import run.halo.app.extension.index.query.Queries;

// 等于
Queries.equal("spec.slug", "hello-world")

// 不等于
Queries.notEqual("spec.status", "draft")

// 大于/小于
Queries.greaterThan("spec.priority", 10)
Queries.greaterThan("spec.priority", 10, true)  // 包含边界
Queries.lessThan("spec.order", 100)
Queries.lessThan("spec.order", 100, true)       // 包含边界

// 范围查询
Queries.between("spec.price", 10, true, 100, true)  // [10, 100]
Queries.between("spec.price", 10, false, 100, false) // (10, 100)

// IN 查询
Queries.in("spec.category", "tech", "news", "blog")
Queries.in("spec.tags", Set.of("java", "spring"))

// 字符串匹配
Queries.startsWith("spec.title", "Hello")
Queries.endsWith("spec.slug", "-post")
Queries.contains("spec.content", "keyword")

// 空值检查
Queries.isNull("spec.deletedAt")

// 逻辑组合
Queries.and(condition1, condition2, condition3)
Queries.or(condition1, condition2)
Queries.not(condition)

// 标签查询
Queries.labelExists("halo.run/featured")
Queries.labelEqual("category", "tech")
Queries.labelIn("status", List.of("published", "pending"))
```

### 分页与排序

```java
import run.halo.app.extension.PageRequestImpl;
import org.springframework.data.domain.Sort;

// 分页
PageRequest pageRequest = PageRequestImpl.of(1, 10);  // 第1页，每页10条

// 带排序的分页
PageRequest pageRequest = PageRequestImpl.of(1, 10, 
    Sort.by(Sort.Order.desc("metadata.creationTimestamp")));

// 仅指定每页数量
PageRequest pageRequest = PageRequestImpl.ofSize(20);

// 排序（用于 listAll）
Sort sort = Sort.by(
    Sort.Order.desc("spec.priority"),
    Sort.Order.asc("metadata.name")
);
```

### 完整查询示例

```java
public Mono<ListResult<Person>> listPersons(int page, int size, String keyword) {
    var options = ListOptions.builder()
        .fieldQuery(Queries.and(
            Queries.equal("spec.status", "active"),
            keyword != null 
                ? Queries.contains("spec.name", keyword) 
                : Queries.empty()
        ))
        .build();
    
    var pageRequest = PageRequestImpl.of(page, size,
        Sort.by(Sort.Order.desc("metadata.creationTimestamp")));
    
    return client.listBy(Person.class, options, pageRequest);
}
```

## 索引

### 为什么需要索引

Halo 使用 `byte[]` 存储数据以支持多种数据库，无法利用数据库原生索引。通过声明索引可以：
- 提高查询效率
- 减少内存开销
- 支持字段查询（`FieldSelector`）

> **重要**：`FieldSelector` 中使用的字段必须添加为索引，否则会抛出异常。

### 默认索引

Halo 自动为每个自定义模型创建以下索引：

- `metadata.name` - 唯一索引
- `metadata.labels` - 标签索引
- `metadata.creationTimestamp` - 创建时间索引
- `metadata.deletionTimestamp` - 删除时间索引

### 声明索引

在注册自定义模型时声明索引：

```java
import static run.halo.app.extension.index.IndexAttributeFactory.multiValueAttribute;
import static run.halo.app.extension.index.IndexAttributeFactory.simpleAttribute;

@Override
public void start() {
    schemeManager.register(Person.class, indexSpecs -> {
        // 单值索引
        indexSpecs.add(new IndexSpec()
            .setName("spec.slug")
            .setIndexFunc(simpleAttribute(Person.class, 
                person -> person.getSpec().getSlug())));
        
        // 多值索引（如标签、分类）
        indexSpecs.add(new IndexSpec()
            .setName("spec.tags")
            .setIndexFunc(multiValueAttribute(Person.class, person -> {
                var tags = person.getSpec().getTags();
                return tags == null ? Set.of() : tags;
            })));
        
        // 唯一索引
        indexSpecs.add(new IndexSpec()
            .setName("spec.email")
            .setUnique(true)
            .setIndexFunc(simpleAttribute(Person.class,
                person -> person.getSpec().getEmail())));
        
        // 指定排序方式
        indexSpecs.add(new IndexSpec()
            .setName("spec.priority")
            .setOrder(IndexSpec.OrderType.DESC)
            .setIndexFunc(simpleAttribute(Person.class,
                person -> String.valueOf(person.getSpec().getPriority()))));
    });
}
```

### IndexSpec 属性

| 属性 | 说明 |
|------|------|
| `name` | 索引名称，建议使用字段路径如 `spec.slug` |
| `indexFunc` | 索引函数，返回索引值 |
| `order` | 排序方式：`ASC`（默认）或 `DESC` |
| `unique` | 是否唯一索引 |

### IndexAttributeFactory

```java
// 单值索引 - 返回单个值，可为 null
simpleAttribute(Class<E> type, Function<E, String> getter)

// 多值索引 - 返回多个值的集合
multiValueAttribute(Class<E> type, Function<E, Set<String>> getter)
```

## 自定义查询请求

### 继承 SortableRequest

```java
import run.halo.app.extension.router.SortableRequest;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.index.query.Queries;

public class PersonQuery extends SortableRequest {

    public PersonQuery(ServerWebExchange exchange) {
        super(exchange);
    }

    // 自定义查询参数
    public String getKeyword() {
        return queryParams.getFirst("keyword");
    }

    public String getStatus() {
        return queryParams.getFirst("status");
    }

    @Override
    public ListOptions toListOptions() {
        var builder = ListOptions.builder(super.toListOptions());
        
        var keyword = getKeyword();
        if (StringUtils.hasText(keyword)) {
            builder.andQuery(Queries.or(
                Queries.contains("spec.name", keyword),
                Queries.contains("spec.email", keyword)
            ));
        }
        
        var status = getStatus();
        if (StringUtils.hasText(status)) {
            builder.andQuery(Queries.equal("spec.status", status));
        }
        
        return builder.build();
    }
}
```

### 在 Endpoint 中使用

```java
@Component
public class PersonEndpoint implements CustomEndpoint {
    
    private final ReactiveExtensionClient client;

    public Mono<ServerResponse> listPersons(ServerRequest request) {
        var query = new PersonQuery(request.exchange());
        return client.listBy(Person.class, query.toListOptions(), query.toPageRequest())
            .flatMap(result -> ServerResponse.ok().bodyValue(result));
    }
}
```

## 监听变更

### 使用 Watcher

```java
client.watch(new Watcher() {
    @Override
    public void onAdd(Extension extension) {
        if (extension instanceof Person person) {
            // 处理新增
        }
    }

    @Override
    public void onUpdate(Extension oldExtension, Extension newExtension) {
        // 处理更新
    }

    @Override
    public void onDelete(Extension extension) {
        // 处理删除
    }
});
```

## 最佳实践

### 1. 优先使用响应式客户端

```java
// 推荐
private final ReactiveExtensionClient client;

// 仅在 Reconciler 等阻塞场景使用
private final ExtensionClient blockingClient;
```

### 2. 合理使用索引

```java
// 只为需要查询的字段创建索引
// 避免过多索引导致性能问题
indexSpecs.add(new IndexSpec()
    .setName("spec.slug")  // 经常用于查询
    .setIndexFunc(...));

// 不需要查询的字段使用 annotations 存储
person.getMetadata().getAnnotations().put("extra-data", jsonData);
```

### 3. 查询优化

```java
// 使用 listAllNames 减少内存占用
client.listAllNames(Person.class, options, sort)
    .flatMap(name -> client.fetch(Person.class, name))
    .filter(person -> /* 复杂过滤 */)
    .collectList();

// 使用 countBy 统计数量
client.countBy(Person.class, options);
```

### 4. 乐观锁处理

```java
public Mono<Person> updateWithRetry(String name, Consumer<Person> updater) {
    return client.fetch(Person.class, name)
        .flatMap(person -> {
            updater.accept(person);
            return client.update(person);
        })
        .retryWhen(Retry.backoff(3, Duration.ofMillis(100))
            .filter(e -> e instanceof OptimisticLockingFailureException));
}
```

### 5. 批量操作

```java
public Flux<Person> batchUpdate(List<String> names, Consumer<Person> updater) {
    return Flux.fromIterable(names)
        .flatMap(name -> client.fetch(Person.class, name))
        .flatMap(person -> {
            updater.accept(person);
            return client.update(person);
        }, 5);  // 并发数限制
}
```

## API 参考

### ReactiveExtensionClient 方法

| 方法 | 返回类型 | 说明 |
|------|----------|------|
| `fetch(Class<E>, String)` | `Mono<E>` | 根据名称查询，不存在返回空 |
| `get(Class<E>, String)` | `Mono<E>` | 根据名称查询，不存在抛异常 |
| `create(E)` | `Mono<E>` | 创建 |
| `update(E)` | `Mono<E>` | 更新 |
| `delete(E)` | `Mono<E>` | 删除 |
| `listBy(Class<E>, ListOptions, PageRequest)` | `Mono<ListResult<E>>` | 分页查询 |
| `listAll(Class<E>, ListOptions, Sort)` | `Flux<E>` | 查询所有 |
| `listAllNames(Class<E>, ListOptions, Sort)` | `Flux<String>` | 查询所有名称 |
| `countBy(Class<E>, ListOptions)` | `Mono<Long>` | 统计数量 |
| `watch(Watcher)` | `void` | 监听变更 |

### ExtensionClient 方法

与 `ReactiveExtensionClient` 类似，但返回类型为同步类型：
- `Mono<E>` → `Optional<E>` 或 `E`
- `Flux<E>` → `List<E>`
