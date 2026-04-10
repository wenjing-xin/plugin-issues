# Halo 插件 Devtools 开发工具

本文档介绍 Halo 插件开发工具 `run.halo.plugin.devtools` 的使用，包括生成 API Client 和角色模板。

## 安装

在 `build.gradle` 中添加：

```groovy
plugins {
  id "run.halo.plugin.devtools" version "0.4.0"
}
```

## 基础配置

```groovy
halo {
  version = '2.20'                    // Halo 版本
  superAdminUsername = 'admin'        // 管理员用户名
  superAdminPassword = 'admin'        // 管理员密码
  externalUrl = 'http://localhost:8090'
  port = 8090                         // 端口号
  debug = true                        // 开启调试模式
  debugPort = 5005                    // 调试端口
  docker {
    url = 'unix:///var/run/docker.sock'  // Mac/Linux
    // url = 'npipe:////./pipe/docker_engine'  // Windows
    apiVersion = '1.42'
  }
}
```

## 常用任务

```bash
# 启动 Halo 服务
./gradlew haloServer

# 重新加载插件（修改代码后）
./gradlew reload

# 监听文件变化并自动重载
./gradlew watch

# 生成 API Client
./gradlew generateApiClient

# 生成角色模板
./gradlew generateRoleTemplates
```

## 生成 API Client

### 什么是 API Client

API Client 是根据 OpenAPI 规范自动生成的 TypeScript 客户端代码，提供：
- 类型安全的 API 调用
- 自动化 HTTP 请求封装
- 统一的错误处理

### 配置 OpenAPI 和生成规则

在 `build.gradle` 中配置：

```groovy
haloPlugin {
  openApi {
    // API 分组规则
    groupingRules {
      // 分组名称（可自定义）
      extensionApis {
        // 分组显示名称
        displayName = 'Extension API for my-plugin'
        // API 路径匹配规则（Ant 风格）
        pathsToMatch = [
          '/apis/my-plugin.halo.run/v1alpha1/**',
          '/apis/console.api.my-plugin.halo.run/v1alpha1/**'
        ]
      }
    }
    
    // 分组映射（固定写法）
    groupedApiMappings = [
      '/v3/api-docs/extensionApis': 'extensionApis.json'
    ]
    
    // 生成器配置
    generator {
      // API Client 输出目录
      outputDir = file("${projectDir}/console/src/api/generated")
      
      // 可选：额外配置
      additionalProperties = [
        useES6: true,
        useSingleRequestParameter: true,
        withSeparateModelsAndApi: true,
        apiPackage: "api",
        modelPackage: "models"
      ]
      
      // 类型映射
      typeMappings = [
        set: "Array"
      ]
    }
  }
}
```

### 执行生成

```bash
./gradlew generateApiClient
```

生成后会在 `console/src/api/generated` 目录下创建 API 客户端代码。

### 创建 API 实例

在 `console/src/api/index.ts` 中创建实例：

```typescript
import { axiosInstance } from "@halo-dev/api-client"
import {
  // 自定义模型生成的 API
  BookV1alpha1Api,
  // Console 端 API（根据 tag 名称导入）
  ConsoleApiBookPluginHaloRunV1alpha1BookApi,
  // UC 端 API
  UcApiBookPluginHaloRunV1alpha1BookApi,
} from "./generated"

// 自定义模型 CRUD API
const bookCoreApiClient = {
  book: new BookV1alpha1Api(undefined, "", axiosInstance),
}

// Console 端 API
const bookConsoleApiClient = {
  book: new ConsoleApiBookPluginHaloRunV1alpha1BookApi(
    undefined,
    "",
    axiosInstance
  ),
}

// UC 端 API
const bookUcApiClient = {
  book: new UcApiBookPluginHaloRunV1alpha1BookApi(
    undefined,
    "",
    axiosInstance
  ),
}

export { bookCoreApiClient, bookConsoleApiClient, bookUcApiClient }
```

### 使用生成的 API Client

