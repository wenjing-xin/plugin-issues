# Halo 自定义模型索引注册与索引查询（基于新版本源码）

> 本文档用于补充 `halo-plugin-dev` skill 在“自定义数据模型索引注册 + 索引查询”上的源码级实践。
>
> 适用场景：用户明确提到 Halo 插件、自定义模型（Extension）、字段索引、listBy/ListOptions/FieldSelector/Queries。

## 1. 核心结论（先看）

1. 索引统一由 `IndexSpecRegistry` 管理，注册入口是 `schemeManager.register(..., indexSpecs -> { ... })`。
2. 运行时默认已存在 4 个索引：
   - `metadata.name`（唯一主键）
   - `metadata.creationTimestamp`
   - `metadata.deletionTimestamp`
   - `metadata.labels`
3. 新版本推荐索引声明方式是类型安全 API：`IndexSpecs.single()` / `IndexSpecs.multi()`。
4. `IndexSpec` 仍可用，但已标记 `@Deprecated(forRemoval = true, since = "2.22.0")`，新代码应优先使用 `IndexSpecs.*`。
5. 查询优先用 `ReactiveExtensionClient`：
   - 分页取对象：`listBy(...)`
   - 仅取名称：`listAllNames(...)` / `listTopNames(...)`
   - 计数：`countBy(...)`
6. 旧 `IndexedQueryEngine` 已废弃（2.22.0 起），仅作兼容理解，不作为新实现首选。

## 2. 源码锚点（用于回答时引用）

- `api/src/main/java/run/halo/app/extension/index/IndexSpecRegistry.java`
  - `indexFor(Scheme)` / `getIndexSpecs(Scheme)` / `contains(Scheme)`
- `application/src/main/java/run/halo/app/extension/index/IndexSpecRegistryImpl.java`
  - `useDefaultIndexSpec(...)` 展示默认索引注入细节
- `api/src/main/java/run/halo/app/extension/index/IndexSpecs.java`
  - `IndexSpecs.single(...)` / `IndexSpecs.multi(...)`
- `api/src/main/java/run/halo/app/extension/index/IndexSpec.java`
  - 已废弃，说明兼容路径
- `api/src/main/java/run/halo/app/extension/ReactiveExtensionClient.java`
  - `listBy` / `listAllNames` / `listTopNames` / `countBy`
- `application/src/main/java/run/halo/app/extension/ReactiveExtensionClientImpl.java`
  - 真实调用链：`indexEngine.retrieve(...)` / `retrieveAll(...)` / `count(...)`

## 3. 索引注册模式（推荐）

### 3.1 在模型注册时声明索引

```java
@Override
public void start() {
    schemeManager.register(Person.class, indexSpecs -> {
        // 单值索引：可用于等值/范围/排序场景
        indexSpecs.add(IndexSpecs.<Person, String>single("spec.slug", String.class)
            .unique(true)
            .indexFunc(person -> Optional.ofNullable(person.getSpec())
                .map(Person.Spec::getSlug)
                .orElse(null)));

        // 多值索引：集合字段（如 tags）
        indexSpecs.add(IndexSpecs.<Person, String>multi("spec.tags", String.class)
            .indexFunc(person -> Optional.ofNullable(person.getSpec())
                .map(Person.Spec::getTags)
                .map(Set::copyOf)
                .orElse(Set.of())));
    });
}
```

### 3.2 命名和设计建议

- 索引名与字段路径保持一致（如 `spec.slug`, `spec.status`），便于 `Queries.*("spec.xxx", ...)` 对齐。
- `unique(true)` 仅用于业务上确实全局唯一的字段。
- 仅为“会查询/会排序/会筛选”的字段建索引，避免无效索引膨胀。

## 4. 查询构建模式（推荐）

### 4.1 ListOptions + Queries

```java
var options = ListOptions.builder()
    .fieldQuery(Queries.and(
        Queries.equal("spec.status", "active"),
        Queries.contains("spec.name", keyword)
    ))
    .build();

var page = PageRequestImpl.of(1, 20,
    Sort.by(Sort.Order.desc("metadata.creationTimestamp")));

return client.listBy(Person.class, options, page);
```

### 4.2 按场景选择 API

- 页面列表：`listBy(type, options, pageRequest)`
- 批处理/低内存扫描：`listAllNames(...)` 后按 name 拉取详情
- 统计：`countBy(type, options)`
- TopN：`listTopNames(type, options, sort, topN)`

## 5. 兼容与迁移要点

1. 若现有代码使用 `new IndexSpec().setName(...).setIndexFunc(...)`，可运行但建议逐步迁移到 `IndexSpecs.single/multi`。
2. 若代码依赖 `indexedQueryEngine()` 或直接 `IndexedQueryEngine`，新实现应改用 `ReactiveExtensionClient` 的 `listAllNames/countBy/listTopNames`。
3. 用户提到“FieldSelector 不生效/报错”时，先核对对应字段是否已注册索引。

## 6. 排障清单（索引相关）

1. 字段查询报错：确认查询字段与索引名完全一致（如 `spec.slug`）。
2. 查询结果异常：检查 `indexFunc` 是否可能返回空值或类型不一致。
3. 启动后查询为空：确认插件 `start()` 中已执行 `schemeManager.register(...)` 并成功注册索引。
4. 大数据量慢查询：优先使用 `listBy` 分页或 `listAllNames + 分批 fetch`，避免一次性 `listAll` 全量对象。

## 7. Skill 执行时的最小流程

1. 先判定任务是“索引注册”还是“索引查询”（或二者都要）。
2. 注册类任务：先给出 `IndexSpecs.single/multi` 方案，再补唯一性与空值策略。
3. 查询类任务：先对齐 `ListOptions + Queries`，再选 `listBy / listAllNames / countBy`。
4. 最后附验证点：字段命中、分页正确、排序正确、计数正确。
