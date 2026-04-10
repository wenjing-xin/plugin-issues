# Halo 插件模板开发指南

本文档介绍如何在插件中为主题提供模板，以及模板开发的最佳实践。

## 概述

插件可以为主题端提供模板，有两种模式：

1. **插件规定模板名称，主题选择性适配** - 插件提供路由和默认模板，主题可以覆盖
2. **插件提供默认模板，主题优先** - 主题有模板时使用主题的，否则使用插件默认模板

## 创建模板

### 目录结构

```
my-plugin/
├── src/main/resources/
│   ├── templates/
│   │   ├── moment.html              # 主模板
│   │   ├── moment.properties        # 默认国际化（中文）
│   │   ├── moment_en.properties     # 英文国际化
│   │   ├── moment_zh_TW.properties  # 繁体中文国际化
│   │   └── fragments/               # 模板片段
│   │       ├── layout.html
│   │       └── components.html
```

### 基础模板示例

```html
<!-- templates/moment.html -->
<!doctype html>
<html xmlns:th="https://www.thymeleaf.org"
      th:replace="~{plugin:my-plugin:fragments/layout :: layout(
          title = #{title},
          head = null,
          body = ~{::body}
      )}">
    <th:block th:fragment="body">
        <div class="moment-container">
            <h1 th:text="#{page.title}">瞬间</h1>
            
            <div class="moment-list">
                <div th:each="moment : ${moments}" class="moment-item">
                    <div class="moment-content" th:text="${moment.spec.content}"></div>
                    <div class="moment-time" th:text="${#temporals.format(moment.spec.releaseTime, 'yyyy-MM-dd HH:mm')}"></div>
                </div>
            </div>
            
            <!-- 分页 -->
            <div th:if="${page.hasNext() or page.hasPrevious()}" class="pagination">
                <a th:if="${page.hasPrevious()}" th:href="@{/moments(page=${page.number})}">上一页</a>
                <a th:if="${page.hasNext()}" th:href="@{/moments(page=${page.number + 2})}">下一页</a>
            </div>
        </div>
    </th:block>
</html>
```

## 路由配置

### 使用 TemplateNameResolver

`TemplateNameResolver` 用于解析模板名称，优先使用主题模板，否则使用插件默认模板：

```java
import run.halo.app.theme.TemplateNameResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.RequestPredicates.GET;

@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
public class MomentRouter {

    private final TemplateNameResolver templateNameResolver;
    private final MomentFinder momentFinder;

    @Bean
    RouterFunction<ServerResponse> momentRouterFunction() {
        return route(GET("/moments"), this::renderMomentPage)
            .andRoute(GET("/moments/{name}"), this::renderMomentDetail);
    }

    Mono<ServerResponse> renderMomentPage(ServerRequest request) {
        int page = parseInt(request.queryParam("page").orElse("1"));
        int size = parseInt(request.queryParam("size").orElse("10"));
        
        return momentFinder.list(page, size)
            .flatMap(moments -> {
                var model = new HashMap<String, Object>();
                model.put("moments", moments.getItems());
                model.put("page", moments);
                
                return templateNameResolver
                    .resolveTemplateNameOrDefault(request.exchange(), "moment")
                    .flatMap(templateName -> ServerResponse.ok().render(templateName, model));
            });
    }

    Mono<ServerResponse> renderMomentDetail(ServerRequest request) {
        String name = request.pathVariable("name");
        
        return momentFinder.getByName(name)
            .flatMap(moment -> {
                var model = new HashMap<String, Object>();
                model.put("moment", moment);
                
                return templateNameResolver
                    .resolveTemplateNameOrDefault(request.exchange(), "moment_detail")
                    .flatMap(templateName -> ServerResponse.ok().render(templateName, model));
            })
            .switchIfEmpty(ServerResponse.notFound().build());
    }
}
```

### TemplateNameResolver 方法

