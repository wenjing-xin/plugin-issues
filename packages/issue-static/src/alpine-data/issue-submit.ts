import type { Issue, IssueTemplateRender, TemplateField } from "../types";
import { publicApiClient } from "@halo-dev/api-client";
import { createIssue, fetchIssueTemplateDetails } from "../api";
import { isFieldVisible, prepareTemplateValues, requiredField } from "../../../issue-shared/template-fields";
import message from "./message";

const messageUtils = message();

export default (subjectName: string, templateName: string) => ({
    issueForm: {
        kind: "Issue", apiVersion: "issue.webjing.com/v1alpha1",
        metadata: { name: "", generateName: "issue-", annotations: {} },
        spec: {
            title: "", content: { raw: "", html: "", medium: [] }, releaseTime: new Date().toISOString(),
            owner: "", assignees: [], labels: [], closedAt: "", approved: false,
            approvedTime: "", subjectName, top: false,
        },
        status: { observedVersion: 0, permalink: "", state: "AWAIT" },
    } as Issue,
    saveLoading: false,
    submitText: "创建 Issue",
    templateLoading: false,
    templateError: "",
    issueTemplateRender: { displayName: "", components: [], annotationFields: [] } as IssueTemplateRender,
    postOptions: [] as Array<{ label: string; value: string }>,
    postsPage: 0,
    postsHasMore: true,
    postsLoading: false,
    postsError: "",

    init() {
        if (templateName) this.getIssueTemplateOptions(templateName);
    },
    fieldVisible(field: TemplateField) {
        return isFieldVisible(field, this.issueTemplateRender.components || [], this.issueForm.metadata.annotations || {});
    },
    fieldRequired: requiredField,
    async submitForm(rawContent: string, preview: string) {
        if (this.saveLoading || this.templateLoading || this.templateError) return;
        const fields = this.issueTemplateRender.components || [];
        const values = prepareTemplateValues(fields, this.issueForm.metadata.annotations || {});
        const missing = fields.find(field => this.fieldVisible(field) && requiredField(field) && !String(values[field.key] ?? "").trim());
        if (missing) {
            messageUtils.showMessage("warning", `请填写：${missing.title}`, 3000);
            return;
        }
        const annotations = Object.fromEntries(Object.entries(values).map(([key, value]) =>
            [key, Array.isArray(value) ? JSON.stringify(value) : String(value ?? "")])) as Record<string, string>;
        this.issueForm.metadata.annotations = annotations;
        this.issueForm.spec.content.raw = rawContent;
        this.issueForm.spec.content.html = preview;
        this.issueForm.spec.issueTemplate = templateName;
        this.saveLoading = true;
        this.submitText = "Issue 创建中…";
        try {
            await createIssue(this.issueForm);
            messageUtils.showMessage("success", "成功创建 Issue", 2000);
            window.location.href = `/subject/${encodeURIComponent(subjectName)}/issues`;
        } catch (error) {
            const status = (error as { response?: { status?: number } }).response?.status;
            messageUtils.showMessage("error", status === 403
                ? "没有发布 Issue 的权限，请联系管理员分配「允许用户发布issue」权限。填写内容已保留。"
                : "创建失败，请检查登录状态和必填字段后重试。填写内容已保留。", 5000);
        } finally {
            this.saveLoading = false;
            this.submitText = "创建 Issue";
        }
    },
    cancelCreate() {
        window.location.href = `/subject/${encodeURIComponent(subjectName)}/issues`;
    },
    async getIssueTemplateOptions(name: string) {
        this.templateLoading = true;
        this.templateError = "";
        try {
            const { data } = await fetchIssueTemplateDetails(name);
            this.issueTemplateRender = data;
            this.issueForm.metadata.annotations ||= {};
            for (const field of this.issueTemplateRender.components || []) {
                this.issueForm.metadata.annotations[field.key] = field.defaultValue || "";
            }
            if (this.issueTemplateRender.components?.some(field => String(field.type) === "POST")) await this.loadMorePosts();
        } catch {
            this.templateError = "模板加载失败，请确认登录及发布权限后刷新重试。";
        } finally {
            this.templateLoading = false;
        }
    },
    async loadMorePosts() {
        if (this.postsLoading || !this.postsHasMore) return;
        this.postsLoading = true;
        this.postsError = "";
        try {
            const page = this.postsPage + 1;
            const { data } = await publicApiClient.content.post.queryPosts({ page, size: 20 });
            const options = data.items.map(post => ({ value: post.metadata.name, label: post.spec?.title || post.metadata.name }));
            this.postOptions = [...this.postOptions, ...options].filter((item, index, all) => all.findIndex(other => other.value === item.value) === index);
            this.postsPage = page;
            this.postsHasMore = page * 20 < data.total;
        } catch {
            this.postsError = "文章加载失败，请重试。";
        } finally {
            this.postsLoading = false;
        }
    },
});
