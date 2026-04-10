# Halo 插件 UI 组件与工具库

本文档介绍 Halo 为插件 UI 部分提供的组件库和工具库。

## 共享工具库 (@halo-dev/ui-shared)

从 Halo 2.22 开始，提供了共享工具库 `@halo-dev/ui-shared`。

### 安装

```bash
pnpm install @halo-dev/ui-shared
```

### stores - 全局状态

#### currentUser - 当前用户

```typescript
import { stores } from "@halo-dev/ui-shared"
import { storeToRefs } from "pinia"

const userStore = stores.currentUser()

// 访问用户数据
console.log(userStore.currentUser?.user.metadata.name)

// 检查是否为匿名用户
console.log(userStore.isAnonymous)

// 使用 storeToRefs 保持响应性
const { currentUser, isAnonymous } = storeToRefs(stores.currentUser())
```

**属性：**
- `currentUser`: 当前登录用户详细信息 (`DetailedUser | undefined`)
- `isAnonymous`: 是否为匿名用户 (`boolean`)

#### globalInfo - 全局配置

```typescript
import { stores } from "@halo-dev/ui-shared"
import { storeToRefs } from "pinia"

const globalInfoStore = stores.globalInfo()

// 访问全局配置
console.log(globalInfoStore.globalInfo?.externalUrl)
console.log(globalInfoStore.globalInfo?.siteTitle)

const { globalInfo } = storeToRefs(stores.globalInfo())
```

**globalInfo 包含：**
- `externalUrl` - 外部访问地址
- `siteTitle` - 站点标题
- `timeZone` - 时区
- `locale` - 语言区域
- `allowComments` - 是否允许评论
- `allowRegistration` - 是否允许注册
- `favicon` - 网站图标

### utils - 工具方法

#### date - 日期处理

```typescript
import { utils } from "@halo-dev/ui-shared"

// 格式化日期
utils.date.format(new Date())                    // "2025-11-05 14:30"
utils.date.format("2025-10-22", "YYYY/MM/DD")   // "2025/10/22"

// 转换为 ISO 格式
utils.date.toISOString(new Date("2025-10-22"))  // "2025-10-22T00:00:00.000Z"

// 转换为 datetime-local 格式
utils.date.toDatetimeLocal(new Date())          // "2025-10-22T14:30"

// 相对时间
utils.date.timeAgo("2025-10-23")                // "1 天后"
utils.date.timeAgo("2025-10-21")                // "1 天前"

// 原始 dayjs 实例
utils.date.dayjs()
```

#### permission - 权限判断

```typescript
import { utils } from "@halo-dev/ui-shared"

// 检查是否拥有任意一个权限
utils.permission.has(["core:posts:manage"], true)  // true

// 检查是否拥有所有权限
utils.permission.has([
  "core:posts:manage",
  "core:attachments:view"
], false)  // true

// 获取当前用户权限列表
const permissions = utils.permission.getUserPermissions()
```

#### attachment - 附件处理

```typescript
import { utils } from "@halo-dev/ui-shared"

// 获取缩略图 URL
utils.attachment.getThumbnailUrl("/uploads/image.jpg", "M")
// 返回: "/uploads/image.jpg?width=800"

// 尺寸选项: "XL"(1600px), "L"(1200px), "M"(800px), "S"(400px)

// 从附件对象提取 URL
utils.attachment.getUrl(attachmentObject)

// 转换为简化格式
utils.attachment.convertToSimple(attachmentObject)
// 返回: { url: string, alt?: string, mediaType?: string }
```

#### id - ID 生成

```typescript
import { utils } from "@halo-dev/ui-shared"

// 生成 UUID v7
const id = utils.id.uuid()  // "018f1c2e-4fcb-7d04-9f21-1a2b3c4d5e6f"
```

### events - 事件总线

```typescript
import { events } from "@halo-dev/ui-shared"

// 监听插件配置更新事件
events.on("core:plugin:configMap:updated", (data) => {
  console.log(`插件 ${data.pluginName} 的配置已更新`)
  console.log(`配置组：${data.group}`)
  // 重新获取配置
})
```

