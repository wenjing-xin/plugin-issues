# Halo 自定义模型定义指南

本文档介绍如何在 Halo 插件中定义和注册自定义数据模型（Extension）。

## 概述

Halo 自定义模型参考 Kubernetes CRD 设计，提供灵活可扩展的数据存储方式。自定义模型遵循 OpenAPI v3 规范，支持多种数据库（MySQL、PostgreSQL、H2）作为存储介质。

## 创建自定义模型

### 1. 继承 AbstractExtension

```java
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import run.halo.app.extension.AbstractExtension;
import run.halo.app.extension.GVK;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@GVK(group = "my-plugin.halo.run",
     version = "v1alpha1",
     kind = "Person",
     plural = "persons",
     singular = "person")
public class Person extends AbstractExtension {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Spec spec;

    private Status status;

    @Data
    @Schema(name = "PersonSpec")
    public static class Spec {
        @Schema(description = "姓名", maxLength = 100)
        private String name;

        @Schema(description = "年龄", maximum = "150", minimum = "0")
        private Integer age;

        @Schema(description = "性别")
        private Gender gender;

        @Schema(description = "邮箱", format = "email")
        private String email;
    }

    @Data
    @Schema(name = "PersonStatus")
    public static class Status {
        private String phase;
        private String message;
    }

    public enum Gender {
        MALE, FEMALE
    }
}
```

### 2. GVK 注解详解

| 属性 | 说明 | 示例 |
|-----|------|------|
| `group` | API 组名，建议使用 `{plugin-name}.halo.run` | `my-plugin.halo.run` |
| `version` | API 版本 | `v1alpha1` |
| `kind` | 资源类型名称 | `Person` |
| `plural` | 复数形式，用于 API 路径 | `persons` |
| `singular` | 单数形式 | `person` |

**命名规范：**
- `group`: 使用反向域名格式
- `kind`: 首字母大写的驼峰命名
- `plural`/`singular`: 全部小写

### 3. 注册自定义模型

在插件的 `start()` 方法中注册：

```java
@Component
@RequiredArgsConstructor
public class MyPlugin extends BasePlugin {

    private final SchemeManager schemeManager;

    @Override
    public void start() {
        // 简单注册
        schemeManager.register(Person.class);
    }

    @Override
    public void stop() {
        // 插件停止时会自动注销
    }
}
```

## 模型结构

自定义模型包含以下部分：

```
Extension
├── apiVersion    # 由 GVK 的 group/version 组成
├── kind          # 由 GVK 的 kind 定义
├── metadata      # 元数据（自动包含）
├── spec          # 期望状态（用户定义）
└── status        # 实际状态（用户定义）
```

### Metadata 元数据

`AbstractExtension` 已包含 `metadata`，包含以下属性：

| 属性 | 类型 | 说明 |
|-----|------|------|
| `name` | String | 唯一标识名，不超过 253 字符，只能包含小写字母、数字和 `-` |
| `generateName` | String | 自动生成名称的前缀 |
| `labels` | Map<String, String> | 标签，用于查询和分类 |
| `annotations` | Map<String, String> | 扩展信息存储 |
| `version` | Long | 乐观锁版本号，自动管理 |
| `creationTimestamp` | Instant | 创建时间，自动生成 |
| `deletionTimestamp` | Instant | 删除时间戳，标记删除状态 |
| `finalizers` | Set<String> | 终结器，用于删除前清理 |

### Spec 与 Status

- **spec**: 声明期望状态，由用户定义
- **status**: 描述实际状态，由 Reconciler 维护

```java
@Data
public static class Spec {
    private String title;
    private String content;
    private Boolean published;
}

@Data
public static class Status {
    private String phase;           // 当前阶段
    private ConditionList conditions; // 状态条件列表
    private Instant lastSyncTime;   // 最后同步时间
}
```

## 字段校验（@Schema）

使用 OpenAPI `@Schema` 注解定义字段校验规则：

```java
@Data
public static class Spec {
    
    // 必填字段
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;
    
    // 字符串长度限制
    @Schema(minLength = 1, maxLength = 100)
    private String title;
    
    // 数值范围
    @Schema(minimum = "0", maximum = "100")
    private Integer score;
    
    // 格式校验
    @Schema(format = "email")
    private String email;
    
    @Schema(format = "uri")
    private String website;
    
    // 枚举值
    @Schema(allowableValues = {"draft", "published", "archived"})
    private String status;
    
    // 正则表达式
    @Schema(pattern = "^[a-z0-9-]+$")
    private String slug;
    
    // 描述和示例
    @Schema(description = "文章摘要", example = "这是一篇关于...")
    private String excerpt;
}
```

### 常用 @Schema 属性

