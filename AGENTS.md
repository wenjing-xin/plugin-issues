# AGENTS.md

本文件为 Claude Code（claude.ai/code）在此仓库中工作时提供指导。

## 项目概览

这是一个 Halo 2 插件仓库，包含 Java 后端和独立的 `ui/` 前端模块。

- 根 Gradle 项目负责构建插件 JAR，并将前端产物打包进插件资源中。
- `src/main/java` 存放插件入口和后端代码。
- `src/main/resources/plugin.yaml` 定义插件元数据、兼容版本、配置项名称及外部链接。
- `ui/` 是独立前端工程，使用 Vue 3 + Rsbuild 构建，随后在根构建过程中复制到 `build/resources/main/console`。

## 常用命令

### 后端 / 整体插件

- 构建插件 JAR：`./gradlew build`
- 运行全部检查：`./gradlew check`
- 仅运行后端测试：`./gradlew test`
- 运行单个后端测试类：`./gradlew test --tests com.webjing.issues.IssuesPluginTest`
- 启动带插件的 Halo 开发服务器：`./gradlew haloServer`

### 前端（`ui/`）

- 安装依赖：`cd ui && pnpm install`
- 启动前端监听构建（用于 Halo 开发）：`cd ui && pnpm dev`
- 构建前端产物：`cd ui && pnpm build`
- 运行前端单元测试：`cd ui && pnpm test:unit`
- 运行单个前端测试文件：`cd ui && pnpm vitest run src/path/to/test.spec.ts`
- 执行前端类型检查：`cd ui && pnpm type-check`
- 执行前端 Lint：`cd ui && pnpm lint`
- 格式化前端源码：`cd ui && pnpm prettier`

## 构建与打包流程

根目录 `build.gradle` 将前端打包流程接入插件构建：

1. `:ui:assemble` 负责执行 UI 构建。
2. 根任务 `processUiResources` 会把 `ui/build/dist` 复制到 `build/resources/main/console`。
3. 根任务 `classes` 依赖 `processUiResources`。
4. `./gradlew build` 最终会在 `build/libs` 下生成插件 JAR，其中包含后端类、`plugin.yaml` 和控制台静态资源。

如果 UI 修改后没有进入最终插件产物，优先检查 `build.gradle` 和 `ui/build.gradle`。

## 架构说明

### 后端

当前后端结构比较精简：

- `src/main/java/com/webjing/issues/IssuesPlugin.java` 是插件主类，继承 `BasePlugin`，通过 Spring `@Component` 注册，并负责生命周期钩子（`start()` / `stop()`）。
- `src/main/resources/plugin.yaml` 是 Halo 插件清单的权威定义。插件名称、兼容版本、设置项键名、主页/仓库/issues 链接、展示信息等都应在这里修改。
- `src/test/java/com/webjing/issues/IssuesPluginTest.java` 是一个轻量级生命周期冒烟测试，使用 JUnit 5 + Mockito。

新增后端能力时，保持插件入口类只负责生命周期和注册逻辑；具体服务、控制器、组件建议放到 `src/main/java/com/webjing/issues/...` 下。

### 前端

前端是一个 Halo 管理后台控制台插件入口：

- `ui/src/index.ts` 通过 `@halo-dev/ui-shared` 的 `definePlugin(...)` 导出插件定义。
- 其中声明的路由会挂载到 Halo Console，父级为 `parentName: 'Root'`。
- `meta.menu` 用于控制页面在 Halo 管理菜单中的展示方式。
- `ui/src/views/HomeView.vue` 是当前示例页面，可作为后台页面开发的参考模式。

### 前端工具链

- Rsbuild 配置位于 `ui/rsbuild.config.ts`，并基于 `@halo-dev/ui-plugin-bundler-kit`。
- Sass 支持通过 `@rsbuild/plugin-sass` 启用。
- UnoCSS 通过 `UnoCSSRspackPlugin` 接入；全局样式入口在 `ui/src/index.ts` 中通过 `import 'uno.css'` 引入。
- 图标组件通过 `unplugin-icons/rspack` 自动加载。
- ESLint 采用 `ui/eslint.config.ts` 中的 flat config，集成了 Vue、TypeScript、Vitest 和 Oxlint 规则。

## 迁移任务约束（old_project → 当前项目）

当前仓库的首要任务不是从零开发，而是将 `old_project/` 中的旧插件实现迁移到当前 Halo 2.23.0 项目中。

### 迁移范围

需要优先迁移且尽量保持原样的内容：

- 旧后端 Java 逻辑：`old_project/src/main/java/com/webjing/issues/**`
- 旧插件资源与配置：`old_project/src/main/resources/**`
- 旧主题端资源：`old_project/packages/**` 以及 `old_project/src/main/resources/console/**`

迁移原则：

- 以“原封不动搬运 + 最小必要适配”为第一原则。
- 先迁移目录和原有实现，再针对 Halo 2.23.0 的 API 变化做兼容修改。
- 不要先大规模重构旧代码，也不要先按新项目脚手架重写一遍。
- 若旧实现与当前最小脚手架（如 `ui/src/views/HomeView.vue`、当前 `IssuesPlugin.java`）冲突，应以旧项目业务代码为主，脚手架代码可被替换。

### 版本迁移背景

