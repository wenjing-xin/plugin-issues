import { axiosInstance } from "@halo-dev/api-client";

export * from './issueComment';
export * from './issue';

export function searchIssue(keywordStr: string) {
    let param = {
        highlightPostTag: "</mark>",
        highlightPreTag :  "<mark>",
        includeTypes: ["issue.issue.webjing.com"],
        keyword: keywordStr,
        limit: 20
    }
    const urlPath = `/apis/api.halo.run/v1alpha1/indices/-/search`;
    return axiosInstance.post(urlPath, param);
}