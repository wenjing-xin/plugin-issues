# Halo 内容处理器与 Thymeleaf 扩展开发指南

## 概述

Halo 提供了多种扩展点用于处理主题端的内容显示和模板渲染：

- **ReactivePostContentHandler** - 文章内容处理器
- **ReactiveSinglePageContentHandler** - 页面内容处理器
- **TemplateHeadProcessor** - HTML head 标签注入
- **TemplateFooterProcessor** - HTML footer 标签注入

## 文章内容处理器

### ReactivePostContentHandler

用于在主题端显示文章内容前对内容进行处理，如添加脚本、修改 HTML、注入样式等。

```java
@Component
@RequiredArgsConstructor
public class MyPostContentHandler implements ReactivePostContentHandler {

    @Override
    public Mono<PostContentContext> handle(@NonNull PostContentContext context) {
        Post post = context.getPost();
        String content = context.getContent();
        String raw = context.getRaw();
        String rawType = context.getRawType();
        
        // 示例：在内容前添加自定义脚本
        String script = """
            <script src="/plugins/my-plugin/assets/js/highlight.js"></script>
            <link rel="stylesheet" href="/plugins/my-plugin/assets/css/highlight.css">
            """;
        
        context.setContent(script + content);
        
        return Mono.just(context);
    }
}
```

### PostContentContext 结构

```java
@Data
@Builder
class PostContentContext {
    private Post post;       // 文章对象
    private String content;  // HTML 内容
    private String raw;      // 原始内容
    private String rawType;  // 原始内容类型（如 markdown）
}
```

### 声明扩展

`resources/extensions/post-content-handler.yaml`:

```yaml
apiVersion: plugin.halo.run/v1alpha1
kind: ExtensionDefinition
metadata:
  name: my-post-content-handler
spec:
  className: com.example.plugin.MyPostContentHandler
  extensionPointName: reactive-post-content-handler
  displayName: "我的文章内容处理器"
  description: "自定义文章内容处理"
```

### 实际应用示例

#### 代码高亮处理器

```java
@Component
public class CodeHighlightHandler implements ReactivePostContentHandler {

    @Override
    public Mono<PostContentContext> handle(@NonNull PostContentContext context) {
        String content = context.getContent();
        
        // 检查是否包含代码块
        if (content.contains("<pre><code")) {
            // 注入高亮脚本
            String injection = """
                <link rel="stylesheet" href="/plugins/code-highlight/assets/prism.css">
                <script src="/plugins/code-highlight/assets/prism.js"></script>
                """;
            context.setContent(injection + content);
        }
        
        return Mono.just(context);
    }
}
```

#### 图片懒加载处理器

```java
@Component
public class LazyLoadHandler implements ReactivePostContentHandler {

    @Override
    public Mono<PostContentContext> handle(@NonNull PostContentContext context) {
        String content = context.getContent();
        
        // 为图片添加懒加载属性
        content = content.replaceAll(
            "<img([^>]*)src=\"([^\"]+)\"([^>]*)>",
            "<img$1src=\"data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7\" data-src=\"$2\" loading=\"lazy\"$3>"
        );
        
        // 注入懒加载脚本
        String script = """
            <script>
            document.addEventListener('DOMContentLoaded', function() {
                const images = document.querySelectorAll('img[data-src]');
                const observer = new IntersectionObserver((entries) => {
                    entries.forEach(entry => {
                        if (entry.isIntersecting) {
                            entry.target.src = entry.target.dataset.src;
                            observer.unobserve(entry.target);
                        }
                    });
                });
                images.forEach(img => observer.observe(img));
            });
            </script>
            """;
        
        context.setContent(content + script);
        return Mono.just(context);
    }
}
```

## 页面内容处理器

### ReactiveSinglePageContentHandler

与文章内容处理器类似，用于处理自定义页面的内容。

```java
@Component
public class MyPageContentHandler implements ReactiveSinglePageContentHandler {

    @Override
    public Mono<SinglePageContentContext> handle(@NonNull SinglePageContentContext context) {
        SinglePage page = context.getSinglePage();
        String content = context.getContent();
        
        // 处理页面内容
        // ...
        
        return Mono.just(context);
    }
}
```

### SinglePageContentContext 结构

```java
@Data
@Builder
class SinglePageContentContext {
    private SinglePage singlePage;  // 页面对象
    private String content;         // HTML 内容
    private String raw;             // 原始内容
    private String rawType;         // 原始内容类型
}
```

### 声明扩展

```yaml
apiVersion: plugin.halo.run/v1alpha1
kind: ExtensionDefinition
metadata:
  name: my-page-content-handler
spec:
  className: com.example.plugin.MyPageContentHandler
  extensionPointName: reactive-single-page-content-handler
  displayName: "我的页面内容处理器"
```

## Thymeleaf Head 标签处理器

### TemplateHeadProcessor

用于向主题模板的 `<head>` 标签中注入内容，如 meta 标签、CSS、JS 等。

```java
@Component
@Order(100)  // 数值越小优先级越高
public class MyHeadProcessor implements TemplateHeadProcessor {

    @Override
    public Mono<Void> process(ITemplateContext context, IModel model,
            IElementModelStructureHandler structureHandler) {
        
        // 创建要注入的内容
        IModelFactory modelFactory = context.getModelFactory();
        
        // 注入 meta 标签
        model.add(modelFactory.createText("""
            <meta name="my-plugin" content="enabled">
            <link rel="stylesheet" href="/plugins/my-plugin/assets/style.css">
            <script src="/plugins/my-plugin/assets/main.js" defer></script>
            """));
        
        return Mono.empty();
    }
}
```