| 方法 | 说明 |
|-----|------|
| `resolveTemplateNameOrDefault(exchange, name)` | 主题有模板用主题的，否则用插件默认模板 |
| `resolveTemplateNameOrDefault(exchange, name, defaultName)` | 主题有模板用主题的，否则用指定的默认模板 |
| `isTemplateAvailableInTheme(exchange, name)` | 检查主题是否有指定模板 |

## 模板片段

### 定义布局片段

```html
<!-- templates/fragments/layout.html -->
<!DOCTYPE html>
<html th:fragment="layout(title, head, body)" 
      th:lang="${#locale.toLanguageTag}"
      xmlns:th="https://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title th:text="${title}">页面标题</title>
    
    <!-- 基础样式 -->
    <link rel="stylesheet" th:href="@{/plugins/my-plugin/assets/css/style.css}">
    
    <!-- 自定义 head 内容 -->
    <th:block th:if="${head != null}" th:replace="${head}"></th:block>
</head>
<body>
    <header class="site-header">
        <nav>
            <a th:href="@{/}">首页</a>
            <a th:href="@{/moments}">瞬间</a>
        </nav>
    </header>
    
    <main class="site-main">
        <th:block th:replace="${body}"></th:block>
    </main>
    
    <footer class="site-footer">
        <p>Powered by Halo</p>
    </footer>
    
    <!-- 基础脚本 -->
    <script th:src="@{/plugins/my-plugin/assets/js/main.js}"></script>
</body>
</html>
```

### 定义组件片段

```html
<!-- templates/fragments/components.html -->

<!-- 瞬间卡片组件 -->
<div th:fragment="momentCard(moment)" class="moment-card">
    <div class="moment-content" th:utext="${moment.spec.content}"></div>
    <div class="moment-meta">
        <span class="moment-time" 
              th:text="${#temporals.format(moment.spec.releaseTime, 'yyyy-MM-dd HH:mm')}"></span>
        <span th:if="${moment.spec.location}" class="moment-location">
            📍 <span th:text="${moment.spec.location}"></span>
        </span>
    </div>
    <div th:if="${not #lists.isEmpty(moment.spec.tags)}" class="moment-tags">
        <span th:each="tag : ${moment.spec.tags}" class="tag" th:text="${tag}"></span>
    </div>
</div>

<!-- 分页组件 -->
<nav th:fragment="pagination(page, baseUrl)" class="pagination">
    <a th:if="${page.hasPrevious()}" 
       th:href="${baseUrl + '?page=' + page.number}"
       class="prev">上一页</a>
    <span class="page-info">
        第 <span th:text="${page.number + 1}"></span> 页，
        共 <span th:text="${page.totalPages}"></span> 页
    </span>
    <a th:if="${page.hasNext()}" 
       th:href="${baseUrl + '?page=' + (page.number + 2)}"
       class="next">下一页</a>
</nav>

<!-- 空状态组件 -->
<div th:fragment="emptyState(message)" class="empty-state">
    <svg viewBox="0 0 24 24" width="48" height="48">
        <path fill="currentColor" d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"/>
    </svg>
    <p th:text="${message}">暂无内容</p>
</div>
```

### 使用片段

在插件模板中使用片段时，必须使用 `plugin:<plugin-name>:` 前缀：

```html
<!-- 使用布局 -->
<html th:replace="~{plugin:my-plugin:fragments/layout :: layout(
    title = #{title},
    head = ~{::customHead},
    body = ~{::body}
)}">
    <th:block th:fragment="customHead">
        <link rel="stylesheet" href="/plugins/my-plugin/assets/css/moment.css">
    </th:block>
    
    <th:block th:fragment="body">
        <!-- 页面内容 -->
    </th:block>
</html>

<!-- 使用组件 -->
<div th:each="moment : ${moments}">
    <div th:replace="~{plugin:my-plugin:fragments/components :: momentCard(moment=${moment})}"></div>
</div>

<!-- 使用分页 -->
<div th:replace="~{plugin:my-plugin:fragments/components :: pagination(page=${page}, baseUrl='/moments')}"></div>
```

