# Halo 自定义 API 开发指南

本文档介绍如何在 Halo 插件中创建自定义 API 端点。

## CustomEndpoint 接口

`CustomEndpoint` 是 Halo 提供的自定义 API 接口，基于 Spring WebFlux 的函数式端点实现：

```java
public interface CustomEndpoint {
    
    // 定义路由
    RouterFunction<ServerResponse> endpoint();
    
    // API 分组版本，决定 API 路径前缀
    default GroupVersion groupVersion() {
        return GroupVersion.parseAPIVersion("api.console.halo.run/v1alpha1");
    }
}
```

## API 路径规则

根据 `groupVersion()` 返回值，API 路径会自动生成：

| GroupVersion | 生成的 API 路径 | 适用场景 |
|-------------|----------------|---------|
| `api.console.halo.run/v1alpha1` | `/apis/api.console.halo.run/v1alpha1/...` | Console 管理端 API |
| `uc.api.halo.run/v1alpha1` | `/apis/uc.api.halo.run/v1alpha1/...` | 用户中心 API |
| `api.halo.run/v1alpha1` | `/apis/api.halo.run/v1alpha1/...` | 公开 API（主题端可用） |
| `api.{plugin}.halo.run/v1alpha1` | `/apis/api.{plugin}.halo.run/v1alpha1/...` | 插件自定义 API |

### 命名规范

- **Console API**: `console.api.{domain}.halo.run/v1alpha1` - 需要管理员权限
- **UC API**: `uc.api.{domain}.halo.run/v1alpha1` - 用户中心，需要登录
- **Public API**: `api.{domain}.halo.run/v1alpha1` - 公开访问

## 基础示例

```java
@Component
@RequiredArgsConstructor
public class MyPluginEndpoint implements CustomEndpoint {

    private final ReactiveExtensionClient client;

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        final var tag = "MyPluginV1alpha1";
        return SpringdocRouteBuilder.route()
            // GET 请求
            .GET("/resources", this::listResources,
                builder -> builder.operationId("ListResources")
                    .description("列出所有资源")
                    .tag(tag)
                    .response(responseBuilder()
                        .implementation(ListResult.generateGenericClass(MyResource.class))))
            // GET 带路径参数
            .GET("/resources/{name}", this::getResource,
                builder -> builder.operationId("GetResource")
                    .description("获取单个资源")
                    .tag(tag)
                    .parameter(parameterBuilder()
                        .name("name")
                        .in(ParameterIn.PATH)
                        .required(true)
                        .implementation(String.class))
                    .response(responseBuilder()
                        .implementation(MyResource.class)))
            // POST 请求
            .POST("/resources", this::createResource,
                builder -> builder.operationId("CreateResource")
                    .description("创建资源")
                    .tag(tag)
                    .requestBody(requestBodyBuilder()
                        .required(true)
                        .content(contentBuilder()
                            .mediaType(MediaType.APPLICATION_JSON_VALUE)
                            .schema(schemaBuilder()
                                .implementation(CreateResourceRequest.class))))
                    .response(responseBuilder()
                        .implementation(MyResource.class)))
            // PUT 请求
            .PUT("/resources/{name}", this::updateResource,
                builder -> builder.operationId("UpdateResource")
                    .description("更新资源")
                    .tag(tag)
                    .parameter(parameterBuilder()
                        .name("name")
                        .in(ParameterIn.PATH)
                        .required(true))
                    .requestBody(requestBodyBuilder()
                        .required(true)
                        .content(contentBuilder()
                            .mediaType(MediaType.APPLICATION_JSON_VALUE)
                            .schema(schemaBuilder()
                                .implementation(UpdateResourceRequest.class))))
                    .response(responseBuilder()
                        .implementation(MyResource.class)))
            // DELETE 请求
            .DELETE("/resources/{name}", this::deleteResource,
                builder -> builder.operationId("DeleteResource")
                    .description("删除资源")
                    .tag(tag)
                    .parameter(parameterBuilder()
                        .name("name")
                        .in(ParameterIn.PATH)
                        .required(true)))
            .build();
    }

    @Override
    public GroupVersion groupVersion() {
        return GroupVersion.parseAPIVersion("api.my-plugin.halo.run/v1alpha1");
    }

    // 处理方法实现
    private Mono<ServerResponse> listResources(ServerRequest request) {
        return client.listAll(MyResource.class, ListOptions.builder().build())
            .collectList()
            .flatMap(list -> ServerResponse.ok().bodyValue(list));
    }

    private Mono<ServerResponse> getResource(ServerRequest request) {
        String name = request.pathVariable("name");
        return client.fetch(MyResource.class, name)
            .flatMap(resource -> ServerResponse.ok().bodyValue(resource))
            .switchIfEmpty(ServerResponse.notFound().build());
    }

    private Mono<ServerResponse> createResource(ServerRequest request) {
        return request.bodyToMono(CreateResourceRequest.class)
            .flatMap(req -> {
                var resource = new MyResource();
                resource.setMetadata(new Metadata());
                resource.getMetadata().setGenerateName("my-resource-");
                resource.setSpec(new MyResource.Spec());
                resource.getSpec().setName(req.getName());
                return client.create(resource);
            })
            .flatMap(created -> ServerResponse.ok().bodyValue(created));
    }

    private Mono<ServerResponse> updateResource(ServerRequest request) {
        String name = request.pathVariable("name");
        return request.bodyToMono(UpdateResourceRequest.class)
            .flatMap(req -> client.fetch(MyResource.class, name)
                .flatMap(resource -> {
                    resource.getSpec().setName(req.getName());
                    return client.update(resource);
                }))
            .flatMap(updated -> ServerResponse.ok().bodyValue(updated))
            .switchIfEmpty(ServerResponse.notFound().build());
    }

    private Mono<ServerResponse> deleteResource(ServerRequest request) {
        String name = request.pathVariable("name");
        return client.fetch(MyResource.class, name)
            .flatMap(client::delete)
            .flatMap(deleted -> ServerResponse.ok().bodyValue(deleted))
            .switchIfEmpty(ServerResponse.notFound().build());
    }
}
```

