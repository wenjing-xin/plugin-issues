# Halo 插件 UI 扩展开发指南

## 概述

Halo 插件支持通过前端扩展点扩展 Console（管理控制台）的 UI。本指南涵盖路由注册、菜单配置、扩展点实现等内容。

## 项目结构

```
console/
├── src/
│   ├── index.ts              # 入口文件
│   ├── views/
│   │   └── MyView.vue        # 视图组件
│   └── components/
│       └── MyWidget.vue      # 组件
├── package.json
└── vite.config.ts
```

## 入口文件配置

使用 `definePlugin` 定义插件：

```ts
import { definePlugin } from "@halo-dev/ui-shared";
import { markRaw } from "vue";
import BasicLayout from "@console/layouts/BasicLayout.vue";
import MyView from "./views/MyView.vue";
import { IconPlug } from "@halo-dev/components";

export default definePlugin({
  name: "my-plugin",
  components: {},
  routes: [
    {
      parentName: "Root",
      route: {
        path: "/my-plugin",
        children: [
          {
            path: "",
            name: "MyPlugin",
            component: MyView,
            meta: {
              title: "我的插件",
              searchable: true,
              permissions: ["plugin:my-plugin:view"],
              menu: {
                name: "我的插件",
                group: "tool",
                icon: markRaw(IconPlug),
                priority: 0,
                mobile: true,
              },
            },
          },
        ],
      },
    },
  ],
  extensionPoints: {},
});
```

## 路由配置

### 路由 Meta 配置

```ts
interface RouteMeta {
  title?: string;              // 页面标题
  searchable?: boolean;        // 是否可搜索
  permissions?: string[];      // 权限标识
  core?: boolean;              // 是否核心功能
  menu?: {
    name: string;              // 菜单名称
    group?: CoreMenuGroupId;   // 菜单分组
    icon?: Component;          // 菜单图标
    priority: number;          // 排序优先级
    mobile?: boolean;          // 是否显示在移动端
  };
}
```

### 菜单分组

内置分组 ID：
- `dashboard` - 仪表盘
- `content` - 内容管理
- `interface` - 外观
- `system` - 系统
- `tool` - 工具

自定义分组：

```ts
menu: {
  name: "帖子",
  group: "社区",  // 自定义分组名
  icon: markRaw(IconCommunity),
  priority: 1,
}
```

## 扩展点

### 仪表盘小部件

```ts
import MyWidget from "./components/MyWidget.vue";

export default definePlugin({
  extensionPoints: {
    "console:dashboard:widgets:create": () => {
      return [
        {
          id: "my-widget",
          component: markRaw(MyWidget),
          group: "my-plugin",
          configFormKitSchema: [
            {
              $formkit: "text",
              name: "title",
              label: "标题",
              value: "默认标题",
            },
          ],
          defaultConfig: {
            title: "我的小部件",
          },
          defaultSize: {
            w: 6,
            h: 8,
            minW: 3,
            minH: 4,
          },
          permissions: ["plugin:my-plugin:view"],
        },
      ];
    },
  },
});
```

小部件组件：

```vue
<template>
  <WidgetCard v-bind="$attrs">
    <template #title>{{ config?.title }}</template>
    <div class="p-4">
      <p v-if="previewMode">预览模式</p>
      <p v-else>小部件内容</p>
    </div>
  </WidgetCard>
</template>

<script lang="ts" setup>
defineProps<{
  editMode?: boolean;
  previewMode?: boolean;
  config?: Record<string, unknown>;
}>();
</script>
```

### 快速操作项

```ts
export default definePlugin({
  extensionPoints: {
    "console:dashboard:widgets:internal:quick-action:item:create": () => {
      return [
        {
          id: "my-action",
          icon: markRaw(IconPlug),
          title: "我的操作",
          action: () => {
            // 执行操作
          },
          permissions: ["plugin:my-plugin:manage"],
        },
      ];
    },
  },
});
```

### 文章列表操作菜单

```ts
import type { ListedPost } from "@halo-dev/api-client";
import { VDropdownItem } from "@halo-dev/components";

export default definePlugin({
  extensionPoints: {
    "post:list-item:operation:create": () => {
      return [
        {
          priority: 21,
          component: markRaw(VDropdownItem),
          label: "导出为 Markdown",
          permissions: [],
          action: async (post: ListedPost) => {
            // 导出逻辑
          },
        },
      ];
    },
  },
});
```

### 文章列表字段扩展

```ts
import type { ListedPost } from "@halo-dev/api-client";
import { VEntityField } from "@halo-dev/components";

export default definePlugin({
  extensionPoints: {
    "post:list-item:field:create": (post: Ref<ListedPost>) => {
      return [
        {
          priority: 40,
          position: "end",
          component: markRaw(VEntityField),
          props: {
            title: "阅读量",
            description: post.value.stats?.visit || 0,
          },
        },
      ];
    },
  },
});
```

### 插件详情选项卡