## 基础组件库 (@halo-dev/components)

### 安装

```bash
pnpm install @halo-dev/components
```

### 常用基础组件

```vue
<script lang="ts" setup>
import {
  VButton,
  VCard,
  VModal,
  VInput,
  VTextarea,
  VSelect,
  VSwitch,
  VTag,
  VAvatar,
  VDropdown,
  VDropdownItem,
  VDropdownDivider,
  VEmpty,
  VLoading,
  VPageHeader,
  VStatusDot,
  VAlert,
  VTabs,
  VTabItem,
  Toast,
  Dialog,
} from "@halo-dev/components"
</script>
```

### VButton - 按钮

```vue
<VButton type="primary">主要按钮</VButton>
<VButton type="secondary">次要按钮</VButton>
<VButton type="danger">危险按钮</VButton>
<VButton size="sm">小按钮</VButton>
<VButton size="lg">大按钮</VButton>
<VButton :loading="isLoading">加载中</VButton>
<VButton circle>
  <IconPlus />
</VButton>
```

### VModal - 弹窗

```vue
<VModal
  v-model:visible="modalVisible"
  title="弹窗标题"
  :width="600"
  @close="handleClose"
>
  <template #actions>
    <VButton @click="modalVisible = false">取消</VButton>
    <VButton type="primary" @click="handleConfirm">确认</VButton>
  </template>
  
  <!-- 弹窗内容 -->
</VModal>
```

### VCard - 卡片

```vue
<VCard title="卡片标题">
  <template #actions>
    <VButton size="sm">操作</VButton>
  </template>
  
  <!-- 卡片内容 -->
</VCard>
```

### Toast - 消息提示

```typescript
import { Toast } from "@halo-dev/components"

Toast.success("操作成功")
Toast.error("操作失败")
Toast.warning("警告信息")
Toast.info("提示信息")
```

### Dialog - 对话框

```typescript
import { Dialog } from "@halo-dev/components"

Dialog.warning({
  title: "确认删除",
  description: "删除后无法恢复，是否继续？",
  confirmType: "danger",
  confirmText: "确认",
  cancelText: "取消",
  onConfirm: async () => {
    // 执行删除操作
  },
})

Dialog.info({
  title: "提示",
  description: "这是一条提示信息",
  confirmText: "知道了",
})
```

## VEntity 列表组件 ⭐

`VEntity` 是 Halo 中最重要的列表项组件，用于展示数据列表。

### 基本结构

```vue
<script lang="ts" setup>
import {
  VEntity,
  VEntityContainer,
  VEntityField,
  VAvatar,
  VTag,
  VStatusDot,
  VDropdownItem,
  VDropdownDivider,
} from "@halo-dev/components"
</script>

<template>
  <!-- 列表容器 -->
  <VEntityContainer>
    <!-- 列表项 -->
    <VEntity
      v-for="item in items"
      :key="item.metadata.name"
      :is-selected="selectedNames.includes(item.metadata.name)"
    >
      <!-- 复选框插槽 -->
      <template #checkbox>
        <input
          type="checkbox"
          :checked="selectedNames.includes(item.metadata.name)"
          @change="handleSelect(item)"
        />
      </template>
      
      <!-- 左侧内容 -->
      <template #start>
        <VEntityField>
          <template #description>
            <VAvatar :src="item.spec.avatar" size="md" />
          </template>
        </VEntityField>
        <VEntityField
          :title="item.spec.displayName"
          :description="item.metadata.name"
          :route="{ name: 'Detail', params: { name: item.metadata.name } }"
        >
          <template #extra>
            <VTag v-if="item.spec.disabled">已禁用</VTag>
          </template>
        </VEntityField>
      </template>
      
      <!-- 右侧内容 -->
      <template #end>
        <VEntityField>
          <template #description>
            <VTag v-for="tag in item.spec.tags" :key="tag">
              {{ tag }}
            </VTag>
          </template>
        </VEntityField>
        <VEntityField v-if="item.metadata.deletionTimestamp">
          <template #description>
            <VStatusDot state="warning" animate />
          </template>
        </VEntityField>
        <VEntityField>
          <template #description>
            <span class="text-xs text-gray-500">
              {{ utils.date.format(item.metadata.creationTimestamp) }}
            </span>
          </template>
        </VEntityField>
      </template>
      
      <!-- 下拉菜单 -->
      <template #dropdownItems>
        <VDropdownItem @click="handleEdit(item)">
          编辑
        </VDropdownItem>
        <VDropdownDivider />
        <VDropdownItem type="danger" @click="handleDelete(item)">
          删除
        </VDropdownItem>
      </template>
    </VEntity>
  </VEntityContainer>
</template>
```

