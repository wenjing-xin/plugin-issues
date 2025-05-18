import { axiosInstance } from "@halo-dev/api-client";
import {
  IssueSubjectV1alpha1Api,
  ConsoleApiIssueSubjectWebjingComV1alpha1IssueSubjectApi,
  IssueV1alpha1Api,
  ConsoleApiIssueWebjingComV1alpha1IssueApi,
  UcApiIssueWebjingComV1alpha1IssueApi,
  IssueCommentV1alpha1Api,
  IssueTemplateV1alpha1Api,
  ConsoleApiIssueTemplateWebjingComV1alpha1IssueTemplateApi
} from "./generated";

const issueSubjectApiClient = {
  issueSubject: new IssueSubjectV1alpha1Api(undefined, "", axiosInstance),
};

const consoleIssueSubjectApiClient = {
  issueSubject: new ConsoleApiIssueSubjectWebjingComV1alpha1IssueSubjectApi(undefined, "", axiosInstance),
};

const issueApiClient = {
  issue: new IssueV1alpha1Api(undefined, "", axiosInstance),
};


const issueCommentApiClient = {
  issueComment: new IssueCommentV1alpha1Api(undefined, "", axiosInstance),
};

const issueTemplateApiClient = {
  issueTemplate: new IssueTemplateV1alpha1Api(undefined, "", axiosInstance),
};

const consoleIssueTemplateApiClient = {
  issueTemplate: new ConsoleApiIssueTemplateWebjingComV1alpha1IssueTemplateApi(undefined, "", axiosInstance),
};

const consoleIssueApiClient = {
  issue: new ConsoleApiIssueWebjingComV1alpha1IssueApi(undefined, "", axiosInstance),
};

const ucIssueApiClient = {
  issue: new UcApiIssueWebjingComV1alpha1IssueApi(undefined, "", axiosInstance),
};

export {
  issueSubjectApiClient,
  consoleIssueSubjectApiClient,
  issueApiClient,
  consoleIssueApiClient,
  ucIssueApiClient,
  issueCommentApiClient,
  issueTemplateApiClient,
  consoleIssueTemplateApiClient
};