```ts
import MyTabPanel from "./views/MyTabPanel.vue";

export default definePlugin({
  extensionPoints: {
    "plugin:self:tabs:create": () => {
      return [
        {
          id: "my-tab",
          label: "自定义面板",
          component: markRaw(MyTabPanel),
          permissions: [],
        },
      ];
    },
  },
});
```

### 编辑器扩展

```ts
import MarkdownEditor from "./components/MarkdownEditor.vue";

export default definePlugin({
  extensionPoints: {
    "editor:create": () => {
      return [
        {
          name: "markdown-editor",
          displayName: "Markdown",
          component: markRaw(MarkdownEditor),
          rawType: "markdown",
        },
      ];
    },
  },
});
```

编辑器组件必须实现 `v-model:raw` 和 `v-model:content`：

```vue
<template>
  <textarea :value="raw" @input="onRawUpdate" />
</template>

<script lang="ts" setup>
const props = defineProps<{
  raw?: string;
  content: string;
}>();

const emit = defineEmits<{
  (event: "update:raw", value: string): void;
  (event: "update:content", value: string): void;
}>();

function onRawUpdate(e: Event) {
  const raw = (e.target as HTMLTextAreaElement).value;
  emit("update:raw", raw);
  emit("update:content", marked(raw));
}
</script>
```

## 自定义 FormKit 输入组件

### 可用类型

| 类型 | 说明 |
|------|------|
| `code` | 代码编辑器 |
| `attachment` | 附件选择 |
| `repeater` | 对象集合 |
| `list` | 动态列表 |
| `menuCheckbox` | 多选菜单 |
| `menuRadio` | 单选菜单 |
| `postSelect` | 文章选择 |
| `singlePageSelect` | 页面选择 |
| `categorySelect` | 分类选择 |
| `tagSelect` | 标签选择 |
| `select` | 通用选择器 |
| `secret` | 密钥管理 |

### 使用示例

Vue 组件中：

```vue
<FormKit
  v-model="postName"
  type="postSelect"
  label="选择文章"
  placeholder="请选择文章"
  validation="required"
/>
```

FormKit Schema 中（插件设置表单）：

```yaml
- $formkit: postSelect
  name: featuredPost
  label: 推荐文章
  validation: required

- $formkit: categorySelect
  name: categories
  label: 分类
  multiple: true

- $formkit: repeater
  name: links
  label: 链接列表
  addLabel: 添加链接
  min: 1
  max: 10
  children:
    - $formkit: text
      name: title
      label: 标题
    - $formkit: url
      name: url
      label: 链接
```

### Select 组件

静态数据源：

```yaml
- $formkit: select
  name: country
  label: 国家
  clearable: true
  searchable: true
  options:
    - label: 中国
      value: cn
    - label: 美国
      value: us
```

远程数据源：

```yaml
- $formkit: select
  name: postName
  label: 选择文章
  clearable: true
  action: /apis/api.console.halo.run/v1alpha1/posts
  requestOption:
    method: GET
    pageField: page
    sizeField: size
    totalField: total
    itemsField: items
    labelField: post.spec.title
    valueField: post.metadata.name
```

## 业务组件

### AnnotationsForm

用于自定义模型的 annotations 数据：

```vue
<script setup lang="ts">
const annotationsFormRef = ref();

async function handleSubmit() {
  annotationsFormRef.value?.handleSubmit();
  await nextTick();

  const { customAnnotations, annotations, customFormInvalid, specFormInvalid } =
    annotationsFormRef.value || {};

  if (customFormInvalid || specFormInvalid) {
    return;
  }

  const finalAnnotations = {
    ...annotations,
    ...customAnnotations,
  };
}
</script>

<template>
  <AnnotationsForm
    ref="annotationsFormRef"
    :value="formState.metadata.annotations"
    kind="Post"
    group="content.halo.run"
  />
</template>
```

## 权限控制

在路由和扩展点中使用 `permissions` 字段控制访问权限：

```ts
{
  permissions: ["plugin:my-plugin:view", "plugin:my-plugin:manage"]
}
```

权限标识格式：`{scope}:{resource}:{action}`

## 最佳实践

1. **使用 markRaw**：组件引用使用 `markRaw()` 避免响应式转换
2. **权限控制**：为所有路由和扩展点配置适当的权限
3. **国际化**：使用 Vue I18n 支持多语言
4. **组件复用**：使用 `@halo-dev/components` 提供的基础组件
5. **类型安全**：使用 TypeScript 确保类型安全

## 常见问题

### 路由不显示

1. 检查 `parentName` 是否为 `"Root"`
2. 确认 `permissions` 配置正确
3. 检查用户是否有相应权限

### 扩展点不生效

1. 确认扩展点名称正确
2. 检查返回值格式是否符合要求
3. 使用 `markRaw()` 包装组件

### 样式问题

1. 使用 Tailwind CSS 类名
2. 遵循 Halo UI 设计规范
3. 使用 `@halo-dev/components` 组件