## 必要的 Import

```java
import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.core.fn.builders.content.Builder.contentBuilder;
import static org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder;
import static org.springdoc.core.fn.builders.requestbody.Builder.requestBodyBuilder;
import static org.springdoc.core.fn.builders.schema.Builder.schemaBuilder;

import io.swagger.v3.oas.annotations.enums.ParameterIn;
import org.springdoc.webflux.core.fn.SpringdocRouteBuilder;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.endpoint.CustomEndpoint;
import run.halo.app.extension.GroupVersion;
import run.halo.app.extension.ListResult;
import run.halo.app.extension.ReactiveExtensionClient;
```

## 分页查询

### 使用 SortableRequest

`SortableRequest` 提供了标准的分页和排序参数处理：

```java
public class MyResourceQuery extends SortableRequest {

    public MyResourceQuery(ServerWebExchange exchange) {
        super(exchange);
    }

    // 自定义查询参数
    @Schema(description = "按关键词搜索")
    public String getKeyword() {
        return queryParams.getFirst("keyword");
    }

    @Schema(description = "按状态过滤")
    public String getStatus() {
        return queryParams.getFirst("status");
    }

    // 构建 OpenAPI 参数文档
    public static void buildParameters(Builder builder) {
        SortableRequest.buildParameters(builder);
        builder.parameter(parameterBuilder()
            .in(ParameterIn.QUERY)
            .name("keyword")
            .implementation(String.class)
            .required(false)
            .description("搜索关键词"));
        builder.parameter(parameterBuilder()
            .in(ParameterIn.QUERY)
            .name("status")
            .implementation(String.class)
            .required(false)
            .description("状态过滤"));
    }
}
```

### 分页查询实现

```java
private Mono<ServerResponse> listResources(ServerRequest request) {
    var query = new MyResourceQuery(request.exchange());
    
    return client.listBy(MyResource.class, query.toListOptions(), query.toPageRequest())
        .flatMap(listResult -> ServerResponse.ok().bodyValue(listResult));
}
```

### SortableRequest 提供的标准参数

| 参数 | 类型 | 说明 |
|-----|------|------|
| `page` | Integer | 页码，从 0 开始 |
| `size` | Integer | 每页大小 |
| `sort` | String[] | 排序字段，如 `metadata.creationTimestamp,desc` |
| `labelSelector` | String[] | 标签选择器，如 `status=published` |
| `fieldSelector` | String[] | 字段选择器，如 `metadata.name==my-resource` |