### VEntity Props

| 属性 | 类型 | 说明 |
|-----|------|------|
| `is-selected` | `boolean` | 是否选中状态 |

### VEntity Slots

| 插槽 | 说明 |
|-----|------|
| `checkbox` | 复选框区域 |
| `start` | 左侧内容区域 |
| `end` | 右侧内容区域 |
| `dropdownItems` | 下拉菜单项 |

### VEntityField Props

| 属性 | 类型 | 说明 |
|-----|------|------|
| `title` | `string` | 标题 |
| `description` | `string` | 描述 |
| `route` | `RouteLocationRaw` | 点击跳转路由 |
| `width` | `string` | 宽度 |

### VEntityField Slots

| 插槽 | 说明 |
|-----|------|
| `description` | 自定义描述内容 |
| `extra` | 额外内容（显示在标题后） |

### 完整列表页示例

```vue
<script lang="ts" setup>
import type { Book } from "@/types"
import { coreApiClient } from "@halo-dev/api-client"
import {
  VButton,
  VCard,
  VEmpty,
  VEntity,
  VEntityContainer,
  VEntityField,
  VLoading,
  VPageHeader,
  VDropdownItem,
  VDropdownDivider,
  VStatusDot,
  Dialog,
  Toast,
} from "@halo-dev/components"
import { utils } from "@halo-dev/ui-shared"
import { useQuery, useQueryClient } from "@tanstack/vue-query"
import { ref, computed } from "vue"

const queryClient = useQueryClient()
const selectedNames = ref<string[]>([])
const keyword = ref("")

// 查询数据
const { data, isLoading, refetch } = useQuery({
  queryKey: ["books", keyword],
  queryFn: async () => {
    const { data } = await coreApiClient.book.listBooks({
      keyword: keyword.value,
    })
    return data.items
  },
})

// 处理选择
const handleSelectAll = (checked: boolean) => {
  if (checked) {
    selectedNames.value = data.value?.map(item => item.metadata.name) || []
  } else {
    selectedNames.value = []
  }
}

const handleSelect = (item: Book) => {
  const index = selectedNames.value.indexOf(item.metadata.name)
  if (index > -1) {
    selectedNames.value.splice(index, 1)
  } else {
    selectedNames.value.push(item.metadata.name)
  }
}

// 删除操作
const handleDelete = (item: Book) => {
  Dialog.warning({
    title: "确认删除",
    description: `确定要删除「${item.spec.title}」吗？`,
    confirmType: "danger",
    onConfirm: async () => {
      await coreApiClient.book.deleteBook({ name: item.metadata.name })
      Toast.success("删除成功")
      refetch()
    },
  })
}
</script>

<template>
  <VPageHeader title="书籍管理">
    <template #actions>
      <VButton type="primary" @click="handleCreate">
        新建
      </VButton>
    </template>
  </VPageHeader>

  <div class="m-4">
    <VCard>
      <!-- 搜索和筛选 -->
      <template #header>
        <div class="flex items-center gap-4">
          <SearchInput v-model="keyword" placeholder="搜索书籍" />
        </div>
      </template>

      <VLoading v-if="isLoading" />
      
      <VEmpty v-else-if="!data?.length" title="暂无数据" />
      
      <VEntityContainer v-else>
        <VEntity
          v-for="item in data"
          :key="item.metadata.name"
          :is-selected="selectedNames.includes(item.metadata.name)"
        >
          <template #checkbox>
            <input
              type="checkbox"
              :checked="selectedNames.includes(item.metadata.name)"
              @change="handleSelect(item)"
            />
          </template>
          
          <template #start>
            <VEntityField
              :title="item.spec.title"
              :description="item.spec.author"
            />
          </template>
          
          <template #end>
            <VEntityField>
              <template #description>
                <span class="text-xs text-gray-500">
                  {{ utils.date.format(item.metadata.creationTimestamp) }}
                </span>
              </template>
            </VEntityField>
          </template>
          
          <template #dropdownItems>
            <VDropdownItem @click="handleEdit(item)">
              编辑
            </VDropdownItem>
            <VDropdownDivider />
            <VDropdownItem type="danger" @click="handleDelete(item)">
              删除
            </VDropdownItem>
          </template>
        </VEntity>
      </VEntityContainer>
    </VCard>
  </div>
</template>
```

