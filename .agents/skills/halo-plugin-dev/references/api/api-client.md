# Halo API Client 请求指南

本文档介绍如何在插件 UI 部分使用 `@halo-dev/api-client` 进行 API 请求。

## 安装

```bash
pnpm install @halo-dev/api-client axios
```

> 注意：使用 `@halo-dev/api-client@2.17.0` 需要将 `plugin.yaml` 中的 `spec.requires` 设置为 `>=2.17.0`

## 模块介绍

```typescript
import {
  // 预配置的 API Client（插件内直接使用）
  coreApiClient,      // 自定义模型 CRUD 接口
  consoleApiClient,   // Console 管理端接口
  ucApiClient,        // 用户中心接口
  publicApiClient,    // 公开访问接口
  
  // 创建自定义 API Client（外部应用使用）
  createCoreApiClient,
  createConsoleApiClient,
  createUcApiClient,
  createPublicApiClient,
  
  // axios 实例
  axiosInstance,
  
  // 分页工具
  paginate,
} from "@halo-dev/api-client"
```

### API Client 分类

| Client | 说明 | API 路径前缀 |
|--------|------|-------------|
| `coreApiClient` | 自定义模型 CRUD | `/apis/{group}/{version}/{plural}` |
| `consoleApiClient` | Console 管理端 | `/apis/console.api.*/v1alpha1/` |
| `ucApiClient` | 用户中心 | `/apis/uc.api.*/v1alpha1/` |
| `publicApiClient` | 公开访问 | `/apis/api.*/v1alpha1/` |

## 在插件中使用

### 调用 Halo 内置 API

直接使用预配置的 API Client，无需任何配置：

```typescript
import { coreApiClient, consoleApiClient } from "@halo-dev/api-client"

// 获取文章列表
const { data } = await coreApiClient.content.post.listPost({
  page: 1,
  size: 10,
})

// 获取单篇文章
const { data: post } = await coreApiClient.content.post.getPost({
  name: "post-abc123",
})

// 创建文章
await consoleApiClient.content.post.draftPost({
  postRequest: {
    post: {
      spec: {
        title: "新文章",
        slug: "new-post",
      },
      apiVersion: "content.halo.run/v1alpha1",
      kind: "Post",
      metadata: {
        name: "",
        generateName: "post-",
      },
    },
    content: {
      raw: "文章内容",
      content: "<p>文章内容</p>",
      rawType: "markdown",
    },
  },
})

// 删除文章
await coreApiClient.content.post.deletePost({
  name: "post-abc123",
})
```

### 调用插件自定义 API

使用 `axiosInstance` 调用插件提供的接口：

```typescript
import { axiosInstance } from "@halo-dev/api-client"

// GET 请求
const { data } = await axiosInstance.get<BookList>(
  "/apis/api.book-plugin.halo.run/v1alpha1/books"
)

// POST 请求
await axiosInstance.post(
  "/apis/api.book-plugin.halo.run/v1alpha1/books",
  {
    spec: {
      title: "新书",
      author: "作者",
    },
  }
)

// PUT 请求
await axiosInstance.put(
  "/apis/api.book-plugin.halo.run/v1alpha1/books/book-123",
  bookData
)

// DELETE 请求
await axiosInstance.delete(
  "/apis/api.book-plugin.halo.run/v1alpha1/books/book-123"
)
```

### 封装插件 API Client

推荐为插件创建专门的 API 模块：

```typescript
// src/api/book.ts
import { axiosInstance } from "@halo-dev/api-client"
import type { Book, BookList, CreateBookRequest } from "@/types"

const BASE_URL = "/apis/api.book-plugin.halo.run/v1alpha1"

export const bookApi = {
  // 列表查询
  list: (params?: { page?: number; size?: number; keyword?: string }) => {
    return axiosInstance.get<BookList>(`${BASE_URL}/books`, { params })
  },

  // 获取单个
  get: (name: string) => {
    return axiosInstance.get<Book>(`${BASE_URL}/books/${name}`)
  },

  // 创建
  create: (data: CreateBookRequest) => {
    return axiosInstance.post<Book>(`${BASE_URL}/books`, data)
  },

  // 更新
  update: (name: string, data: Book) => {
    return axiosInstance.put<Book>(`${BASE_URL}/books/${name}`, data)
  },

  // 删除
  delete: (name: string) => {
    return axiosInstance.delete(`${BASE_URL}/books/${name}`)
  },

  // 自定义操作
  publish: (name: string) => {
    return axiosInstance.put(`${BASE_URL}/books/${name}/publish`)
  },
}
```

使用：

```typescript
import { bookApi } from "@/api/book"

// 获取列表
const { data } = await bookApi.list({ page: 1, size: 10 })

// 创建
await bookApi.create({
  spec: { title: "新书", author: "作者" },
})
```

## 分页查询

### 使用 paginate 工具

```typescript
import { coreApiClient, paginate } from "@halo-dev/api-client"
import type { Post, PostV1alpha1ApiListPostRequest } from "@halo-dev/api-client"

// 获取所有数据（自动处理分页）
const allPosts = await paginate<PostV1alpha1ApiListPostRequest, Post>(
  (params) => coreApiClient.content.post.listPost(params),
  {
    size: 100,
    labelSelector: ["content.halo.run/published=true"],
  }
)

console.log(allPosts.length) // 所有文章数量
```

### 手动分页