- `old_project` 基于 Halo 2.20/2.21 时代的写法。
- 当前项目依赖平台版本是 `2.23.0`（见根 `build.gradle` 与 `ui/package.json`）。
- 因此迁移时要默认旧代码里存在过时 API，尤其集中在：
    - 插件启动入口中的自定义模型注册与索引声明
    - 查询 API（字段查询、索引查询、分页查询）
    - Console/UI 接入方式

### 迁移时优先参考

遇到 Halo 2.23.0 适配问题时，优先使用项目内 skill：`halo-plugin-dev`。

重点参考域：

- Runtime：索引注册、插件启动、扩展点、Reconciler
- API integration：`ReactiveExtensionClient`、`ListOptions`、`Queries`、`listBy/countBy/listAllNames`
- UI domain：控制台 UI 扩展与前端接入

尤其参考：

- `halo-plugin-dev/references/runtime/indexing-and-query.md`
- `halo-plugin-dev/references/runtime/server-extension-points.md`
- `halo-plugin-dev/references/api/extension-client.md`

### 已知高风险迁移点

#### 1. 插件入口与索引注册

旧项目 `old_project/src/main/java/com/webjing/issues/IssuesPlugin.java` 中大量使用：

- `schemeManager.register(...)`
- `new IndexSpec().setName(...).setIndexFunc(...)`
- `IndexAttributeFactory.simpleAttribute / multiValueAttribute`

在 Halo 2.23.0 中：

- 仍可理解旧逻辑，但新实现应优先迁移到 `IndexSpecs.single(...)` / `IndexSpecs.multi(...)`。
- 若旧代码还能编译，不要一次性改业务逻辑，只替换索引声明写法。
- 先保证索引名与原查询字段完全一致，再做查询层迁移。

#### 2. 查询 API 迁移

旧项目中存在典型旧写法，例如：

- `QueryFactory.*`
- `FieldSelector.of(...)`
- `ListOptions#setFieldSelector(...)`

在 Halo 2.23.0 中应优先迁移为：

- `ListOptions.builder().fieldQuery(...)`
- `Queries.equal / and / or / contains / between ...`
- `ReactiveExtensionClient.listBy(...) / countBy(...) / listAllNames(...)`

迁移顺序必须是：

1. 先确认查询字段已经注册索引。
2. 再把 `QueryFactory` / `FieldSelector` 改成 `Queries` + `ListOptions.builder()`。
3. 最后按场景选用 `listBy`、`countBy`、`listAllNames` 等 API。

#### 3. UI 迁移策略

当前新项目使用 `ui/` 子模块构建 Halo Console 前端；旧项目同时存在：

- `old_project/packages/issue-static/**`
- `old_project/src/main/resources/console/**`
- `old_project/src/main/resources/templates/**`
- `old_project/src/main/resources/static/**`

迁移时不要先假设旧 UI 只能直接复用到 `ui/`。应先区分三类资产：

- Console 插件前端资源
- 主题端模板/静态资源
- 仅构建产物或历史遗留文件

先搬运原文件，再判断哪些应该进入：

- `ui/src/**`（新的 Halo Console 前端）
- `src/main/resources/templates/**`
- `src/main/resources/static/**`
- `src/main/resources/console/**`

UI 迁移补充要求：

- UI 迁移完成后，必须重新生成并对齐当前项目所需的 API 调用代码，不要继续直接沿用旧项目面向 Halo 2.20 的 API 使用方式。
- `ui/package.json` 中缺少但旧项目 `old_project/packages/issue-static/package.json` 已使用的依赖，需要按实际代码用量迁移过来，不能只迁源码不迁依赖。
- 迁移依赖时遵循“仅补齐实际使用的缺失依赖”，不要把旧 `package.json` 整体无差别覆盖到新 `ui/package.json`。
- 若旧 UI 代码依赖的构建方式与当前 `ui/` 工程不同，优先保留当前项目的 Rsbuild/Halo Console 插件构建体系，在此基础上吸收旧业务代码和缺失依赖。
- 前端改动完成后，至少执行一次 `pnpm build`、`pnpm type-check`、`pnpm lint` 进行校验。

### 开发环境版本要求

- Java：21
- Node.js：20

涉及本项目开发、迁移、排障时，默认按以上版本执行；不要继续按旧项目的 Node 18 或其他历史版本假设处理。

### 推荐迁移顺序

1. 盘点 `old_project` 的 Java、resources、packages 边界，确认哪些是运行时必需文件。
2. 先迁移自定义模型、service、endpoint、finder、reconciler 等后端代码。
3. 再迁移 `plugin.yaml`、`settings.yaml`、`role-templates.yaml`、通知模板、模板页面、静态资源。
4. 再迁移 UI/console 相关目录与构建资源。
5. 最后统一处理 Halo 2.23.0 适配问题，重点修正索引注册和查询 API。
6. 每迁移一批后立即执行编译/测试，避免最后一次性排错。

### 实施要求

- 做迁移类任务时，先阅读 `old_project` 对应旧实现，再改当前项目，不要脱离旧代码猜测实现。
- 修改要以“能运行旧功能”为目标，不要顺手重构命名、目录或抽象层。
- 如果需要替换过时 API，只替换到能在 Halo 2.23.0 正常工作为止。
- 当遇到索引/查询报错时，先怀疑“索引未注册或索引名不一致”，不要先改业务层逻辑。