## 国际化

### 属性文件命名

```
templates/
├── moment.properties           # 默认语言（中文）
├── moment_en.properties        # 英文
├── moment_es.properties        # 西班牙文
├── moment_zh_TW.properties     # 繁体中文
└── fragments/
    ├── layout.properties
    ├── layout_en.properties
    └── layout_zh_TW.properties
```

### 属性文件内容

```properties
# moment.properties (默认中文)
title=瞬间
page.title=我的瞬间
page.description=记录生活中的点滴
empty.message=暂无瞬间
pagination.prev=上一页
pagination.next=下一页
```

```properties
# moment_en.properties
title=Moments
page.title=My Moments
page.description=Record the moments of life
empty.message=No moments yet
pagination.prev=Previous
pagination.next=Next
```

### 在模板中使用

```html
<!-- 使用 #{key} 语法 -->
<h1 th:text="#{page.title}">页面标题</h1>

<!-- 带参数的消息 -->
<p th:text="#{welcome.message(${user.displayName})}">欢迎</p>

<!-- 使用 #messages 工具 -->
<span th:text="${#messages.msgOrNull('custom.key') ?: '默认值'}"></span>
```

## 暴露模板给主题

### 文档说明

为主题开发者提供清晰的文档，说明：

1. **模板名称** - 主题需要创建的模板文件名
2. **可用变量** - 模板中可用的数据变量
3. **路由规则** - URL 路径和参数

```markdown
## 瞬间插件主题适配指南

### 模板文件

| 模板名称 | 路由 | 说明 |
|---------|------|------|
| `moment.html` | `/moments` | 瞬间列表页 |
| `moment_detail.html` | `/moments/{name}` | 瞬间详情页 |

### 可用变量

#### moment.html

| 变量 | 类型 | 说明 |
|-----|------|------|
| `moments` | `List<MomentVo>` | 瞬间列表 |
| `page` | `ListResult` | 分页信息 |

#### moment_detail.html

| 变量 | 类型 | 说明 |
|-----|------|------|
| `moment` | `MomentVo` | 瞬间详情 |

### MomentVo 结构

```java
class MomentVo {
    MetadataOperator metadata;
    MomentSpec spec;
    MomentStatus status;
}

class MomentSpec {
    String content;      // 内容（HTML）
    String rawContent;   // 原始内容
    Instant releaseTime; // 发布时间
    String location;     // 位置
    Set<String> tags;    // 标签
}
```
```

### 提供示例模板

在插件文档或仓库中提供主题适配的示例模板：

```html
<!-- 主题适配示例：moment.html -->
<!DOCTYPE html>
<html lang="zh" xmlns:th="https://www.thymeleaf.org">
<head>
    <title th:text="|瞬间 - ${site.title}|">瞬间</title>
</head>
<body>
    <!-- 使用主题的布局 -->
    <th:block th:replace="~{modules/header :: header}"></th:block>
    
    <main class="moment-page">
        <h1>瞬间</h1>
        
        <div th:if="${#lists.isEmpty(moments)}" class="empty">
            暂无瞬间
        </div>
        
        <div th:unless="${#lists.isEmpty(moments)}" class="moment-list">
            <article th:each="moment : ${moments}" class="moment-item">
                <div class="moment-content" th:utext="${moment.spec.content}"></div>
                <time th:text="${#temporals.format(moment.spec.releaseTime, 'yyyy-MM-dd HH:mm')}"></time>
            </article>
        </div>
        
        <!-- 分页 -->
        <nav th:if="${page.totalPages > 1}" class="pagination">
            <a th:if="${page.hasPrevious()}" th:href="@{/moments(page=${page.number})}">上一页</a>
            <a th:if="${page.hasNext()}" th:href="@{/moments(page=${page.number + 2})}">下一页</a>
        </nav>
    </main>
    
    <th:block th:replace="~{modules/footer :: footer}"></th:block>
</body>
</html>
```

