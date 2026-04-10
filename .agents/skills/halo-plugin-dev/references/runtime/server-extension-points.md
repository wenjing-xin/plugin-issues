---
inclusion: manual
---

# Halo 服务端扩展点

服务端扩展点是 Halo 提供的用于在后端添加特定功能的接口。扩展点位于服务核心功能和集成之间，是对服务的扩充但不影响核心功能。

## 使用扩展点的必要步骤

1. 实现扩展点接口，标记 `@Component` 注解
2. 声明 `ExtensionDefinition` 自定义模型对象

## Web 过滤器 (AdditionalWebFilter)

Web 过滤器扩展点允许在服务器处理请求之前或之后执行特定任务，如安全验证、超时处理等。

### 接口

```java
run.halo.app.security.AdditionalWebFilter
```

### 实现示例

```java
@Component
public class UsernamePasswordAuthenticator implements AdditionalWebFilter {
    
    final ServerWebExchangeMatcher requiresMatcher = 
        ServerWebExchangeMatchers.pathMatchers(HttpMethod.POST, "/login");

    @Override
    @NonNull
    public Mono<Void> filter(@NonNull ServerWebExchange exchange, 
                             @NonNull WebFilterChain chain) {
        return this.requiresAuthenticationMatcher.matches(exchange)
            .filter(matchResult -> matchResult.isMatch())
            .flatMap(matchResult -> this.authenticationConverter.convert(exchange))
            .switchIfEmpty(chain.filter(exchange).then(Mono.empty()))
            .flatMap(token -> this.authenticate(exchange, chain, token))
            .onErrorResume(AuthenticationException.class, ex -> 
                this.authenticationFailureHandler.onAuthenticationFailure(
                    new WebFilterExchange(exchange, chain), ex));
    }

    @Override
    public int getOrder() {
        return SecurityWebFiltersOrder.FORM_LOGIN.getOrder();
    }
}
```

### ExtensionPointDefinition

```yaml
apiVersion: plugin.halo.run/v1alpha1
kind: ExtensionPointDefinition
metadata:
  name: additional-webfilter
spec:
  className: run.halo.app.security.AdditionalWebFilter
  displayName: AdditionalWebFilter
  type: MULTI_INSTANCE
  description: "Contract for interception-style, chained processing of Web requests"
```

### 关键点

- `extensionPointName`: `additional-webfilter`
- 类型: `MULTI_INSTANCE` (多实例)
- 用途: 实现跨切面、应用无关的需求，如安全、超时等
- 通过 `getOrder()` 方法控制过滤器执行顺序


---

## 认证安全过滤器 (FormLoginSecurityWebFilter)

认证安全过滤器是专门用于扩展认证相关功能的扩展点。此前 `AdditionalWebFilter` 曾用于认证扩展，但现已明确用途分离，认证相关功能应使用此扩展点。

### 接口

```java
run.halo.app.security.FormLoginSecurityWebFilter
```

### 实现示例

```java
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import run.halo.app.security.FormLoginSecurityWebFilter;

@Component
public class MyFormLoginSecurityWebFilter implements FormLoginSecurityWebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        // 在此处实现认证逻辑
        return chain.filter(exchange);
    }
}
```

### 关键点

- 用途: 专门用于认证相关的过滤器扩展
- 与 `AdditionalWebFilter` 的区别: 
  - `AdditionalWebFilter` 用于通用的 Web 请求拦截处理
  - `FormLoginSecurityWebFilter` 专门用于认证安全相关功能
- 无需额外声明 ExtensionDefinition，直接使用 `@Component` 注解即可


---

## 附件存储 (AttachmentHandler)

附件存储策略扩展点支持扩展附件的上传和存储方式，如将附件存储到第三方云存储服务中。

### 接口

```java
run.halo.app.core.extension.attachment.endpoint.AttachmentHandler
```

### 接口定义

