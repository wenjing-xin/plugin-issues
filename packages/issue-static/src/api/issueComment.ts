import { IssueComment } from "../types";
import { axiosInstance } from "@halo-dev/api-client";

const apiVersion = "/apis/uc.api.issueComment.webjing.com/v1alpha1";

export function createIssueComment(issueComment:IssueComment) {
    const urlPath = `${apiVersion}/issuecomments`;
    return axiosInstance.post(urlPath, issueComment)
}

export function updateMyIssueComment(issueComment:IssueComment) {
    const urlPath = `${apiVersion}/issuecomments/${issueComment.metadata.name}`;
    return axiosInstance.put(urlPath, issueComment)
}

export function fetchIssueCommentContent(issueCommentName: string){
    const urlPath = `${apiVersion}/issuecomments/content?issueCommentName=${issueCommentName}`;
    return axiosInstance.get(urlPath)
}