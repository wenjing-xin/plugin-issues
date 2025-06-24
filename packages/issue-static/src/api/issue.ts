import { axiosInstance } from "@halo-dev/api-client";

const apiVersion = "/apis/uc.api.issue.webjing.com/v1alpha1";

export function fetchIssueContent(issueName: string){
    const urlPath = `${apiVersion}/issues/content?issueName=${issueName}`;
    return axiosInstance.get(urlPath)
}