# Halo Finder 开发指南

本文档介绍如何在插件中创建 Finder，为主题模板提供数据查询能力。

## 概述

Finder 是 Halo 提供的一种机制，允许插件向主题模板暴露数据查询方法。主题开发者可以在 Thymeleaf 模板中直接调用 Finder 方法获取数据。

## 创建 Finder

### 1. 定义接口

首先定义一个接口，声明需要提供给主题的数据查询方法：

```java
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface LinkFinder {
    
    /**
     * 根据名称获取单个链接
     */
    Mono<LinkVo> getByName(String name);
    
    /**
     * 获取所有链接
     */
    Flux<LinkVo> listAll();
    
    /**
     * 分页查询链接
     */
    Mono<ListResult<LinkVo>> list(Integer page, Integer size);
    
    /**
     * 按分组获取链接
     */
    Flux<LinkVo> listByGroup(String groupName);
}
```

**方法返回值要求：**
- 单个对象使用 `Mono<T>`
- 多个对象使用 `Flux<T>`
- 分页结果使用 `Mono<ListResult<T>>`

### 2. 实现接口并添加 @Finder 注解

```java
import run.halo.app.theme.finders.Finder;
import run.halo.app.extension.ReactiveExtensionClient;
import lombok.RequiredArgsConstructor;

@Finder("myPluginLinkFinder")
@RequiredArgsConstructor
public class LinkFinderImpl implements LinkFinder {

    private final ReactiveExtensionClient client;

    @Override
    public Mono<LinkVo> getByName(String name) {
        return client.fetch(Link.class, name)
            .map(LinkVo::from);
    }

    @Override
    public Flux<LinkVo> listAll() {
        return client.listAll(Link.class, new ListOptions(), defaultSort())
            .map(LinkVo::from);
    }

    @Override
    public Mono<ListResult<LinkVo>> list(Integer page, Integer size) {
        var pageRequest = PageRequestImpl.of(
            page == null ? 1 : page,
            size == null ? 10 : size
        );
        return client.listBy(Link.class, new ListOptions(), pageRequest)
            .map(result -> {
                var items = result.get()
                    .map(LinkVo::from)
                    .toList();
                return new ListResult<>(result.getPage(), result.getSize(), 
                    result.getTotal(), items);
            });
    }

    @Override
    public Flux<LinkVo> listByGroup(String groupName) {
        var options = ListOptions.builder()
            .andQuery(Queries.equal("spec.groupName", groupName))
            .build();
        return client.listAll(Link.class, options, defaultSort())
            .map(LinkVo::from);
    }

    private Sort defaultSort() {
        return Sort.by(Sort.Order.desc("metadata.creationTimestamp"));
    }
}
```

### 3. 定义 VO 对象

为主题提供的数据应该使用 VO（Value Object）封装，避免直接暴露 Extension 对象：

```java
import lombok.Builder;
import lombok.Value;
import run.halo.app.extension.MetadataOperator;

@Value
@Builder
public class LinkVo {

    MetadataOperator metadata;

    Link.LinkSpec spec;

    /**
     * 从 Extension 转换为 VO
     */
    public static LinkVo from(Link link) {
        return LinkVo.builder()
            .metadata(link.getMetadata())
            .spec(link.getSpec())
            .build();
    }
}
```

## @Finder 注解

```java
@Finder("myPluginLinkFinder")
```

- `value`: Finder 在主题模板中的变量名
- 注解本身包含 `@Service`，会自动注册为 Spring Bean

### 命名规范

为避免与其他插件冲突，建议使用插件名作为前缀：

```java
// 推荐格式：{pluginName}{功能}Finder
@Finder("myPluginLinkFinder")
@Finder("myPluginMomentFinder")
@Finder("myPluginBookFinder")
```

**命名规则：**
- 使用驼峰命名
- 只能包含字母、数字和下划线 `_`
- 不能包含其他特殊字符

## 在主题中使用

### 基础用法

```html
<!-- 获取单个对象 -->
<div th:with="link = ${myPluginLinkFinder.getByName('my-link')}">
    <a th:href="${link.spec.url}" th:text="${link.spec.displayName}"></a>
</div>

<!-- 遍历列表 -->
<ul th:each="link : ${myPluginLinkFinder.listAll()}">
    <li>
        <a th:href="${link.spec.url}" th:text="${link.spec.displayName}"></a>
    </li>
</ul>

<!-- 分页查询 -->
<div th:with="result = ${myPluginLinkFinder.list(1, 10)}">
    <ul th:each="link : ${result.items}">
        <li th:text="${link.spec.displayName}"></li>
    </ul>
    <span th:text="${result.total}">总数</span>
</div>

<!-- 带参数查询 -->
<ul th:each="link : ${myPluginLinkFinder.listByGroup('friends')}">
    <li th:text="${link.spec.displayName}"></li>
</ul>
```

### 处理空值

```html
<!-- 使用 th:if 判断 -->
<div th:with="link = ${myPluginLinkFinder.getByName('my-link')}"
     th:if="${link != null}">
    <a th:href="${link.spec.url}" th:text="${link.spec.displayName}"></a>
</div>

<!-- 使用 Elvis 运算符提供默认值 -->
<span th:text="${link.spec.description} ?: '暂无描述'"></span>
```

## 完整示例

### 自定义模型

```java
@Data
@EqualsAndHashCode(callSuper = true)
@GVK(group = "link-plugin.halo.run",
     version = "v1alpha1",
     kind = "Link",
     plural = "links",
     singular = "link")
public class Link extends AbstractExtension {

    private LinkSpec spec;

    @Data
    public static class LinkSpec {
        private String displayName;
        private String url;
        private String logo;
        private String description;
        private String groupName;
        private Integer priority;
    }
}
```

