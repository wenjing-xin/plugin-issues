---
inclusion: manual
---

# Lit Web Components 跨端复用方案

本文档介绍如何使用 Lit 构建 Web Components，实现在 Halo 主题端和插件 UI 部分之间共享组件。

## 概述

当需要在主题端（前台）和插件 UI 部分共享 UI 组件时，推荐使用 Web Components 技术。Halo 官方的评论组件插件 `plugin-comment-widget` 就是采用这种方案。

**优势：**
- 框架无关：Web Components 是浏览器原生支持的标准
- 跨端复用：同一组件可在主题端（Thymeleaf）和 UI 部分（Vue）中使用
- 独立发布：可作为 npm 包独立发布，供第三方使用
- 样式隔离：Shadow DOM 提供样式封装

## 项目结构

推荐使用 Monorepo 结构组织项目：

```
my-plugin/
├── packages/
│   └── my-widget/                    # Web Components 包
│       ├── src/
│       │   ├── components/           # Lit 组件
│       │   │   ├── my-widget.ts
│       │   │   └── sub-component.ts
│       │   ├── styles/               # 样式
│       │   │   └── base.css
│       │   └── index.ts              # 入口文件
│       ├── package.json
│       ├── tsconfig.json
│       └── vite.config.ts
├── ui/                               # 插件 UI 部分
│   ├── src/
│   │   ├── index.ts
│   │   └── views/
│   ├── package.json
│   └── vite.config.ts
├── src/main/
│   ├── java/                         # 后端代码
│   └── resources/
│       ├── plugin.yaml
│       └── console/                  # 构建输出
├── build.gradle
├── pnpm-workspace.yaml
└── package.json
```

## pnpm-workspace.yaml

```yaml
packages:
  - 'packages/*'
  - 'ui'
```

## Web Components 包配置

### packages/my-widget/package.json

```json
{
  "name": "@my-plugin/my-widget",
  "version": "1.0.0",
  "type": "module",
  "main": "./dist/my-widget.es.js",
  "module": "./dist/my-widget.es.js",
  "types": "./dist/index.d.ts",
  "exports": {
    ".": {
      "import": "./dist/my-widget.es.js",
      "types": "./dist/index.d.ts"
    },
    "./var.css": "./dist/var.css"
  },
  "files": [
    "dist"
  ],
  "scripts": {
    "dev": "vite build --watch",
    "build": "vite build && tsc --emitDeclarationOnly"
  },
  "dependencies": {
    "lit": "^3.1.0"
  },
  "devDependencies": {
    "@types/node": "^20.0.0",
    "typescript": "^5.3.0",
    "vite": "^5.0.0"
  }
}
```

### packages/my-widget/vite.config.ts

```ts
import { defineConfig } from 'vite';
import { resolve } from 'path';

export default defineConfig({
  build: {
    lib: {
      entry: resolve(__dirname, 'src/index.ts'),
      name: 'MyWidget',
      formats: ['es', 'umd'],
      fileName: (format) => `my-widget.${format}.js`,
    },
    rollupOptions: {
      // 不外部化 lit，打包进组件
      external: [],
    },
    cssCodeSplit: false,
  },
});
```

### packages/my-widget/tsconfig.json

```json
{
  "compilerOptions": {
    "target": "ES2021",
    "module": "ESNext",
    "moduleResolution": "bundler",
    "lib": ["ES2021", "DOM", "DOM.Iterable"],
    "declaration": true,
    "declarationDir": "./dist",
    "emitDeclarationOnly": true,
    "strict": true,
    "noEmit": false,
    "experimentalDecorators": true,
    "useDefineForClassFields": false
  },
  "include": ["src/**/*.ts"]
}
```

## Lit 组件开发

### packages/my-widget/src/components/my-widget.ts

