import { axiosInstance } from "@halo-dev/api-client";
import { Issue } from "../types";

const apiVersion = "/apis/uc.api.issue.webjing.com/v1alpha1";

export function fetchIssueContent(issueName: string){
    const urlPath = `${apiVersion}/issues/content?issueName=${issueName}`;
    return axiosInstance.get(urlPath)
}

export function createIssue(issue: Issue) {
    const urlPath = `${apiVersion}/issues`;
    return axiosInstance.post(urlPath, issue);
}

export function fetchIssueTemplateDetails(templateName: string) {
    const urlPath = `${apiVersion}/issuetemplates/${templateName}`;
    return axiosInstance.get(urlPath);
}

export function fetchIssueDetail(issueName: string){
    const urlPath = `${apiVersion}/issues/${issueName}`;
    return axiosInstance.get(urlPath)
}

export function updateMyIssue(issue: Issue){
    const urlPath = `${apiVersion}/issues/${issue.metadata.name}`;
    return axiosInstance.put(urlPath, issue)
}

export function closedMyIssue(curIssueName: string, closedComment: string){
    const urlPath = `${apiVersion}/issuestatus`;
    return axiosInstance.put(urlPath, {issueName: curIssueName, changeComment: closedComment, issueState: 'CLOSED'})
}

export function reopenMyIssue(curIssueName: string){
    const urlPath = `${apiVersion}/issuestatus`;
    return axiosInstance.put(urlPath, {issueName: curIssueName, changeComment: "重新打开Issue", issueState: 'PROGRESS'})
}