| 属性 | 说明 |
|-----|------|
| `description` | 字段描述 |
| `example` | 示例值 |
| `requiredMode` | 是否必填 |
| `minLength` / `maxLength` | 字符串长度限制 |
| `minimum` / `maximum` | 数值范围 |
| `format` | 格式（email、uri、date-time 等） |
| `pattern` | 正则表达式 |
| `allowableValues` | 允许的值列表 |

## 索引定义

为提高查询效率，可以为字段创建索引。

### 基础索引（2.22.0+ 推荐方式）

Halo 2.22.0 引入了新的类型安全索引 API，使用 `IndexSpecs.single()` 和 `IndexSpecs.multi()` 静态方法。

```java
import run.halo.app.extension.index.IndexSpecs;

@Override
public void start() {
    schemeManager.register(Person.class, indexSpecs -> {
        // 单值索引 - 使用 IndexSpecs.single()
        indexSpecs.add(IndexSpecs.<Person, String>single("spec.name", String.class)
            .indexFunc(person -> Optional.ofNullable(person.getSpec())
                .map(Person.Spec::getName)
                .orElse(null)));
        
        // 唯一索引
        indexSpecs.add(IndexSpecs.<Person, String>single("spec.slug", String.class)
            .unique(true)
            .indexFunc(person -> Optional.ofNullable(person.getSpec())
                .map(Person.Spec::getSlug)
                .orElse(null)));
        
        // 多值索引（如标签）- 使用 IndexSpecs.multi()
        indexSpecs.add(IndexSpecs.<Person, String>multi("spec.tags", String.class)
            .indexFunc(person -> Optional.ofNullable(person.getSpec())
                .map(Person.Spec::getTags)
                .map(Set::copyOf)
                .orElse(Set.of())));
    });
}
```

### IndexSpecs API 详解

#### 单值索引 `IndexSpecs.single()`

用于一对一映射的字段：

```java
IndexSpecs.<E, K>single(String name, Class<K> keyType)
    .indexFunc(Function<E, K> func)  // 必须：提取索引值的函数
    .unique(boolean)                  // 可选：是否唯一索引，默认 false
    .nullable(boolean)                // 可选：是否允许 null，默认 true
    .build()                          // 可选：显式构建，add() 会自动调用
```

#### 多值索引 `IndexSpecs.multi()`

用于一对多映射的字段（如集合、列表）：

```java
IndexSpecs.<E, K>multi(String name, Class<K> keyType)
    .indexFunc(Function<E, Set<K>> func)  // 必须：返回 Set 的函数
    .unique(boolean)                       // 可选：是否唯一索引
    .nullable(boolean)                     // 可选：是否允许 null
    .build()
```

### 完整示例（插件入口类）

```java
@Slf4j
@Component
public class MyPlugin extends BasePlugin {

    private final SchemeManager schemeManager;

    public MyPlugin(PluginContext pluginContext, SchemeManager schemeManager) {
        super(pluginContext);
        this.schemeManager = schemeManager;
    }

    @Override
    public void start() {
        // 注册模型并定义索引
        schemeManager.register(ChatRecord.class, indexSpecs -> {
            // 字符串字段索引
            indexSpecs.add(IndexSpecs.<ChatRecord, String>single("spec.senderOwner", String.class)
                .indexFunc(record -> Optional.ofNullable(record.getSpec())
                    .map(ChatRecord.Spec::getSenderOwner)
                    .orElse(null)));
            
            indexSpecs.add(IndexSpecs.<ChatRecord, String>single("spec.receiverOwner", String.class)
                .indexFunc(record -> Optional.ofNullable(record.getSpec())
                    .map(ChatRecord.Spec::getReceiverOwner)
                    .orElse(null)));
            
            indexSpecs.add(IndexSpecs.<ChatRecord, String>single("spec.sessionId", String.class)
                .indexFunc(record -> Optional.ofNullable(record.getSpec())
                    .map(ChatRecord.Spec::getSessionId)
                    .orElse(null)));
            
            // 枚举字段索引 - 转换为字符串
            indexSpecs.add(IndexSpecs.<ChatRecord, String>single("spec.messageType", String.class)
                .indexFunc(record -> Optional.ofNullable(record.getSpec())
                    .map(spec -> spec.getMessageType() != null ? spec.getMessageType().name() : null)
                    .orElse(null)));
            
            // 时间字段索引 - 转换为字符串
            indexSpecs.add(IndexSpecs.<ChatRecord, String>single("spec.sendTime", String.class)
                .indexFunc(record -> Optional.ofNullable(record.getSpec())
                    .map(spec -> spec.getSendTime() != null ? spec.getSendTime().toString() : null)
                    .orElse(null)));
            
            // 多值索引 - 集合字段
            indexSpecs.add(IndexSpecs.<ChatRecord, String>multi("spec.invisibleUsers", String.class)
                .indexFunc(record -> Optional.ofNullable(record.getSpec())
                    .map(ChatRecord.Spec::getInvisibleUsers)
                    .map(Set::copyOf)
                    .orElse(Set.of())));
            
            indexSpecs.add(IndexSpecs.<ChatRecord, String>multi("spec.readByUsers", String.class)
                .indexFunc(record -> Optional.ofNullable(record.getSpec())
                    .map(ChatRecord.Spec::getReadByUsers)
                    .map(Set::copyOf)
                    .orElse(Set.of())));
            
            // Status 字段索引
            indexSpecs.add(IndexSpecs.<ChatRecord, String>single("status.phase", String.class)
                .indexFunc(record -> Optional.ofNullable(record.getStatus())
                    .map(status -> status.getPhase() != null ? status.getPhase().name() : null)
                    .orElse(null)));
        });

        // 注册其他模型
        schemeManager.register(GroupChat.class, indexSpecs -> {
            indexSpecs.add(IndexSpecs.<GroupChat, String>single("spec.displayName", String.class)
                .indexFunc(group -> Optional.ofNullable(group.getSpec())
                    .map(GroupChat.Spec::getDisplayName)
                    .orElse(null)));
            
            indexSpecs.add(IndexSpecs.<GroupChat, String>single("spec.creator", String.class)
                .indexFunc(group -> Optional.ofNullable(group.getSpec())
                    .map(GroupChat.Spec::getCreator)
                    .orElse(null)));
            
            // 布尔字段索引 - 转换为字符串
            indexSpecs.add(IndexSpecs.<GroupChat, String>single("spec.isPublic", String.class)
                .indexFunc(group -> Optional.ofNullable(group.getSpec())
                    .map(spec -> String.valueOf(spec.isPublic()))
                    .orElse(null)));
        });
    }

    @Override
    public void stop() {
        // 注销模型
        schemeManager.unregister(Scheme.buildFromType(ChatRecord.class));
        schemeManager.unregister(Scheme.buildFromType(GroupChat.class));
    }
}
```