```ts
import { LitElement, html, css, PropertyValues } from 'lit';
import { customElement, property, state } from 'lit/decorators.js';

@customElement('my-widget')
export class MyWidget extends LitElement {
  // 外部属性
  @property({ type: String })
  baseUrl = '';

  @property({ type: String })
  group = '';

  @property({ type: String })
  kind = '';

  @property({ type: String })
  name = '';

  // 内部状态
  @state()
  private loading = false;

  @state()
  private data: any[] = [];

  // 样式定义
  static styles = css`
    :host {
      display: block;
      font-family: var(--my-widget-font-family, inherit);
    }

    .container {
      padding: var(--my-widget-padding, 16px);
      border-radius: var(--my-widget-border-radius, 8px);
      background: var(--my-widget-bg-color, #fff);
    }

    .title {
      color: var(--my-widget-text-color, #333);
      font-size: var(--my-widget-title-size, 18px);
      margin-bottom: 12px;
    }

    .loading {
      text-align: center;
      padding: 20px;
      color: var(--my-widget-muted-color, #999);
    }

    .item {
      padding: 8px 0;
      border-bottom: 1px solid var(--my-widget-border-color, #eee);
    }

    .item:last-child {
      border-bottom: none;
    }
  `;

  // 生命周期
  connectedCallback() {
    super.connectedCallback();
    this.fetchData();
  }

  updated(changedProperties: PropertyValues) {
    if (changedProperties.has('name') && this.name) {
      this.fetchData();
    }
  }

  // 数据获取
  private async fetchData() {
    if (!this.baseUrl || !this.name) return;

    this.loading = true;
    try {
      const response = await fetch(
        `${this.baseUrl}/apis/${this.group}/${this.kind}/${this.name}`
      );
      this.data = await response.json();
    } catch (error) {
      console.error('Failed to fetch data:', error);
    } finally {
      this.loading = false;
    }
  }

  // 渲染
  render() {
    return html`
      <div class="container">
        <h3 class="title">
          <slot name="title">My Widget</slot>
        </h3>
        
        ${this.loading
          ? html`<div class="loading">加载中...</div>`
          : html`
              <div class="list">
                ${this.data.map(
                  (item) => html`
                    <div class="item">
                      <slot name="item" .item=${item}>
                        ${item.name}
                      </slot>
                    </div>
                  `
                )}
              </div>
            `}
      </div>
    `;
  }
}

// 类型声明
declare global {
  interface HTMLElementTagNameMap {
    'my-widget': MyWidget;
  }
}
```

### packages/my-widget/src/index.ts

```ts
// 导出组件
export { MyWidget } from './components/my-widget';

// 自动注册（导入即注册）
import './components/my-widget';
```

### packages/my-widget/src/styles/var.css

```css
/* CSS 变量默认值，供主题覆盖 */
:root {
  --my-widget-font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  --my-widget-padding: 16px;
  --my-widget-border-radius: 8px;
  --my-widget-bg-color: #ffffff;
  --my-widget-text-color: #1f2937;
  --my-widget-muted-color: #6b7280;
  --my-widget-border-color: #e5e7eb;
  --my-widget-title-size: 18px;
  --my-widget-primary-color: #3b82f6;
}

/* 暗黑模式 */
@media (prefers-color-scheme: dark) {
  :root {
    --my-widget-bg-color: #1f2937;
    --my-widget-text-color: #f9fafb;
    --my-widget-muted-color: #9ca3af;
    --my-widget-border-color: #374151;
  }
}

/* 支持 class 切换 */
.dark,
[data-theme='dark'] {
  --my-widget-bg-color: #1f2937;
  --my-widget-text-color: #f9fafb;
  --my-widget-muted-color: #9ca3af;
  --my-widget-border-color: #374151;
}
```

## 在主题端使用

### 方式一：通过 TemplateHeadProcessor 注入

```java
@Component
@Order(100)
public class MyWidgetHeadProcessor implements TemplateHeadProcessor {

    @Override
    public Mono<Void> process(ITemplateContext context, IModel model,
            IElementModelStructureHandler structureHandler) {
        
        IModelFactory modelFactory = context.getModelFactory();
        model.add(modelFactory.createText("""
            <link rel="stylesheet" href="/plugins/my-plugin/assets/var.css">
            <script type="module" src="/plugins/my-plugin/assets/my-widget.es.js"></script>
            """));
        
        return Mono.empty();
    }
}
```

### 方式二：在主题模板中直接使用

```html
<!-- 引入样式和脚本 -->
<link rel="stylesheet" th:href="@{/plugins/my-plugin/assets/var.css}">
<script type="module" th:src="@{/plugins/my-plugin/assets/my-widget.es.js}"></script>

<!-- 使用组件 -->
<my-widget
  base-url="/"
  group="content.halo.run"
  kind="posts"
  name="my-post-name">
  <span slot="title">自定义标题</span>
</my-widget>
```

### 主题样式覆盖

```css
/* 在主题 CSS 中覆盖变量 */
:root {
  --my-widget-primary-color: #10b981;
  --my-widget-border-radius: 12px;
}
```

## 在插件 UI 部分使用

### ui/vite.config.ts

```ts
import { viteConfig } from "@halo-dev/ui-plugin-bundler-kit";
import vue from "@vitejs/plugin-vue";

export default viteConfig({
  vite: {
    plugins: [
      vue({
        template: {
          compilerOptions: {
            // 将 my-widget 标记为自定义元素
            isCustomElement: (tag) => tag === 'my-widget',
          },
        },
      }),
    ],
  },
});
```

### ui/src/views/MyView.vue

```vue
<script setup lang="ts">
import '@my-plugin/my-widget';
import '@my-plugin/my-widget/var.css';
</script>

<template>
  <my-widget
    base-url="/"
    group="content.halo.run"
    kind="posts"
    name="my-post-name"
  >
    <span slot="title">UI 组件标题</span>
  </my-widget>
</template>
```

## 构建配置

### build.gradle

```groovy
plugins {
    id 'run.halo.plugin.devtools' version '0.3.0'
}

halo {
    version = '2.20.0'
}

// 构建 Web Components
task buildWidget(type: PnpmTask) {
    args = ['--filter', '@my-plugin/my-widget', 'build']
}

// 复制构建产物到 resources
task copyWidgetAssets(type: Copy) {
    dependsOn buildWidget
    from 'packages/my-widget/dist'
    into 'src/main/resources/static/assets'
}

// 构建插件 UI
task buildUI(type: PnpmTask) {
    dependsOn copyWidgetAssets
    args = ['--filter', 'ui', 'build']
}

build.dependsOn buildUI
```

## 发布为 npm 包

如果希望组件可以被第三方使用（如 Headless CMS 场景），可以发布到 npm：

### 发布配置

```json
{
  "name": "@halo-dev/my-widget",
  "publishConfig": {
    "access": "public"
  }
}
```

### 第三方使用

```bash
pnpm install @halo-dev/my-widget
```

```vue
<!-- Vue 项目 -->
<script setup>
import '@halo-dev/my-widget';
import '@halo-dev/my-widget/var.css';
</script>

<template>
  <my-widget
    base-url="https://your-halo-site.com"
    group="content.halo.run"
    kind="Post"
    name="post-uuid"
  />
</template>
```

```tsx
// React 项目
import '@halo-dev/my-widget';
import '@halo-dev/my-widget/var.css';

function App() {
  return (
    <my-widget
      baseUrl="https://your-halo-site.com"
      group="content.halo.run"
      kind="Post"
      name="post-uuid"
    />
  );
}
```

## 最佳实践

### 1. CSS 变量命名规范

使用统一前缀，便于主题覆盖：

```css
--{plugin-name}-{component}-{property}

/* 示例 */
--my-widget-primary-color
--my-widget-text-color
--my-widget-border-radius
```

### 2. 属性命名

Web Components 属性使用 kebab-case：

```html
<my-widget base-url="..." post-name="..."></my-widget>
```

Lit 中使用 camelCase，会自动转换：

```ts
@property({ type: String })
baseUrl = '';  // 对应 base-url 属性
```

### 3. 事件通信

使用 CustomEvent 与外部通信：

```ts
// 组件内部
this.dispatchEvent(new CustomEvent('item-click', {
  detail: { item },
  bubbles: true,
  composed: true,  // 穿透 Shadow DOM
}));

// 外部监听
document.querySelector('my-widget')
  .addEventListener('item-click', (e) => {
    console.log(e.detail.item);
  });
```

### 4. Slot 插槽

提供灵活的内容定制：

```ts
render() {
  return html`
    <div class="header">
      <slot name="header">默认标题</slot>
    </div>
    <div class="content">
      <slot></slot>  <!-- 默认插槽 -->
    </div>
    <div class="footer">
      <slot name="footer"></slot>
    </div>
  `;
}
```

### 5. 响应式设计

```ts
static styles = css`
  :host {
    display: block;
  }

  @media (max-width: 768px) {
    .container {
      padding: 8px;
    }
  }
`;
```

## 参考资源

- [Lit 官方文档](https://lit.dev/)
- [Web Components MDN](https://developer.mozilla.org/en-US/docs/Web/Web_Components)
- [Halo 评论组件插件](https://github.com/halo-dev/plugin-comment-widget)
