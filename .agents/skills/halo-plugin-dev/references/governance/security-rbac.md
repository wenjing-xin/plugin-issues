---
inclusion: manual
---

# Halo 插件权限管理

Halo 使用基于角色的权限控制（RBAC）来管理权限，通过定义用户角色来简化和增强权限管理。

## RBAC 核心概念

| 概念 | 说明 |
|------|------|
| 角色 (Role) | 定义一组操作权限，可绑定到用户，支持角色依赖实现权限继承 |
| 用户 (User) | 实际使用 Halo 资源的实体 |
| 角色绑定 (RoleBinding) | 将用户与特定角色关联，使用户获得角色权限 |

## 角色模板定义

插件的 APIs 默认只有超级管理员能访问，需要定义角色模板来授权给其他用户。

### 文件位置

角色模板文件存放于 `src/main/resources/extensions`，文件名任意。

### 基本框架

```yaml
apiVersion: v1alpha1
kind: Role
metadata:
  name: my-plugin-role-view-persons  # 必须以插件名为前缀
  labels:
    halo.run/role-template: "true"   # 必须标记为角色模板
  annotations:
    rbac.authorization.halo.run/module: "Persons Management"        # 分组名称
    rbac.authorization.halo.run/display-name: "Person View"         # 显示名称
    rbac.authorization.halo.run/dependencies: |                     # 依赖的角色
      ["role-template-view-person"]
    rbac.authorization.halo.run/ui-permissions: |                   # UI 权限
      ["plugin:my-plugin:person:view"]
rules:
  - apiGroups: []
    resources: []
    resourceNames: []
    verbs: []
```

### 关键 Labels

| Label | 说明 |
|-------|------|
| `halo.run/role-template: "true"` | 标识为角色模板（必须） |
| `halo.run/hidden: "true"` | 隐藏角色模板，不在 UI 显示 |

### 关键 Annotations

| Annotation | 说明 |
|------------|------|
| `rbac.authorization.halo.run/module` | 角色模板分组名称 |
| `rbac.authorization.halo.run/display-name` | 显示名称 |
| `rbac.authorization.halo.run/dependencies` | 依赖的角色（JSON 数组） |
| `rbac.authorization.halo.run/ui-permissions` | UI 权限（JSON 数组） |

## Rules 配置

### 资源型规则

适用于符合 `/apis/<group>/<version>/<resource>[/<resourceName>/<subresource>]` 格式的 API。

```yaml
rules:
  - apiGroups: ["my-plugin.halo.run"]
    resources: ["my-plugin/persons"]
    resourceNames: ["zhangsan"]  # 可选，限定特定资源
    verbs: ["get", "list"]
```

### 非资源型规则

适用于不符合资源型格式的 API。

```yaml
rules:
  - nonResourceURLs: ["/healthz", "/healthz/*"]
    verbs: ["get", "create"]
```

### Verbs 详解

| Verb | HTTP Method | 说明 |
|------|-------------|------|
| `create` | POST | 创建新资源 |
| `get` | GET | 获取单个资源（含 resourceName） |
| `list` | GET | 获取资源列表 |
| `watch` | GET | 监控资源变化（WebSocket） |
| `update` | PUT | 更新资源全部内容 |
| `patch` | PATCH | 部分更新资源 |
| `delete` | DELETE | 删除单个资源 |
| `deletecollection` | DELETE | 删除资源集合 |

## 完整示例

```yaml
# 查看权限
apiVersion: v1alpha1
kind: Role
metadata:
  name: my-plugin-role-view-persons
  labels:
    halo.run/role-template: "true"
  annotations:
    rbac.authorization.halo.run/module: "Persons Management"
    rbac.authorization.halo.run/display-name: "Person View"
    rbac.authorization.halo.run/ui-permissions: |
      ["plugin:my-plugin:person:view"]
rules:
  - apiGroups: ["my-plugin.halo.run"]
    resources: ["my-plugin/persons"]
    verbs: ["get", "list"]
---
# 管理权限（依赖查看权限）
apiVersion: v1alpha1
kind: Role
metadata:
  name: my-plugin-role-manage-persons
  labels:
    halo.run/role-template: "true"
  annotations:
    rbac.authorization.halo.run/module: "Persons Management"
    rbac.authorization.halo.run/display-name: "Person Manage"
    rbac.authorization.halo.run/dependencies: |
      ["my-plugin-role-view-persons"]
    rbac.authorization.halo.run/ui-permissions: |
      ["plugin:my-plugin:person:manage"]
rules:
  - apiGroups: ["my-plugin.halo.run"]
    resources: ["my-plugin/persons"]
    verbs: ["*"]
```

## 聚合角色

将角色权限合并到 Halo 内置角色，通过 `rbac.authorization.halo.run/aggregate-to-{role-name}` label 实现。

### 聚合到 anonymous（公开访问）

```yaml
apiVersion: v1alpha1
kind: Role
metadata:
  name: template-moment-anonymous-resources
  labels:
    halo.run/role-template: "true"
    halo.run/hidden: "true"  # 隐藏，不在权限列表显示
    rbac.authorization.halo.run/aggregate-to-anonymous: "true"
rules:
  - apiGroups: ["api.moment.halo.run"]
    resources: ["moments"]
    verbs: ["get", "list"]
```

### 聚合到 authenticated（登录用户）

```yaml
apiVersion: v1alpha1
kind: Role
metadata:
  name: template-authenticated-resources
  labels:
    halo.run/role-template: "true"
    rbac.authorization.halo.run/aggregate-to-authenticated: "true"
rules:
  - apiGroups: ["my-plugin.halo.run"]
    resources: ["my-plugin/persons"]
    verbs: ["get", "list"]
```

## 内置角色

| 角色名 | 说明 |
|--------|------|
| `super-role` | 超级管理员，拥有所有权限 |
| `guest` | 访客，无额外权限 |
| `anonymous` | 匿名访客（未登录用户自动获得） |
| `authenticated` | 已登录用户（登录后自动获得） |

---

## UI 权限控制

UI 权限控制用于在前端界面控制菜单、按钮等元素的显示。

### 权限命名规则

```
plugin:{your-plugin-name}:{scope-name}
```

例如：`plugin:my-plugin:person:view`

### 在角色模板中声明

```yaml
annotations:
  rbac.authorization.halo.run/ui-permissions: |
    ["plugin:my-plugin:person:view"]
```

### 在路由中使用

```typescript
export default definePlugin({
  routes: [
    {
      parentName: "Root",
      route: {
        path: "/example",
        name: "Example",
        component: HomeView,
        meta: {
          title: "示例页面",
          // 菜单权限控制
          permissions: ["plugin:my-plugin:person:view"],
          menu: {
            name: "示例页面",
            group: "示例分组",
            icon: markRaw(IconPlug),
            priority: 0,
          },
        },
      },
    },
  ],
});
```

### 在组件中使用

```vue
<template>
  <!-- HasPermission 组件无需导入，直接使用 -->
  <HasPermission :permissions="['plugin:my-plugin:person:view']">
    <UserFilterDropdown v-model="selectedUser" label="用户" />
  </HasPermission>
</template>
```

## 角色绑定

将角色分配给用户：

```yaml
apiVersion: v1alpha1
kind: RoleBinding
metadata:
  name: guqing-post-reader-binding
roleRef:
  apiGroup: ''
  kind: Role
  name: post-reader
subjects:
  - apiGroup: ''
    kind: User
    name: guqing  # 用户的 username
```
