import { publicApiClient } from "@halo-dev/api-client";

export default (name: string) => ({
    title: "文章不可用",
    url: "",
    async init() {
        if (!name) return;
        try {
            const { data } = await publicApiClient.content.post.queryPostByName({ name });
            this.title = data.spec?.title || name;
            const url = data.status?.permalink || "";
            this.url = /^(\/|https?:\/\/)/.test(url) ? url : "";
        } catch {
            // Public API visibility applies here as well: never expose private post metadata.
        }
    },
});
