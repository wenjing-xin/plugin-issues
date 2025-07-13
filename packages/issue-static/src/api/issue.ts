import { axiosInstance } from "@halo-dev/api-client";
import {Issue} from "../types";

const apiVersion = "/apis/uc.api.issue.webjing.com/v1alpha1";

export function fetchIssueContent(issueName: string){
    const urlPath = `${apiVersion}/issues/content?issueName=${issueName}`;
    return axiosInstance.get(urlPath)
}

export function createIssue(issue: Issue) {
    const urlPath = `${apiVersion}/issues`;
    return axiosInstance.post(urlPath, issue);
}