---
name: halo-plugin-dev
description: Unified skill for Halo plugin development across four domains: (1) frontend UI extensions, (2) API/client integration, (3) runtime model and backend extension points, and (4) governance/operations including RBAC, settings, notification, and diagnostics. Trigger when users ask for Halo plugin design, implementation, integration, or troubleshooting. Do NOT use for non-Halo generic framework tasks unrelated to plugin development context.
---

# halo-plugin-dev

## Domain Routing
Use this skill as a single entrypoint and route tasks into one of these domains:

1. **UI domain**
   - Keywords: UI 扩展, 组件扩展, 编辑器扩展, finder, template, ui extension, web components
   - Read from: `references/ui/*`

2. **API integration domain**
   - Keywords: extension-client, api-client, custom-api, 请求封装, 联调, listBy, ListOptions, FieldSelector, Queries
   - Read from: `references/api/*`

3. **Runtime domain**
   - Keywords: extension model, reconciler, content handler, server extension points, 生命周期, 索引注册, IndexSpecs, IndexSpecRegistry
   - Read from: `references/runtime/*`

4. **Governance/Ops domain**
   - Keywords: RBAC, 权限控制, settings, notification, devtools, 排障
   - Read from: `references/governance/*`

## Inputs
- Plugin requirement and target outcome
- Current code location/module boundaries
- Constraints (security, compatibility, rollout scope)

## Outputs
- Domain-classified implementation plan
- Minimal file-level change suggestions
- Validation checklist (functional + boundary checks)

## Constraints
- Reuse existing patterns before introducing new abstractions
- Keep changes minimal and focused on requested scope
- Only read required references for the active domain (progressive disclosure)
- Explicitly declare non-goals to prevent scope drift

## Workflow
1. Classify the request into one primary domain (UI/API/Runtime/Governance).
2. Read only the references needed for that domain.
3. If request involves custom model indexing, follow this sequence: register indexes first (`IndexSpecs.single/multi`, or compatible `IndexSpec`), then construct query (`ListOptions` + `Queries`/`FieldSelector`), finally select retrieval API (`listBy`/`listAllNames`/`countBy`) by use case.
4. Reuse existing patterns and identify minimal impacted files.
5. Produce a concise implementation/test plan.
6. If cross-domain, split into ordered sub-tasks and execute domain by domain.

## References Index
- UI: `references/ui/ui-extension.md`, `references/ui/ui-components.md`, `references/ui/web-components.md`, `references/ui/editor.md`, `references/ui/finder.md`, `references/ui/template.md`
- API: `references/api/extension-client.md`, `references/api/api-client.md`, `references/api/custom-api.md`
- Runtime: `references/runtime/extension-model.md`, `references/runtime/reconciler.md`, `references/runtime/content-handler.md`, `references/runtime/server-extension-points.md`, `references/runtime/indexing-and-query.md`
- Governance: `references/governance/security-rbac.md`, `references/governance/settings.md`, `references/governance/notification.md`, `references/governance/devtools.md`

## Sync Rule
Single source of truth is `powers/halo-plugin-dev/steering/*.md`. Update `references/*` first when source changes; adjust routing/workflow only if boundaries changed.