### Finder 接口

```java
public interface LinkFinder {
    
    Mono<LinkVo> getByName(String name);
    
    Flux<LinkVo> listAll();
    
    Mono<ListResult<LinkVo>> list(Integer page, Integer size);
    
    Flux<LinkVo> listByGroup(String groupName);
    
    Flux<LinkGroupVo> listGroups();
}
```

### Finder 实现

```java
@Finder("linkPluginFinder")
@RequiredArgsConstructor
public class LinkFinderImpl implements LinkFinder {

    private final ReactiveExtensionClient client;

    @Override
    public Mono<LinkVo> getByName(String name) {
        return client.fetch(Link.class, name)
            .map(LinkVo::from);
    }

    @Override
    public Flux<LinkVo> listAll() {
        var options = ListOptions.builder().build();
        var sort = Sort.by(
            Sort.Order.asc("spec.priority"),
            Sort.Order.desc("metadata.creationTimestamp")
        );
        return client.listAll(Link.class, options, sort)
            .map(LinkVo::from);
    }

    @Override
    public Mono<ListResult<LinkVo>> list(Integer page, Integer size) {
        var pageRequest = PageRequestImpl.of(
            page == null ? 1 : page,
            size == null ? 10 : size,
            Sort.by(Sort.Order.asc("spec.priority"))
        );
        return client.listBy(Link.class, new ListOptions(), pageRequest)
            .map(result -> {
                var items = result.get()
                    .map(LinkVo::from)
                    .toList();
                return new ListResult<>(result.getPage(), result.getSize(),
                    result.getTotal(), items);
            });
    }

    @Override
    public Flux<LinkVo> listByGroup(String groupName) {
        var options = ListOptions.builder()
            .andQuery(Queries.equal("spec.groupName", groupName))
            .build();
        var sort = Sort.by(Sort.Order.asc("spec.priority"));
        return client.listAll(Link.class, options, sort)
            .map(LinkVo::from);
    }

    @Override
    public Flux<LinkGroupVo> listGroups() {
        return client.listAll(LinkGroup.class, new ListOptions(), 
                Sort.by(Sort.Order.asc("spec.priority")))
            .flatMap(group -> {
                return listByGroup(group.getMetadata().getName())
                    .collectList()
                    .map(links -> LinkGroupVo.builder()
                        .metadata(group.getMetadata())
                        .spec(group.getSpec())
                        .links(links)
                        .build());
            });
    }
}
```

### VO 对象

```java
@Value
@Builder
public class LinkVo {
    MetadataOperator metadata;
    Link.LinkSpec spec;

    public static LinkVo from(Link link) {
        return LinkVo.builder()
            .metadata(link.getMetadata())
            .spec(link.getSpec())
            .build();
    }
}

@Value
@Builder
public class LinkGroupVo {
    MetadataOperator metadata;
    LinkGroup.LinkGroupSpec spec;
    List<LinkVo> links;
}
```

### 主题模板使用

```html
<!-- links.html -->
<div class="link-groups">
    <div th:each="group : ${linkPluginFinder.listGroups()}" class="link-group">
        <h3 th:text="${group.spec.displayName}">分组名称</h3>
        <p th:text="${group.spec.description}">分组描述</p>
        
        <div class="links">
            <div th:each="link : ${group.links}" class="link-item">
                <a th:href="${link.spec.url}" target="_blank">
                    <img th:src="${link.spec.logo}" th:alt="${link.spec.displayName}">
                    <span th:text="${link.spec.displayName}">链接名称</span>
                </a>
                <p th:text="${link.spec.description}">链接描述</p>
            </div>
        </div>
    </div>
</div>
```

## Halo 内置 Finder

Halo 提供了以下内置 Finder，插件可以注入使用：

| Finder | 变量名 | 说明 |
|--------|-------|------|
| PostFinder | `postFinder` | 文章查询 |
| SinglePageFinder | `singlePageFinder` | 自定义页面查询 |
| CategoryFinder | `categoryFinder` | 分类查询 |
| TagFinder | `tagFinder` | 标签查询 |
| CommentFinder | `commentFinder` | 评论查询 |
| MenuFinder | `menuFinder` | 菜单查询 |
| ContributorFinder | `contributorFinder` | 贡献者查询 |
| SiteStatsFinder | `siteStatsFinder` | 站点统计 |
| ThemeFinder | `themeFinder` | 主题信息 |
| PluginFinder | `pluginFinder` | 插件信息 |
| ThumbnailFinder | `thumbnail` | 缩略图 |

### 在插件中使用内置 Finder

```java
@Finder("myPluginFinder")
@RequiredArgsConstructor
public class MyFinderImpl implements MyFinder {

    private final PostFinder postFinder;
    private final CategoryFinder categoryFinder;

    public Flux<ListedPostVo> getRelatedPosts(String categoryName) {
        return categoryFinder.getByName(categoryName)
            .flatMapMany(category -> 
                postFinder.listByCategory(1, 5, categoryName)
                    .flatMapMany(result -> Flux.fromIterable(result.getItems()))
            );
    }
}
```

## 最佳实践

1. **命名规范**：使用 `{pluginName}{功能}Finder` 格式命名
2. **返回 VO**：不要直接返回 Extension 对象，使用 VO 封装
3. **空值处理**：方法应该正确处理空值情况
4. **分页支持**：列表查询应该支持分页
5. **排序支持**：提供合理的默认排序
6. **性能考虑**：避免在 Finder 中执行耗时操作
7. **文档说明**：为主题开发者提供 Finder 使用文档
