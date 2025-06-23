import { IssueContent } from "../types";
import { axiosInstance } from "@halo-dev/api-client";

const apiVersion = "/apis/uc.api.issue.webjing.com/v1alpha1";

export function fetchIssueContent(issueCommentName: string){
    const urlPath = `${apiVersion}/issuecomments/content`;
    return axiosInstance.put(urlPath, issueCommentName)
}