### 索引字段类型转换

由于索引值需要是 `Comparable` 类型，不同字段类型需要适当转换：

| 原始类型 | 索引类型 | 转换方式 |
|---------|---------|---------|
| String | String | 直接使用 |
| Integer/Long | String | `String.valueOf(value)` |
| Boolean | String | `String.valueOf(value)` |
| Enum | String | `enumValue.name()` |
| Instant | String | `instant.toString()` |
| List/Set | String (multi) | `Set.copyOf(collection)` |

### 旧版索引方式（已废弃）

```java
import static run.halo.app.extension.index.IndexAttributeFactory.simpleAttribute;
import static run.halo.app.extension.index.IndexAttributeFactory.multiValueAttribute;

// 已废弃，建议使用 IndexSpecs.single() 和 IndexSpecs.multi()
indexSpecs.add(new IndexSpec()
    .setName("spec.name")
    .setIndexFunc(simpleAttribute(Person.class, 
        person -> person.getSpec().getName())));
```

### 默认索引

Halo 自动为以下字段创建索引，无需手动定义：

- `metadata.name` - 唯一索引
- `metadata.labels`
- `metadata.creationTimestamp`
- `metadata.deletionTimestamp`

### 索引查询

创建索引后，可使用 `fieldSelector` 参数查询：

```
GET /apis/my-plugin.halo.run/v1alpha1/persons?fieldSelector=spec.name=张三
```

## 声明初始化数据

在 `src/main/resources/extensions/` 目录下创建 YAML 文件声明初始数据：

```yaml
# src/main/resources/extensions/persons.yaml
apiVersion: my-plugin.halo.run/v1alpha1
kind: Person
metadata:
  name: default-person
spec:
  name: 默认用户
  age: 18
  gender: MALE
---
apiVersion: my-plugin.halo.run/v1alpha1
kind: Person
metadata:
  name: admin-person
spec:
  name: 管理员
  age: 30
  gender: MALE
```

**注意：** 
- 插件每次启动都会创建/更新这些资源
- 不要将用户可修改的配置放在这里，会被覆盖

## Labels 与 Annotations

### Labels（标签）

用于分类和查询，会自动创建索引：

```java
var person = new Person();
person.setMetadata(new Metadata());
person.getMetadata().setName("my-person");
person.getMetadata().setLabels(Map.of(
    "my-plugin.halo.run/category", "vip",
    "my-plugin.halo.run/region", "cn"
));
```

**命名规范：**
- 格式：`<prefix>/<name>`
- 前缀：反向域名格式，最多 253 字符
- 名称：最多 63 字符，以字母数字开头和结尾

### Annotations（注解）

用于存储扩展信息，不创建索引：

```java
person.getMetadata().setAnnotations(Map.of(
    "my-plugin.halo.run/extra-data", "{\"key\": \"value\"}",
    "my-plugin.halo.run/description", "这是一段描述"
));
```