## 请求体验证

使用 Bean Validation 注解验证请求参数：

```java
@Data
public class CreateResourceRequest {
    
    @NotBlank(message = "名称不能为空")
    @Size(max = 100, message = "名称长度不能超过100")
    private String name;
    
    @Email(message = "邮箱格式不正确")
    private String email;
    
    @Min(value = 0, message = "数量不能为负数")
    private Integer count;
}
```

在处理方法中验证：

```java
private Mono<ServerResponse> createResource(ServerRequest request) {
    return request.bodyToMono(CreateResourceRequest.class)
        .doOnNext(this::validate)  // 验证
        .flatMap(req -> {
            // 处理逻辑
        });
}

private void validate(Object target) {
    var violations = validator.validate(target);
    if (!violations.isEmpty()) {
        throw new ConstraintViolationException(violations);
    }
}
```

## MVC 风格 API（@ApiVersion）

除了函数式端点，也可以使用传统的 `@RestController` 风格，配合 `@ApiVersion` 注解：

```java
@ApiVersion("v1alpha1")
@RestController
@RequestMapping("/my-resources")
@RequiredArgsConstructor
public class MyResourceController {

    private final ExtensionClient client;

    @GetMapping
    public List<MyResource> list() {
        return client.listAll(MyResource.class, ListOptions.builder().build());
    }

    @GetMapping("/{name}")
    public MyResource get(@PathVariable String name) {
        return client.fetch(MyResource.class, name)
            .orElseThrow(() -> new NotFoundException("Resource not found"));
    }

    @PostMapping
    public MyResource create(@RequestBody @Valid CreateResourceRequest request) {
        var resource = new MyResource();
        // 设置属性
        return client.create(resource);
    }
}
```

**注意**：`@ApiVersion` 方式生成的 API 路径为 `/apis/{plugin-name}/v1alpha1/...`

## 自动生成的 CRUD API

对于使用 `@GVK` 注解定义的自定义模型，Halo 会自动生成标准的 CRUD API：

```
GET    /apis/{group}/{version}/{plural}           # 列表
GET    /apis/{group}/{version}/{plural}/{name}    # 获取
POST   /apis/{group}/{version}/{plural}           # 创建
PUT    /apis/{group}/{version}/{plural}/{name}    # 更新
DELETE /apis/{group}/{version}/{plural}/{name}    # 删除
```

例如，对于以下模型：

```java
@GVK(group = "my-plugin.halo.run",
     version = "v1alpha1",
     kind = "MyResource",
     plural = "myresources",
     singular = "myresource")
public class MyResource extends AbstractExtension {
    // ...
}
```

自动生成的 API：

```
GET    /apis/my-plugin.halo.run/v1alpha1/myresources
GET    /apis/my-plugin.halo.run/v1alpha1/myresources/{name}
POST   /apis/my-plugin.halo.run/v1alpha1/myresources
PUT    /apis/my-plugin.halo.run/v1alpha1/myresources/{name}
DELETE /apis/my-plugin.halo.run/v1alpha1/myresources/{name}
```

## 错误处理

### 返回错误响应

```java
private Mono<ServerResponse> getResource(ServerRequest request) {
    String name = request.pathVariable("name");
    return client.fetch(MyResource.class, name)
        .flatMap(resource -> ServerResponse.ok().bodyValue(resource))
        .switchIfEmpty(Mono.defer(() -> 
            ServerResponse.status(HttpStatus.NOT_FOUND)
                .bodyValue(new ErrorResponse("Resource not found: " + name))));
}
```

### 使用 Halo 异常

```java
import run.halo.app.infra.exception.NotFoundException;

private Mono<ServerResponse> getResource(ServerRequest request) {
    String name = request.pathVariable("name");
    return client.fetch(MyResource.class, name)
        .switchIfEmpty(Mono.error(new NotFoundException("Resource not found: " + name)))
        .flatMap(resource -> ServerResponse.ok().bodyValue(resource));
}
```

## 文件上传

