# halo-plugin-dev（单一总 skill）

该目录已整合为 **1 个总 skill**：`halo-plugin-dev`。

## Skill 用途
`halo-plugin-dev` 统一覆盖四类插件开发任务：
1. UI 扩展（editor/finder/template/web-components）
2. API 对接（extension-client/api-client/custom-api）
3. Runtime 模型（extension-model/reconciler/content-handler/server-extension-points）
4. 治理运维（RBAC/settings/notification/devtools）

## 触发示例
- “帮我做插件编辑器扩展并接入模板渲染”
- “帮我把 custom API 接到 extension client”
- “给 extension model 增加 reconcile 流程”
- “补齐 RBAC 和通知策略，并给出排障步骤”

## References 组织
- `references/ui/*`
- `references/api/*`
- `references/runtime/*`（含索引注册与索引查询专题）
- `references/governance/*`

## 同步策略
- 知识源：`powers/halo-plugin-dev/steering/*.md`
- 变更时先同步 `references/*`，再按需调整 `SKILL.md` 的路由边界。

## 触发词回归清单（建议每次调整后执行）

### A. 正例（应触发 `halo-plugin-dev`）
1. 帮我做 Halo 插件的 UI 扩展，新增一个 finder 面板入口。
2. 我想给插件编辑器加一个按钮并接入 template 渲染。
3. 请把 custom-api 接到 extension-client，并补齐请求封装。
4. 帮我梳理 api-client 的调用层，统一错误处理。
5. 给 extension-model 增加 reconcile 流程，保证幂等。
6. 需要实现 content-handler，并挂到 server extension points。
7. 帮我设计插件 RBAC 权限控制与角色边界。
8. 这个插件的 settings 要分组配置并支持默认值策略。
9. 帮我把 notification 事件接入并补一套排障步骤。
10. Halo 插件开发里 UI、API、运行时都要改，给我分阶段方案。
11. 帮我给自定义模型注册索引（IndexSpecs.single/multi），并说明字段查询怎么写。
12. 这个插件的 listBy 查询很慢，帮我改成基于索引和 ListOptions 的查询方案。

### B. 反例（不应触发 `halo-plugin-dev`）
1. 帮我写一个通用 React 登录页（与 Halo 插件无关）。
2. 解释一下 Java 的线程池原理。
3. 帮我优化 PostgreSQL 慢查询索引。
4. 生成一份公司周报模板（文档写作任务）。
5. 合并这两个 PDF 并加水印。
6. 读取并清洗一个 xlsx 文件的数据。
7. 帮我做一个通用 Kubernetes 部署脚本。
8. 修复 Android 页面布局错位。
9. 解释 OAuth2 授权码流程（泛知识问答，无 Halo 上下文）。
10. 帮我画一个产品宣传海报。

### C. 邻域例（应先判定是否 Halo 插件上下文）
1. “帮我做 UI 扩展” → 如果明确是 Halo 插件场景，则触发；否则不触发。
2. “帮我对接 API client” → 如果指向 Halo 插件客户端层则触发。
3. “做权限控制” → 若是插件 RBAC/Settings 范围则触发，否则不触发。
4. “加一个 reconciler” → 若是插件运行时模型语境则触发。
5. “帮我加索引并优化查询” → 若是 Halo Extension 的索引注册/listBy 语境则触发；若是数据库通用索引优化则不触发。

### E. 人工测试打勾模板（每次回归复制一份）

```md
# halo-plugin-dev 触发回归记录

- 日期：YYYY-MM-DD
- 执行人：
- 版本/变更说明：

## 正例（应触发）
- [ ] 用例1：……（✅/❌）备注：
- [ ] 用例2：……（✅/❌）备注：
- [ ] 用例3：……（✅/❌）备注：
- [ ] 用例4：……（✅/❌）备注：
- [ ] 用例5：……（✅/❌）备注：

## 反例（不应触发）
- [ ] 用例1：……（✅/❌）备注：
- [ ] 用例2：……（✅/❌）备注：
- [ ] 用例3：……（✅/❌）备注：
- [ ] 用例4：……（✅/❌）备注：
- [ ] 用例5：……（✅/❌）备注：

## 邻域例（需上下文判断）
- [ ] 用例1：……（✅/❌）备注：
- [ ] 用例2：……（✅/❌）备注：
- [ ] 用例3：……（✅/❌）备注：

## 结论
- 触发准确率：
- 主要误触发场景：
- 调整建议（description / domain keywords / workflow）：
```

### F. 快速执行建议
1. 每次改 `SKILL.md` 后至少跑 5 条正例 + 5 条反例。
2. 若误触发集中在某一域，优先收紧该域关键词与非触发边界。
3. 连续两次回归通过后，再更新长期记录。