```java
public interface AttachmentHandler extends ExtensionPoint {

    Mono<Attachment> upload(UploadContext context);

    Mono<Attachment> delete(DeleteContext context);

    default Mono<URI> getSharedURL(Attachment attachment,
        Policy policy,
        ConfigMap configMap,
        Duration ttl) {
        return Mono.empty();
    }
    
    default Mono<URI> getPermalink(Attachment attachment,
        Policy policy,
        ConfigMap configMap) {
        return Mono.empty();
    }
}
```

### 方法说明

| 方法 | 说明 |
|------|------|
| `upload` | 上传附件，返回上传成功后的附件对象 |
| `delete` | 删除附件，返回删除后的附件对象 |
| `getSharedURL` | 获取附件的共享链接（带有效期） |
| `getPermalink` | 获取附件的永久链接 |

### ExtensionPointDefinition

```yaml
apiVersion: plugin.halo.run/v1alpha1
kind: ExtensionPointDefinition
metadata:
  name: attachment-handler
spec:
  className: run.halo.app.core.extension.attachment.endpoint.AttachmentHandler
  displayName: AttachmentHandler
  type: MULTI_INSTANCE
  description: "Provide extension points for attachment storage strategies"
```

### 关键点

- `extensionPointName`: `attachment-handler`
- 类型: `MULTI_INSTANCE` (多实例)
- 用途: 扩展附件存储策略，支持第三方云存储

### 参考项目