```java
.POST("/upload", this::uploadFile,
    builder -> builder.operationId("UploadFile")
        .description("上传文件")
        .tag(tag)
        .requestBody(requestBodyBuilder()
            .required(true)
            .content(contentBuilder()
                .mediaType(MediaType.MULTIPART_FORM_DATA_VALUE)
                .schema(schemaBuilder()
                    .implementation(FileUploadRequest.class))))
        .response(responseBuilder()
            .implementation(UploadResult.class)))

private Mono<ServerResponse> uploadFile(ServerRequest request) {
    return request.multipartData()
        .flatMap(parts -> {
            var filePart = (FilePart) parts.getFirst("file");
            if (filePart == null) {
                return Mono.error(new IllegalArgumentException("File is required"));
            }
            // 处理文件
            return processFile(filePart);
        })
        .flatMap(result -> ServerResponse.ok().bodyValue(result));
}

@Data
public class FileUploadRequest {
    @Schema(type = "string", format = "binary")
    private FilePart file;
}
```

## 权限控制

自定义 API 的权限通过 `RoleTemplate` 配置：

```yaml
# resources/extensions/role-templates.yaml
apiVersion: v1alpha1
kind: RoleTemplate
metadata:
  name: my-plugin-manage
  labels:
    halo.run/role-template: "true"
  annotations:
    rbac.authorization.halo.run/module: "我的插件"
    rbac.authorization.halo.run/display-name: "资源管理"
rules:
  - apiGroups: ["api.my-plugin.halo.run"]
    resources: ["myresources"]
    verbs: ["*"]
```

## 完整示例

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class BookEndpoint implements CustomEndpoint {

    private final ReactiveExtensionClient client;

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        final var tag = "BookV1alpha1";
        return SpringdocRouteBuilder.route()
            .GET("/books", this::listBooks,
                builder -> {
                    builder.operationId("ListBooks")
                        .description("分页查询书籍列表")
                        .tag(tag)
                        .response(responseBuilder()
                            .implementation(ListResult.generateGenericClass(Book.class)));
                    BookQuery.buildParameters(builder);
                })
            .GET("/books/{name}", this::getBook,
                builder -> builder.operationId("GetBook")
                    .description("获取书籍详情")
                    .tag(tag)
                    .parameter(parameterBuilder()
                        .name("name")
                        .in(ParameterIn.PATH)
                        .required(true))
                    .response(responseBuilder()
                        .implementation(Book.class)))
            .GET("/books/stats", this::getStats,
                builder -> builder.operationId("GetBookStats")
                    .description("获取书籍统计信息")
                    .tag(tag)
                    .response(responseBuilder()
                        .implementation(BookStats.class)))
            .build();
    }

    @Override
    public GroupVersion groupVersion() {
        return GroupVersion.parseAPIVersion("api.book-plugin.halo.run/v1alpha1");
    }

    private Mono<ServerResponse> listBooks(ServerRequest request) {
        var query = new BookQuery(request.exchange());
        return client.listBy(Book.class, query.toListOptions(), query.toPageRequest())
            .flatMap(result -> ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(result));
    }

    private Mono<ServerResponse> getBook(ServerRequest request) {
        String name = request.pathVariable("name");
        return client.fetch(Book.class, name)
            .flatMap(book -> ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(book))
            .switchIfEmpty(ServerResponse.notFound().build());
    }

    private Mono<ServerResponse> getStats(ServerRequest request) {
        return client.listAll(Book.class, ListOptions.builder().build())
            .collectList()
            .map(books -> new BookStats(books.size()))
            .flatMap(stats -> ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(stats));
    }

    @Data
    @AllArgsConstructor
    public static class BookStats {
        private int totalCount;
    }
}
```

## 最佳实践

1. **API 版本管理**：从 `v1alpha1` 开始，稳定后升级到 `v1beta1`，最终到 `v1`
2. **命名规范**：使用 `{plugin-name}.halo.run` 作为 group
3. **OpenAPI 文档**：使用 `SpringdocRouteBuilder` 自动生成 API 文档
4. **错误处理**：统一使用 Halo 提供的异常类
5. **分页查询**：继承 `SortableRequest` 实现标准分页
6. **权限控制**：通过 `RoleTemplate` 配置 API 权限