### 根据页面类型注入

```java
@Component
@Order(50)
public class ConditionalHeadProcessor implements TemplateHeadProcessor {

    @Override
    public Mono<Void> process(ITemplateContext context, IModel model,
            IElementModelStructureHandler structureHandler) {
        
        IModelFactory modelFactory = context.getModelFactory();
        
        // 获取当前模板 ID
        Object templateId = context.getVariable("_templateId");
        
        if ("post".equals(templateId)) {
            // 仅在文章页注入
            model.add(modelFactory.createText("""
                <script src="/plugins/my-plugin/assets/post-enhance.js"></script>
                """));
        } else if ("index".equals(templateId)) {
            // 仅在首页注入
            model.add(modelFactory.createText("""
                <script src="/plugins/my-plugin/assets/index-enhance.js"></script>
                """));
        }
        
        return Mono.empty();
    }
}
```

### 获取模板变量

```java
@Component
public class DynamicHeadProcessor implements TemplateHeadProcessor {

    @Override
    public Mono<Void> process(ITemplateContext context, IModel model,
            IElementModelStructureHandler structureHandler) {
        
        IModelFactory modelFactory = context.getModelFactory();
        
        // 获取文章信息
        Object postObj = context.getVariable("post");
        if (postObj instanceof PostVo post) {
            String title = post.getSpec().getTitle();
            String description = post.getSpec().getExcerpt().getRaw();
            
            // 注入 SEO meta 标签
            model.add(modelFactory.createText(String.format("""
                <meta property="og:title" content="%s">
                <meta property="og:description" content="%s">
                """, escapeHtml(title), escapeHtml(description))));
        }
        
        return Mono.empty();
    }
    
    private String escapeHtml(String text) {
        return text == null ? "" : text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;");
    }
}
```

### 声明扩展

```yaml
apiVersion: plugin.halo.run/v1alpha1
kind: ExtensionDefinition
metadata:
  name: my-head-processor
spec:
  className: com.example.plugin.MyHeadProcessor
  extensionPointName: template-head-processor
  displayName: "我的 Head 处理器"
```

## Thymeleaf Footer 标签处理器

### TemplateFooterProcessor

用于向主题模板的 `<halo:footer />` 标签位置注入内容。

```java
@Component
public class MyFooterProcessor implements TemplateFooterProcessor {

    @Override
    public Mono<Void> process(ITemplateContext context, IProcessableElementTag tag,
            IElementTagStructureHandler structureHandler, IModel model) {
        
        IModelFactory modelFactory = context.getModelFactory();
        
        // 注入统计脚本
        model.add(modelFactory.createText("""
            <script>
            // 统计代码
            (function() {
                console.log('Page loaded');
            })();
            </script>
            """));
        
        return Mono.empty();
    }
}
```

### 声明扩展

```yaml
apiVersion: plugin.halo.run/v1alpha1
kind: ExtensionDefinition
metadata:
  name: my-footer-processor
spec:
  className: com.example.plugin.MyFooterProcessor
  extensionPointName: template-footer-processor
  displayName: "我的 Footer 处理器"
```

## 处理器执行顺序

使用 `@Order` 注解控制执行顺序：

```java
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)  // 最先执行
public class FirstProcessor implements TemplateHeadProcessor { }

@Component
@Order(0)  // 默认优先级
public class NormalProcessor implements TemplateHeadProcessor { }

@Component
@Order(Ordered.LOWEST_PRECEDENCE)  // 最后执行
public class LastProcessor implements TemplateHeadProcessor { }
```

数值越小，优先级越高，越先执行。

## 完整示例：评论增强插件

```java
// 文章内容处理器 - 注入评论区增强脚本
@Component
public class CommentEnhanceContentHandler implements ReactivePostContentHandler {

    @Override
    public Mono<PostContentContext> handle(@NonNull PostContentContext context) {
        // 检查文章是否允许评论
        Post post = context.getPost();
        if (Boolean.TRUE.equals(post.getSpec().getAllowComment())) {
            String script = """
                <div id="comment-enhance-container" data-post="%s"></div>
                """.formatted(post.getMetadata().getName());
            context.setContent(context.getContent() + script);
        }
        return Mono.just(context);
    }
}

// Head 处理器 - 注入样式和脚本
@Component
@Order(100)
public class CommentEnhanceHeadProcessor implements TemplateHeadProcessor {

    @Override
    public Mono<Void> process(ITemplateContext context, IModel model,
            IElementModelStructureHandler structureHandler) {
        
        Object templateId = context.getVariable("_templateId");
        if ("post".equals(templateId) || "page".equals(templateId)) {
            IModelFactory modelFactory = context.getModelFactory();
            model.add(modelFactory.createText("""
                <link rel="stylesheet" href="/plugins/comment-enhance/assets/style.css">
                <script src="/plugins/comment-enhance/assets/main.js" defer></script>
                """));
        }
        
        return Mono.empty();
    }
}
```

## 最佳实践

1. **按需注入** - 根据页面类型判断是否需要注入资源
2. **使用 defer/async** - 脚本使用 defer 或 async 避免阻塞渲染
3. **合理排序** - 使用 @Order 控制处理器执行顺序
4. **错误处理** - 处理器中的错误不应影响页面渲染
5. **缓存考虑** - 注入的资源应考虑浏览器缓存策略
6. **安全性** - 对动态内容进行 HTML 转义