- [S3 对象存储协议的存储插件](https://github.com/halo-sigs/plugin-s3)
- [阿里云 OSS 的存储策略插件](https://github.com/halo-sigs/plugin-alioss)
- [又拍云 OSS 的存储策略](https://github.com/halo-sigs/plugin-uposs)


---

## 评论主体展示 (CommentSubject)

评论主体扩展点用于在管理端评论列表中展示评论的主体内容。如果你的插件使用了 Halo 的评论自定义模型，需要实现此扩展点来展示评论来源，否则评论列表中对应的评论主体会显示为"未知"。

### 接口

```java
run.halo.app.content.comment.CommentSubject
```

### 接口定义

```java
public interface CommentSubject<T extends Extension> extends ExtensionPoint {

    Mono<T> get(String name);

    default Mono<SubjectDisplay> getSubjectDisplay(String name) {
        return Mono.empty();
    }

    boolean supports(Ref ref);

    record SubjectDisplay(String title, String url, String kindName) {
    }
}
```

### 方法说明

| 方法 | 说明 |
|------|------|
| `get` | 获取评论主体对象，参数 `name` 是主体的自定义模型对象名称 |
| `getSubjectDisplay` | 获取评论主体的展示信息（标题、链接、类型名称），用于主题端展示 |
| `supports` | 判断是否支持该评论主体，返回 `true` 表示支持 |

### 实现示例（文章评论主体）

```java
public class PostCommentSubject implements CommentSubject<Post> {

    private final ReactiveExtensionClient client;
    private final ExternalLinkProcessor externalLinkProcessor;

    @Override
    public Mono<Post> get(String name) {
        return client.fetch(Post.class, name);
    }

    @Override
    public Mono<SubjectDisplay> getSubjectDisplay(String name) {
        return get(name)
            .map(post -> {
                var url = externalLinkProcessor
                    .processLink(post.getStatusOrDefault().getPermalink());
                return new SubjectDisplay(post.getSpec().getTitle(), url, "文章");
            });
    }

    @Override
    public boolean supports(Ref ref) {
        Assert.notNull(ref, "Subject ref must not be null.");
        GroupVersionKind groupVersionKind =
            new GroupVersionKind(ref.getGroup(), ref.getVersion(), ref.getKind());
        return GroupVersionKind.fromExtension(Post.class).equals(groupVersionKind);
    }
}
```

### ExtensionPointDefinition

```yaml
apiVersion: plugin.halo.run/v1alpha1
kind: ExtensionPointDefinition
metadata:
  name: comment-subject
spec:
  className: run.halo.app.content.comment.CommentSubject
  displayName: CommentSubject
  type: MULTI_INSTANCE
  description: "Provide extension points for comment subject display"
```

### 关键点

- `extensionPointName`: `comment-subject`
- 类型: `MULTI_INSTANCE` (多实例)
- 用途: 让管理端评论列表正确显示评论来源
- 配合前端扩展点 [UI 评论来源显示](https://docs.halo.run/developer-guide/plugin/extension-points/ui/comment-subject-ref-create) 使用

### 参考项目

- [Halo 自定义页面评论主体](https://github.com/halo-dev/halo)
- [瞬间的评论主体](https://github.com/halo-sigs/plugin-moments)


---

## 通知器 (ReactiveNotifier)

通知器扩展点用于为 Halo 通知系统提供更多通知方式，例如：邮件、短信、WebHook 等。

### 接口

```java
run.halo.app.notification.ReactiveNotifier
```

### 接口定义

```java
public interface ReactiveNotifier extends ExtensionPoint {

    Mono<Void> notify(NotificationContext context);
}
```

### 方法说明

| 方法 | 说明 |
|------|------|
| `notify` | 发送通知，`context` 包含通知内容、接收者、通知配置等信息 |

### 实现步骤

除了实现扩展点接口并声明 `ExtensionDefinition`，还需要声明 `NotifierDescriptor` 自定义模型对象：

```yaml
apiVersion: notification.halo.run/v1alpha1
kind: NotifierDescriptor
metadata:
  name: default-email-notifier
spec:
  displayName: '邮件通知'
  description: '通过邮件将通知发送给用户'
  notifierExtName: 'halo-email-notifier'
  senderSettingRef:
    name: 'notifier-setting-for-email'
    group: 'sender'
  # receiverSettingRef:
  #   name: ''
  #   group: ''
```

### NotifierDescriptor 配置说明

| 字段 | 说明 |
|------|------|
| `notifierExtName` | 通知器扩展的自定义模型对象名称 |
| `senderSettingRef.name` | 发送者配置的 Setting 自定义模型对象名称 |
| `senderSettingRef.group` | Setting 中 formSchema 的 group 名称 |
| `receiverSettingRef` | 接收者配置（可选），配置方式同 senderSettingRef |

当配置了 `senderSettingRef` 后，`notify` 方法的 `context` 参数中会包含 `senderConfig`（发送者配置值），`receiverConfig` 同理。

### ExtensionPointDefinition

```yaml
apiVersion: plugin.halo.run/v1alpha1
kind: ExtensionPointDefinition
metadata:
  name: reactive-notifier
spec:
  className: run.halo.app.notification.ReactiveNotifier
  displayName: Notifier
  type: MULTI_INSTANCE
  description: "Provides a way to extend the notifier to send notifications to users."
```

### 关键点

- `extensionPointName`: `reactive-notifier`
- 类型: `MULTI_INSTANCE` (多实例)
- 用途: 扩展通知发送方式（邮件、短信、WebHook 等）
- 需要额外声明 `NotifierDescriptor` 来描述通知器

### 参考项目

- [Halo 邮件通知器](https://github.com/halo-dev/halo)


---

## 主题端 HTML Head 标签处理 (TemplateHeadProcessor)

用于干预 HTML 页面中的 Head 标签内容，可以添加自定义的 CSS、JS 及 meta 标签等。

### 使用场景

- 添加自定义样式或脚本（CSS、JavaScript）
- 定制 Meta 标签（描述、作者、关键词等，提高 SEO）
- 引入第三方库（Google Fonts、Font Awesome 等）
- 定制 Open Graph 等社交媒体标签

### 接口

```java
run.halo.app.theme.dialect.TemplateHeadProcessor
```

### 接口定义

```java
@FunctionalInterface
public interface TemplateHeadProcessor extends ExtensionPoint {

    Mono<Void> process(ITemplateContext context, IModel model,
        IElementModelStructureHandler structureHandler);
}
```

### 实现示例

```java
@Component
public class CustomHeadProcessor implements TemplateHeadProcessor {

    @Override
    public Mono<Void> process(ITemplateContext context, IModel model,
        IElementModelStructureHandler structureHandler) {
        // 添加自定义 CSS 文件
        model.add(context.createStandaloneElementTag("link",
                "rel", "stylesheet",
                "href", "/custom/styles.css"));
        
        // 添加自定义 Meta 标签
        model.add(context.createStandaloneElementTag("meta",
                "name", "author",
                "content", "Your Name"));
        return Mono.empty();
    }
}
```

### ExtensionDefinition 示例

```yaml
apiVersion: plugin.halo.run/v1alpha1
kind: ExtensionDefinition
metadata:
  name: custom-head-extension
spec:
  extensionPointName: template-head-processor
  className: com.example.CustomHeadProcessor
  displayName: "Custom Head Extension"
  description: "Adds custom CSS and meta tags to the head section."
```

### 关键点

- `extensionPointName`: `template-head-processor`
- 类型: `MULTI_INSTANCE` (多实例)
- 用途: 向主题端 HTML Head 注入内容

### 参考项目

- [highlight.js 代码块高亮渲染](https://github.com/halo-sigs/plugin-highlightjs)
- [lightgallery.js 图片放大显示](https://github.com/halo-sigs/plugin-lightgallery)
- [Umami 统计集成](https://github.com/halo-sigs/plugin-umami)

---

## 主题端 Halo Footer 标签处理 (TemplateFooterProcessor)

用于扩展主题端 `<halo:footer/>` 自定义标签的内容，添加额外的页脚内容。

### 使用场景

- 添加备案号
- 添加版权信息
- 添加统计代码
- 添加自定义脚本
- 添加自定义链接

### 接口

```java
run.halo.app.theme.dialect.TemplateFooterProcessor
```

### 接口定义

```java
public interface TemplateFooterProcessor extends ExtensionPoint {

    Mono<Void> process(ITemplateContext context, IProcessableElementTag tag,
        IElementTagStructureHandler structureHandler, IModel model);
}
```

### 实现示例

```java
@Component
public class FakeFooterCodeInjection implements TemplateFooterProcessor {

    @Override
    public Mono<Void> process(ITemplateContext context, IProcessableElementTag tag,
        IElementTagStructureHandler structureHandler, IModel model) {
        var factory = context.getModelFactory();
        // 添加版权信息
        var copyRight = factory.createText("<div>© 2024 Halo</div>");
        model.add(copyRight);
        return Mono.empty();
    }
}
```

### ExtensionDefinition 示例

```yaml
apiVersion: plugin.halo.run/v1alpha1
kind: ExtensionDefinition
metadata:
  name: custom-footer-extension
spec:
  extensionPointName: template-footer-processor
  className: com.example.FakeFooterCodeInjection
  displayName: "Custom Footer Extension"
  description: "Adds custom footer content."
```

### 关键点

- `extensionPointName`: `template-footer-processor`
- 类型: `MULTI_INSTANCE` (多实例)
- 用途: 向主题端 `<halo:footer/>` 标签注入内容


---

## 用户名密码认证管理器 (UsernamePasswordAuthenticationManager)

用于替换 Halo 默认的用户名密码认证管理器实现，例如使用第三方身份验证服务（如 LDAP）。

### 接口

```java
run.halo.app.security.authentication.login.UsernamePasswordAuthenticationManager
```

### 接口定义

```java
public interface UsernamePasswordAuthenticationManager extends ExtensionPoint {
    Mono<Authentication> authenticate(Authentication authentication);
}
```

### 方法说明

| 方法 | 说明 |
|------|------|
| `authenticate` | 执行认证逻辑，接收认证信息，返回认证结果 |

### ExtensionPointDefinition

```yaml
apiVersion: plugin.halo.run/v1alpha1
kind: ExtensionPointDefinition
metadata:
  name: username-password-authentication-manager
spec:
  className: run.halo.app.security.authentication.login.UsernamePasswordAuthenticationManager
  displayName: Username password authentication manager
  type: SINGLETON
  description: "Provides a way to extend the username password authentication."
```

### 关键点

- `extensionPointName`: `username-password-authentication-manager`
- 类型: `SINGLETON` (单实例) - 只能有一个实现
- 用途: 替换默认认证逻辑，集成第三方身份验证服务

### 参考项目

- [TOTP 认证](https://github.com/halo-sigs/plugin-totp)
