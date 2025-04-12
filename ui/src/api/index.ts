import { axiosInstance } from "@halo-dev/api-client";
import {
  IssueMessageV1alpha1Api,
  ConsoleApiIssueMessageWebjingComV1alpha1IssueMessageApi,
  UcApiIssueMessageWebjingComV1alpha1IssueMessageApi,
  IssueDetailV1alpha1Api,
  IssueTemplateV1alpha1Api,
  ConsoleApiIssueTemplateWebjingComV1alpha1IssueTemplateApi
} from "./generated";

const issueMessageApiClient = {
  issueMessage: new IssueMessageV1alpha1Api(undefined, "", axiosInstance),
};


const issueDetailApiClient = {
  issueDetail: new IssueDetailV1alpha1Api(undefined, "", axiosInstance),
};

const issueTemplateApiClient = {
  issueTemplate: new IssueTemplateV1alpha1Api(undefined, "", axiosInstance),
};

const consoleIssueTemplateApiClient = {
  issueTemplate: new ConsoleApiIssueTemplateWebjingComV1alpha1IssueTemplateApi(undefined, "", axiosInstance),
};

const consoleIssueMessageApiClient = {
  issueMessage: new ConsoleApiIssueMessageWebjingComV1alpha1IssueMessageApi(undefined, "", axiosInstance),
};

const ucIssueMessageApiClient = {
  issueMessage: new UcApiIssueMessageWebjingComV1alpha1IssueMessageApi(undefined, "", axiosInstance),
};

export {
  issueMessageApiClient,
  consoleIssueMessageApiClient,
  ucIssueMessageApiClient,
  issueDetailApiClient,
  issueTemplateApiClient,
  consoleIssueTemplateApiClient
};