```typescript
import { coreApiClient } from "@halo-dev/api-client"

const { data } = await coreApiClient.content.post.listPost({
  page: 1,
  size: 10,
  sort: ["metadata.creationTimestamp,desc"],
  labelSelector: ["content.halo.run/published=true"],
})

console.log(data.items)      // 当前页数据
console.log(data.total)      // 总数
console.log(data.page)       // 当前页码
console.log(data.size)       // 每页大小
console.log(data.totalPages) // 总页数
```

## 结合 Vue Query 使用

推荐使用 `@tanstack/vue-query` 管理请求状态：

```vue
<script lang="ts" setup>
import { coreApiClient } from "@halo-dev/api-client"
import { useQuery, useMutation, useQueryClient } from "@tanstack/vue-query"
import { ref } from "vue"

const queryClient = useQueryClient()
const keyword = ref("")
const page = ref(1)

// 查询列表
const { data, isLoading, refetch } = useQuery({
  queryKey: ["posts", keyword, page],
  queryFn: async () => {
    const { data } = await coreApiClient.content.post.listPost({
      page: page.value,
      size: 10,
      keyword: keyword.value || undefined,
    })
    return data
  },
})

// 删除操作
const { mutate: deletePost, isPending: isDeleting } = useMutation({
  mutationFn: async (name: string) => {
    await coreApiClient.content.post.deletePost({ name })
  },
  onSuccess: () => {
    // 刷新列表
    queryClient.invalidateQueries({ queryKey: ["posts"] })
  },
})
</script>

<template>
  <div v-if="isLoading">加载中...</div>
  <div v-else>
    <div v-for="post in data?.items" :key="post.metadata.name">
      {{ post.spec.title }}
      <button @click="deletePost(post.metadata.name)">删除</button>
    </div>
  </div>
</template>
```

## 常用 API 示例

### 用户相关

```typescript
import { coreApiClient, consoleApiClient } from "@halo-dev/api-client"

// 获取用户列表
const { data } = await coreApiClient.user.listUser()

// 获取当前用户
const { data: currentUser } = await consoleApiClient.user.getCurrentUserDetail()

// 创建用户
await coreApiClient.user.createUser({
  user: {
    apiVersion: "v1alpha1",
    kind: "User",
    metadata: { name: "new-user" },
    spec: {
      displayName: "新用户",
      email: "user@example.com",
    },
  },
})
```

### 附件相关

```typescript
import { coreApiClient, consoleApiClient } from "@halo-dev/api-client"

// 获取附件列表
const { data } = await coreApiClient.storage.attachment.listAttachment({
  page: 1,
  size: 20,
})

// 上传附件
const formData = new FormData()
formData.append("file", file)
formData.append("policyName", "default-policy")

await consoleApiClient.storage.attachment.uploadAttachment({
  file: file,
  policyName: "default-policy",
})
```

### 插件相关

```typescript
import { coreApiClient, consoleApiClient } from "@halo-dev/api-client"

// 获取插件列表
const { data } = await coreApiClient.plugin.plugin.listPlugin()

// 启用/禁用插件
await consoleApiClient.plugin.plugin.changePluginRunningState({
  name: "my-plugin",
  pluginRunningStateRequest: {
    enable: true,
  },
})

// 获取插件设置
const { data: setting } = await coreApiClient.plugin.plugin.fetchPluginSetting({
  name: "my-plugin",
})
```

### 分类和标签

```typescript
import { coreApiClient } from "@halo-dev/api-client"

// 获取分类列表
const { data: categories } = await coreApiClient.content.category.listCategory()

// 获取标签列表
const { data: tags } = await coreApiClient.content.tag.listTag()
```

## 错误处理

在 Halo 插件内部，API Client 已经处理了常见错误（登录失效、无权限等），但你仍可以添加自定义错误处理：

```typescript
import { axiosInstance } from "@halo-dev/api-client"
import { Toast } from "@halo-dev/components"

try {
  await axiosInstance.post("/apis/my-plugin.halo.run/v1alpha1/action")
  Toast.success("操作成功")
} catch (error) {
  if (error.response?.status === 400) {
    Toast.error("请求参数错误")
  } else if (error.response?.status === 404) {
    Toast.error("资源不存在")
  } else {
    Toast.error("操作失败")
  }
}
```

## 在外部应用中使用

如果需要在 Halo 外部的应用中使用 API Client：

```typescript
import axios from "axios"
import { createCoreApiClient, createConsoleApiClient } from "@halo-dev/api-client"

// 创建自定义 axios 实例
const axiosInstance = axios.create({
  baseURL: "https://your-halo-site.com",
  headers: {
    // 使用个人令牌认证
    Authorization: "Bearer pat_xxxxxxxxxxxx",
  },
})

// 添加响应拦截器处理错误
axiosInstance.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      console.error("认证失败")
    }
    return Promise.reject(error)
  }
)

// 创建 API Client
const coreApiClient = createCoreApiClient(axiosInstance)
const consoleApiClient = createConsoleApiClient(axiosInstance)

// 使用
const { data } = await coreApiClient.content.post.listPost()
```

## 类型定义

`@halo-dev/api-client` 提供了完整的 TypeScript 类型定义：

```typescript
import type {
  Post,
  PostList,
  Category,
  Tag,
  User,
  Attachment,
  Plugin,
  Role,
  // ... 更多类型
} from "@halo-dev/api-client"
```

## 最佳实践

1. **使用预配置 Client** - 在插件内优先使用 `coreApiClient` 等预配置的 Client
2. **封装插件 API** - 为插件自定义 API 创建专门的模块
3. **使用 Vue Query** - 结合 `@tanstack/vue-query` 管理请求状态和缓存
4. **类型安全** - 使用 TypeScript 获得完整的类型提示
5. **错误处理** - 对关键操作添加适当的错误处理和用户提示
