import { axiosInstance } from "@halo-dev/api-client";
import { UcApiIssueWebjingComV1alpha1IssueApi } from "../../../../ui/src/api/generated";

const issueApi = new UcApiIssueWebjingComV1alpha1IssueApi(undefined, "", axiosInstance);

export default (issueName: string, initialLabels = "") => ({
    options: [] as Array<{ label: string; value: string }>,
    selected: initialLabels.split(",").filter(Boolean),
    loading: true,
    saving: false,
    error: "",
    async init() {
        try {
            const { data } = await issueApi.getIssueLabelOptions({ name: issueName });
            this.options = (data.issueLabelOptions || [])
                .filter(option => option.value && option.label)
                .map(option => ({ label: option.label!, value: option.value! }));
            this.selected = this.selected.filter(value => this.options.some(option => option.value === value));
        } catch {
            this.error = "无法加载标签，请确认具有 Issue 管理权限后重试。";
        } finally {
            this.loading = false;
        }
    },
    async save() {
        this.saving = true;
        this.error = "";
        try {
            await issueApi.updateIssueLabels({ name: issueName, issueLabelsUpdateRequest: { labels: this.selected } });
            window.location.reload();
        } catch {
            this.error = "标签保存失败，请刷新后重试。";
        } finally {
            this.saving = false;
        }
    },
});