## 自动生成的 CRUD API

注册自定义模型后，Halo 自动生成以下 API：

| 方法 | 路径 | 说明 |
|-----|------|------|
| GET | `/apis/{group}/{version}/{plural}` | 列表查询 |
| GET | `/apis/{group}/{version}/{plural}/{name}` | 获取单个 |
| POST | `/apis/{group}/{version}/{plural}` | 创建 |
| PUT | `/apis/{group}/{version}/{plural}/{name}` | 更新 |
| DELETE | `/apis/{group}/{version}/{plural}/{name}` | 删除 |

### 列表查询参数

| 参数 | 说明 | 示例 |
|-----|------|------|
| `page` | 页码，从 1 开始 | `page=1` |
| `size` | 每页数量 | `size=10` |
| `sort` | 排序字段 | `sort=metadata.creationTimestamp,desc` |
| `labelSelector` | 标签选择器 | `labelSelector=category=vip` |
| `fieldSelector` | 字段选择器 | `fieldSelector=spec.name=张三` |

### 选择器语法

**labelSelector：**
- `key=value` - 等于
- `key!=value` - 不等于
- `key` - 存在
- `!key` - 不存在

**fieldSelector：**
- `field=value` - 等于
- `field!=value` - 不等于
- `field=(value1,value2)` - 在集合中

## 完整示例

```java
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@GVK(group = "book-plugin.halo.run",
     version = "v1alpha1",
     kind = "Book",
     plural = "books",
     singular = "book")
public class Book extends AbstractExtension {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private BookSpec spec;

    private BookStatus status;

    @Data
    @Schema(name = "BookSpec")
    public static class BookSpec {
        
        @Schema(description = "书名", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 200)
        private String title;
        
        @Schema(description = "作者")
        private String author;
        
        @Schema(description = "ISBN", pattern = "^[0-9-]+$")
        private String isbn;
        
        @Schema(description = "价格", minimum = "0")
        private BigDecimal price;
        
        @Schema(description = "分类标签")
        private Set<String> categories;
        
        @Schema(description = "是否上架")
        private Boolean published;
        
        @Schema(description = "封面图片")
        private String cover;
    }

    @Data
    @Schema(name = "BookStatus")
    public static class BookStatus {
        private String phase;
        private Integer viewCount;
        private Instant lastViewTime;
    }
}
```

注册并添加索引：

```java
@Override
public void start() {
    schemeManager.register(Book.class, indexSpecs -> {
        // 书名索引
        indexSpecs.add(IndexSpecs.<Book, String>single("spec.title", String.class)
            .indexFunc(book -> Optional.ofNullable(book.getSpec())
                .map(Book.BookSpec::getTitle)
                .orElse(null)));
        
        // ISBN 唯一索引
        indexSpecs.add(IndexSpecs.<Book, String>single("spec.isbn", String.class)
            .unique(true)
            .indexFunc(book -> Optional.ofNullable(book.getSpec())
                .map(Book.BookSpec::getIsbn)
                .orElse(null)));
        
        // 作者索引
        indexSpecs.add(IndexSpecs.<Book, String>single("spec.author", String.class)
            .indexFunc(book -> Optional.ofNullable(book.getSpec())
                .map(Book.BookSpec::getAuthor)
                .orElse(null)));
        
        // 分类多值索引
        indexSpecs.add(IndexSpecs.<Book, String>multi("spec.categories", String.class)
            .indexFunc(book -> Optional.ofNullable(book.getSpec())
                .map(Book.BookSpec::getCategories)
                .map(Set::copyOf)
                .orElse(Set.of())));
        
        // 上架状态索引
        indexSpecs.add(IndexSpecs.<Book, String>single("spec.published", String.class)
            .indexFunc(book -> Optional.ofNullable(book.getSpec())
                .map(spec -> String.valueOf(spec.getPublished()))
                .orElse(null)));
        
        // 价格索引
        indexSpecs.add(IndexSpecs.<Book, String>single("spec.price", String.class)
            .indexFunc(book -> Optional.ofNullable(book.getSpec())
                .map(spec -> spec.getPrice() != null ? spec.getPrice().toString() : null)
                .orElse(null)));
    });
}

@Override
public void stop() {
    schemeManager.unregister(Scheme.buildFromType(Book.class));
}
```

## 最佳实践

1. **命名规范**：使用 `{plugin-name}.halo.run` 作为 group
2. **版本管理**：从 `v1alpha1` 开始，稳定后升级
3. **字段校验**：使用 `@Schema` 注解定义校验规则
4. **索引优化**：只为需要查询的字段创建索引
5. **Labels vs Annotations**：需要查询的用 labels，仅存储的用 annotations
6. **Spec vs Status**：用户输入放 spec，系统状态放 status