## 样式与脚本

### 静态资源目录

```
my-plugin/
├── src/main/resources/
│   └── static/
│       ├── css/
│       │   └── moment.css
│       └── js/
│           └── moment.js
```

### 在模板中引用

```html
<!-- 插件静态资源 -->
<link rel="stylesheet" th:href="@{/plugins/my-plugin/assets/css/moment.css}">
<script th:src="@{/plugins/my-plugin/assets/js/moment.js}"></script>

<!-- 使用版本号防止缓存 -->
<link rel="stylesheet" th:href="|/plugins/my-plugin/assets/css/moment.css?v=${pluginVersion}|">
```

### CSS 变量适配主题

```css
/* moment.css */
.moment-card {
    /* 使用 CSS 变量，便于主题覆盖 */
    --moment-bg: var(--color-bg, #ffffff);
    --moment-text: var(--color-text, #333333);
    --moment-border: var(--color-border, #e5e5e5);
    
    background: var(--moment-bg);
    color: var(--moment-text);
    border: 1px solid var(--moment-border);
    border-radius: 8px;
    padding: 1rem;
    margin-bottom: 1rem;
}

/* 暗色模式适配 */
@media (prefers-color-scheme: dark) {
    .moment-card {
        --moment-bg: #1a1a1a;
        --moment-text: #e5e5e5;
        --moment-border: #333333;
    }
}
```

## 完整示例

### 路由配置

```java
@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
public class MomentRouter {

    private final TemplateNameResolver templateNameResolver;
    private final ReactiveExtensionClient client;

    @Bean
    RouterFunction<ServerResponse> momentRoutes() {
        return route()
            .GET("/moments", this::listMoments)
            .GET("/moments/{name}", this::getMoment)
            .build();
    }

    private Mono<ServerResponse> listMoments(ServerRequest request) {
        int page = parseIntOrDefault(request.queryParam("page"), 1);
        int size = parseIntOrDefault(request.queryParam("size"), 10);
        
        var pageRequest = PageRequestImpl.of(page, size,
            Sort.by(Sort.Order.desc("spec.releaseTime")));
        
        return client.listBy(Moment.class, new ListOptions(), pageRequest)
            .flatMap(result -> {
                var model = Map.of(
                    "moments", result.getItems().stream()
                        .map(MomentVo::from)
                        .toList(),
                    "page", result
                );
                return templateNameResolver
                    .resolveTemplateNameOrDefault(request.exchange(), "moment")
                    .flatMap(name -> ServerResponse.ok().render(name, model));
            });
    }

    private Mono<ServerResponse> getMoment(ServerRequest request) {
        String name = request.pathVariable("name");
        
        return client.fetch(Moment.class, name)
            .map(MomentVo::from)
            .flatMap(moment -> {
                var model = Map.of("moment", moment);
                return templateNameResolver
                    .resolveTemplateNameOrDefault(request.exchange(), "moment_detail")
                    .flatMap(templateName -> ServerResponse.ok().render(templateName, model));
            })
            .switchIfEmpty(ServerResponse.notFound().build());
    }

    private int parseIntOrDefault(Optional<String> value, int defaultValue) {
        return value.map(Integer::parseInt).orElse(defaultValue);
    }
}
```

## 最佳实践

1. **使用 TemplateNameResolver** - 让主题可以覆盖插件模板
2. **提供完整国际化** - 至少支持中文和英文
3. **使用模板片段** - 抽取公共部分，便于复用和维护
4. **CSS 变量** - 使用 CSS 变量便于主题适配
5. **文档完善** - 为主题开发者提供清晰的适配文档
6. **示例模板** - 提供主题适配的示例代码
7. **语义化 HTML** - 使用语义化标签，便于主题样式覆盖
8. **响应式设计** - 确保模板在移动端正常显示
