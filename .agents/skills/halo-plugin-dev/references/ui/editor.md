---
inclusion: manual
---

# 编辑器扩展开发指南

本文档介绍如何扩展 Halo 默认编辑器的功能，以及如何在插件中使用编辑器组件。

## 概述

Halo 默认编辑器基于 [Tiptap](https://tiptap.dev/) 构建，支持以下扩展方式：

1. **扩展默认编辑器** - 通过 `default:editor:extension:create` 扩展点添加功能
2. **创建自定义编辑器** - 通过 `editor:create` 扩展点创建全新编辑器
3. **使用编辑器组件** - 在插件中使用 `RichTextEditor` 组件

## 扩展区域说明

编辑器支持以下扩展区域：

| 区域 | 扩展方法 | 说明 |
|------|----------|------|
| 顶部工具栏 | `getToolbarItems` | 常用操作按钮，如加粗、颜色等 |
| 工具箱 | `getToolboxItems` | 附属操作，如插入表格、组件等 |
| Slash Command | `getCommandMenuItems` | 斜杠命令，快捷执行功能 |
| 悬浮菜单 | `getBubbleMenu` | 选中内容时的悬浮操作菜单 |
| 拖拽菜单 | `getDraggableMenuItems` | 拖拽块时的操作菜单 |

## 扩展默认编辑器

### 定义方式

在插件的 `index.ts` 中注册扩展点：

```ts
import { definePlugin } from "@halo-dev/console-shared";
import MyExtension from "./editor/my-extension";

export default definePlugin({
  extensionPoints: {
    "default:editor:extension:create": () => {
      return [MyExtension];
    },
  },
});
```

### 创建 Tiptap Extension

```ts
// editor/my-extension.ts
import { Extension, type Editor } from "@halo-dev/richtext-editor";
import { markRaw } from "vue";
import ToolbarItem from "@halo-dev/richtext-editor/dist/components/toolbar/ToolbarItem.vue";
import ToolboxItem from "@halo-dev/richtext-editor/dist/components/toolbox/ToolboxItem.vue";
import MyIcon from "./icons/MyIcon.vue";

const MyExtension = Extension.create({
  name: "myExtension",

  addOptions() {
    return {
      ...this.parent?.(),

      // 顶部工具栏扩展
      getToolbarItems({ editor }: { editor: Editor }) {
        return {
          priority: 100,
          component: markRaw(ToolbarItem),
          props: {
            editor,
            isActive: editor.isActive("myMark"),
            icon: markRaw(MyIcon),
            title: "我的功能",
            action: () => {
              // 执行操作
              editor.chain().focus().run();
            },
          },
        };
      },

      // 工具箱扩展
      getToolboxItems({ editor }: { editor: Editor }) {
        return {
          priority: 50,
          component: markRaw(ToolboxItem),
          props: {
            editor,
            icon: markRaw(MyIcon),
            title: "插入我的组件",
            description: "插入一个自定义组件",
            action: () => {
              // 插入内容
              editor.chain().focus().insertContent({
                type: "myNode",
                attrs: { /* ... */ },
              }).run();
            },
          },
        };
      },

      // Slash Command 扩展
      getCommandMenuItems() {
        return {
          priority: 100,
          icon: markRaw(MyIcon),
          title: "editor.extensions.my_extension.title",
          keywords: ["my", "custom", "zidingyi"],
          command: ({ editor, range }) => {
            editor.chain()
              .focus()
              .deleteRange(range)
              .insertContent({ type: "myNode" })
              .run();
          },
        };
      },

      // 悬浮菜单扩展
      getBubbleMenu({ editor }: { editor: Editor }) {
        return {
          pluginKey: "myBubbleMenu",
          shouldShow: ({ state }) => {
            return isActive(state, "myNode");
          },
          getRenderContainer(node) {
            return node.closest(".my-node-wrapper") || node;
          },
          items: [
            {
              priority: 10,
              props: {
                icon: markRaw(EditIcon),
                title: "编辑",
                action: () => {
                  // 编辑操作
                },
              },
            },
            {
              priority: 20,
              props: {
                icon: markRaw(DeleteIcon),
                title: "删除",
                action: () => {
                  editor.chain().focus().deleteSelection().run();
                },
              },
            },
          ],
        };
      },

      // 拖拽菜单扩展
      getDraggableMenuItems({ editor }: { editor: Editor }) {
        return {
          priority: 100,
          key: "my-action",
          title: "我的操作",
          icon: markRaw(MyIcon),
          action: ({ editor, node, pos, close }) => {
            // 执行操作
            close();
          },
        };
      },
    };
  },
});

export default MyExtension;
```

## 类型定义

### ExtensionOptions

```ts
interface ExtensionOptions {
  // 顶部工具栏扩展
  getToolbarItems?: ({ editor }: { editor: Editor }) => 
    ToolbarItemType | ToolbarItemType[];

  // Slash Command 扩展
  getCommandMenuItems?: () => 
    CommandMenuItemType | CommandMenuItemType[];

  // 悬浮菜单扩展
  getBubbleMenu?: ({ editor }: { editor: Editor }) => 
    NodeBubbleMenuType;

  // 工具箱扩展
  getToolboxItems?: ({ editor }: { editor: Editor }) => 
    ToolboxItemType | ToolboxItemType[];

  // 拖拽菜单扩展
  getDraggableMenuItems?: ({ editor }: { editor: Editor }) => 
    DragButtonType | DragButtonType[];
}
```

### ToolbarItemType

```ts
interface ToolbarItemType {
  priority: number;              // 优先级，数字越小越靠前
  component: Component;          // 组件，通常使用 ToolbarItem
  props: {
    editor: Editor;
    isActive: boolean;           // 是否激活状态
    disabled?: boolean;          // 是否禁用
    icon?: Component;            // 图标组件
    title?: string;              // 标题/提示
    action?: () => void;         // 点击操作
  };
  children?: ToolbarItemType[];  // 子菜单项
}
```

### ToolboxItemType

```ts
interface ToolboxItemType {
  priority: number;
  component: Component;          // 通常使用 ToolboxItem
  props: {
    editor: Editor;
    icon?: Component;
    title?: string;
    description?: string;        // 描述文本
    action?: () => void;
  };
}
```

### CommandMenuItemType

```ts
interface CommandMenuItemType {
  priority: number;
  icon: Component;
  title: string;                 // 支持 i18n key
  keywords: string[];            // 搜索关键词
  command: ({ editor, range }: { 
    editor: Editor; 
    range: Range;
  }) => void;
}
```

### NodeBubbleMenuType

```ts
interface NodeBubbleMenuType {
  pluginKey?: string;            // 唯一标识
  shouldShow: (props: {          // 显示条件
    editor: Editor;
    state: EditorState;
    node?: HTMLElement;
    view?: EditorView;
    from?: number;
    to?: number;
  }) => boolean;
  getRenderContainer?: (node: HTMLElement) => HTMLElement;
  tippyOptions?: Record<string, unknown>;
  component?: Component;         // 自定义组件（与 items 二选一）
  items?: BubbleItemType[];      // 菜单项（与 component 二选一）
  extendsKey?: string;           // 扩展已有菜单的 key
}

interface BubbleItemType {
  priority: number;
  component?: Component;         // 自定义组件
  key?: string;                  // 唯一标识
  props?: {
    isActive?: ({ editor }) => boolean;
    visible?: ({ editor }) => boolean;
    icon?: Component;
    iconStyle?: string;
    title?: string;
    action?: ({ editor }) => Component | void;
  };
}
```

### DragButtonType

```ts
interface DragButtonType {
  extendsKey?: string;           // 扩展已有菜单项
  key?: string;                  // 唯一标识
  priority?: number;
  title?: string | (() => string);
  icon?: Component;
  action?: ({ editor, node, pos, close }) => 
    Component | boolean | void | Promise<...>;
  iconStyle?: string;
  class?: string;
  visible?: ({ editor, node, pos }) => boolean;
  isActive?: ({ editor, node, pos }) => boolean;
  disabled?: ({ editor, node, pos }) => boolean;
  keyboard?: string;             // 快捷键
  component?: Component;         // 自定义组件
  children?: {                   // 子菜单
    component?: Component;
    items?: DragButtonItemProps[];
  };
}
```

## 扩展已有菜单

### 扩展拖拽菜单的"转换为"功能

```ts
import { CONVERT_TO_KEY } from "@halo-dev/richtext-editor";

getDraggableMenuItems({ editor }: { editor: Editor }) {
  return {
    extendsKey: CONVERT_TO_KEY,
    children: {
      items: [
        {
          priority: 100,
          icon: markRaw(MyIcon),
          title: "转换为我的格式",
          action: ({ editor }) => {
            editor.chain().focus().setMyNode().run();
          },
        },
      ],
    },
  };
}
```

### 扩展已有悬浮菜单

```ts
getBubbleMenu({ editor }: { editor: Editor }) {
  return {
    extendsKey: "imageBubbleMenu",  // 扩展图片悬浮菜单
    items: [
      {
        priority: 100,
        key: "my-image-action",
        props: {
          icon: markRaw(MyIcon),
          title: "我的图片操作",
          action: ({ editor }) => {
            // 操作
          },
        },
      },
    ],
  };
}
```

## 创建自定义编辑器

通过 `editor:create` 扩展点创建全新的编辑器：

```ts
// components/MarkdownEditor.vue
<template>
  <div class="markdown-editor">
    <textarea :value="raw" @input="onRawUpdate" />
    <div v-html="content" />
  </div>
</template>

<script lang="ts" setup>
import { watch } from "vue";
import { marked } from "marked";

const props = withDefaults(
  defineProps<{
    raw?: string;
    content: string;
  }>(),
  {
    raw: "",
    content: "",
  }
);

const emit = defineEmits<{
  (event: "update:raw", value: string): void;
  (event: "update:content", value: string): void;
}>();

function onRawUpdate(e: Event) {
  const raw = (e.target as HTMLTextAreaElement).value;
  emit("update:raw", raw);
}

watch(
  () => props.raw,
  () => {
    emit("update:content", marked(props.raw));
  }
);
</script>
```

```ts
// index.ts
import MarkdownEditor from "./components/MarkdownEditor.vue";
import { markRaw } from "vue";

export default definePlugin({
  extensionPoints: {
    "editor:create": () => {
      return [
        {
          name: "markdown-editor",
          displayName: "Markdown",
          logo: "/plugins/my-plugin/assets/markdown-logo.png",
          component: markRaw(MarkdownEditor),
          rawType: "markdown",
        },
      ];
    },
  },
});
```

**编辑器组件要求：**

- 必须实现 `v-model:raw` - 原始内容
- 必须实现 `v-model:content` - 渲染后的 HTML 内容

## 在插件中使用编辑器

### 使用 RichTextEditor 组件

```vue
<template>
  <RichTextEditor
    v-if="editor"
    :editor="editor"
    locale="zh-CN"
  />
</template>

<script lang="ts" setup>
import { 
  RichTextEditor, 
  VueEditor, 
  ExtensionsKit 
} from "@halo-dev/richtext-editor";
import { shallowRef, onMounted, onBeforeUnmount } from "vue";

const editor = shallowRef<VueEditor>();
const content = ref("");

onMounted(() => {
  editor.value = new VueEditor({
    content: "<p>初始内容</p>",
    extensions: [
      ExtensionsKit.configure({
        placeholder: {
          placeholder: "请输入内容...",
        },
      }),
    ],
    onUpdate: () => {
      content.value = editor.value?.getHTML() || "";
    },
  });
});

onBeforeUnmount(() => {
  editor.value?.destroy();
});
</script>
```

### ExtensionsKit 配置选项

```ts
ExtensionsKit.configure({
  // 图片上传
  image: {
    uploadImage: async (file: File) => {
      // 上传图片，返回 Attachment
      return attachment;
    },
  },
  // 图库上传
  gallery: {
    uploadImage: async (file: File) => attachment,
  },
  // 视频上传
  video: {
    uploadVideo: async (file: File) => attachment,
  },
  // 音频上传
  audio: {
    uploadAudio: async (file: File) => attachment,
  },
  // 占位符
  placeholder: {
    placeholder: "请输入内容...",
  },
  // 自定义扩展
  customExtensions: [
    MyExtension1,
    MyExtension2,
  ],
});
```

### 编辑器导出

`@halo-dev/richtext-editor` 包导出以下内容：

```ts
// 核心组件
export { RichTextEditor } from "./components/RichTextEditor.vue";
export { VueEditor } from "./editor/VueEditor";

// 扩展套件
export { ExtensionsKit } from "./extensions/extensions-kit";

// UI 组件
export { ToolbarItem } from "./components/toolbar/ToolbarItem.vue";
export { ToolboxItem } from "./components/toolbox/ToolboxItem.vue";

// Tiptap 核心
export { Editor, Extension, Node, Mark } from "@tiptap/core";
export { Plugin, PluginKey } from "@tiptap/pm/state";
export { DecorationSet } from "@tiptap/pm/view";

// 内置扩展
export { ExtensionHeading } from "./extensions/heading";
export { ExtensionImage } from "./extensions/image";
export { ExtensionTable } from "./extensions/table";
// ... 更多扩展

// 工具函数
export { isActive } from "./utils/isActive";
export { convertToMediaContents } from "./utils/media";

// 类型
export type {
  ExtensionOptions,
  ToolbarItemType,
  ToolboxItemType,
  CommandMenuItemType,
  NodeBubbleMenuType,
  BubbleItemType,
  DragButtonType,
} from "./types";
```

## 完整示例：代码高亮扩展

```ts
import { Extension, type Editor } from "@halo-dev/richtext-editor";
import { markRaw } from "vue";
import ToolboxItem from "@halo-dev/richtext-editor/dist/components/toolbox/ToolboxItem.vue";
import CodeIcon from "~icons/mdi/code-braces";

const CodeHighlightExtension = Extension.create({
  name: "codeHighlight",

  addOptions() {
    return {
      ...this.parent?.(),

      getToolboxItems({ editor }: { editor: Editor }) {
        return {
          priority: 60,
          component: markRaw(ToolboxItem),
          props: {
            editor,
            icon: markRaw(CodeIcon),
            title: "代码块",
            description: "插入带语法高亮的代码块",
            action: () => {
              editor.chain()
                .focus()
                .toggleCodeBlock()
                .run();
            },
          },
        };
      },

      getCommandMenuItems() {
        return {
          priority: 80,
          icon: markRaw(CodeIcon),
          title: "代码块",
          keywords: ["code", "codeblock", "daima"],
          command: ({ editor, range }) => {
            editor.chain()
              .focus()
              .deleteRange(range)
              .toggleCodeBlock()
              .run();
          },
        };
      },
    };
  },
});

export default CodeHighlightExtension;
```

## 最佳实践

1. **使用 `markRaw`** - 组件和图标必须用 `markRaw` 包装，避免响应式开销
2. **合理设置 priority** - 数字越小优先级越高，越靠前显示
3. **支持 i18n** - title 使用 i18n key，支持多语言
4. **添加 keywords** - Slash Command 添加拼音关键词，方便中文用户
5. **清理资源** - 在组件卸载时调用 `editor.destroy()`
6. **使用 shallowRef** - 编辑器实例使用 `shallowRef` 而非 `ref`

## 参考资源

- [Tiptap 官方文档](https://tiptap.dev/)
- [Halo 编辑器源码](https://github.com/halo-dev/halo/tree/main/ui/packages/editor)