```typescript
import { bookConsoleApiClient } from "@/api"

// 查询列表
const { data } = await bookConsoleApiClient.book.listBooks({
  page: 1,
  size: 10,
  keyword: "搜索关键词",
})

// 获取单个
const { data: book } = await bookCoreApiClient.book.getBook({
  name: "book-123",
})

// 创建
await bookCoreApiClient.book.createBook({
  book: {
    apiVersion: "book-plugin.halo.run/v1alpha1",
    kind: "Book",
    metadata: { name: "", generateName: "book-" },
    spec: { title: "新书", author: "作者" },
  },
})
```

### 后端 API 文档声明

要生成 API Client，后端需要使用 `SpringdocRouteBuilder` 声明 API 文档：

```java
@Override
public RouterFunction<ServerResponse> endpoint() {
    final var tag = "BookV1alpha1Console";  // 这个 tag 会成为生成的类名
    return SpringdocRouteBuilder.route()
        .GET("books", this::listBooks, builder -> {
            builder.operationId("ListBooks")
                .description("List all books")
                .tag(tag)
                .response(responseBuilder()
                    .implementation(ListResult.generateGenericClass(Book.class)));
            BookQuery.buildParameters(builder);
        })
        .GET("books/{name}", this::getBook, builder -> {
            builder.operationId("GetBook")
                .description("Get book by name")
                .tag(tag)
                .parameter(parameterBuilder()
                    .name("name")
                    .in(ParameterIn.PATH)
                    .required(true))
                .response(responseBuilder()
                    .implementation(Book.class));
        })
        .build();
}
```

## 生成角色模板 (generateRoleTemplates)

### 作用

自动根据 OpenAPI 文档生成 `roleTemplates.yaml` 文件，简化权限配置。

### 配置

使用与 `generateApiClient` 相同的 `openApi` 配置。

### 执行生成

```bash
./gradlew generateRoleTemplates
```

生成的文件位于 `workplace` 目录下。

### 生成结果示例

```yaml
apiVersion: v1alpha1
kind: Role
metadata:
  name: book-plugin-role
  labels:
    halo.run/role-template: "true"
  annotations:
    rbac.authorization.halo.run/module: "书籍管理"
    rbac.authorization.halo.run/display-name: "书籍管理"
rules:
  - apiGroups: ["book-plugin.halo.run"]
    resources: ["books"]
    verbs: ["get", "list", "create", "update", "delete"]
```

### 调整角色模板

生成的模板包含所有可能的操作，需要根据实际需求调整：

**查看权限角色：**
```yaml
rules:
  - apiGroups: ["book-plugin.halo.run"]
    resources: ["books"]
    verbs: ["get", "list"]  # 只保留读取权限
```

**管理权限角色：**
```yaml
rules:
  - apiGroups: ["book-plugin.halo.run"]
    resources: ["books"]
    verbs: ["get", "list", "create", "update", "delete"]  # 完整权限
```

## 调试后端代码

### 配置调试模式

```groovy
halo {
  debug = true
  debugPort = 5005
  suspend = true  // 启动时挂起等待调试器连接
}
```

### 使用 IDEA 调试

1. 运行 `./gradlew haloServer`
2. 在 IDEA 中点击 "Attach debugger" 连接调试器
3. 设置断点开始调试

## 监听文件变化

### 默认监听目录

- `src/main/java`
- `src/main/resources`

### 自定义监听目录

```groovy
haloPlugin {
  watchDomains {
    consoleSource {
      files files('console/src/')
      // 排除规则
      // exclude '**/node_modules/**'
      // exclude '**/.git/**'
    }
  }
}
```

## 工作目录

Halo 数据挂载到 `workplace` 目录，重启不会丢失数据。

### 自定义 Halo 配置

创建 `workplace/config/application.yaml`：

```yaml
logging:
  level:
    run.halo.app: DEBUG
```

## 注意事项

1. **版本要求**：使用 Halo 2.20.x 需要 devtools 0.2.0+
2. **Gradle 版本**：devtools 0.2.x+ 需要 Gradle 8.3+
3. **生成前检查**：执行 `generateApiClient` 前先通过 Swagger UI 检查 API 分组是否正确
4. **输出目录**：`generateApiClient` 会清空输出目录，建议使用独立目录
5. **Docker 环境**：`haloServer` 和 `watch` 任务需要 Docker 环境

## 升级 Gradle

```bash
./gradlew wrapper --gradle-version=8.9
```