## 业务组件（全局注册）

以下组件已全局注册，可直接使用：

### UppyUpload - 文件上传

```vue
<UppyUpload
  :restrictions="{
    maxFileSize: 10 * 1024 * 1024,
    allowedFileTypes: ['image/*'],
  }"
  @uploaded="handleUploaded"
/>
```

### AttachmentSelectorModal - 附件选择

```vue
<AttachmentSelectorModal
  v-model:visible="selectorVisible"
  :accepts="['image/*']"
  @select="handleSelect"
/>
```

### HasPermission - 权限判断

```vue
<HasPermission :permission="['system:plugins:manage']">
  <VButton>需要权限的按钮</VButton>
</HasPermission>
```

### SearchInput - 搜索输入

```vue
<SearchInput
  v-model="keyword"
  placeholder="搜索..."
/>
```

### VCodemirror - 代码编辑器

```vue
<VCodemirror
  v-model="code"
  :language="'javascript'"
  :height="300"
/>
```

### AnnotationsForm - 元数据表单

```vue
<AnnotationsForm
  :group="'my-plugin.halo.run'"
  :kind="'Book'"
  :value="annotations"
  @change="handleAnnotationsChange"
/>
```

## 指令

### v-permission - 权限指令

```vue
<!-- 有权限时显示 -->
<VButton v-permission="['system:plugins:manage']">
  管理插件
</VButton>

<!-- 无权限时禁用 -->
<VButton v-permission:disabled="['system:plugins:manage']">
  管理插件
</VButton>
```

### v-tooltip - 提示指令

```vue
<VButton v-tooltip="'这是提示文字'">
  悬停显示提示
</VButton>

<VButton v-tooltip="{ content: '提示内容', placement: 'top' }">
  自定义位置
</VButton>
```

## 图标

```vue
<script lang="ts" setup>
import {
  IconAddCircle,
  IconArrowLeft,
  IconArrowRight,
  IconClose,
  IconDelete,
  IconEdit,
  IconEye,
  IconMore,
  IconPlus,
  IconRefresh,
  IconSearch,
  IconSettings,
  IconShieldUser,
} from "@halo-dev/components"
</script>

<template>
  <IconPlus />
  <IconEdit />
  <IconDelete />
</template>
```

## 最佳实践

1. **使用 VEntity 展示列表** - 保持 UI 一致性
2. **使用 Toast/Dialog** - 统一的消息提示和确认对话框
3. **使用 utils 工具** - 日期格式化、权限判断等
4. **监听配置更新** - 使用 events 监听插件配置变更
5. **权限控制** - 使用 v-permission 指令或 HasPermission 组件
6. **响应式数据** - 使用 storeToRefs 保持 store 数据响应性
