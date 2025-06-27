import { axiosInstance } from "@halo-dev/api-client";
import {
  IssueSubjectV1alpha1Api,
  ConsoleApiIssueSubjectWebjingComV1alpha1IssueSubjectApi,
  IssueV1alpha1Api,
  ConsoleApiIssueWebjingComV1alpha1IssueApi,
  UcApiIssueWebjingComV1alpha1IssueApi,
  IssueCommentV1alpha1Api,
  ConsoleApiIssueCommentWebjingComV1alpha1IssueApi,
  UcApiIssueCommentWebjingComV1alpha1IssueCommentApi,
  IssueTemplateV1alpha1Api,
  ConsoleApiIssueTemplateWebjingComV1alpha1IssueTemplateApi,
  IssueLabelV1alpha1Api,
  ConsoleApiIssueLabelWebjingComV1alpha1IssueLabelApi
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

const consoleIssueCommentApiClient = {
  issueComment: new ConsoleApiIssueCommentWebjingComV1alpha1IssueApi(undefined, "", axiosInstance),
};

const ucIssueCommentApiClient = {
  issueComment: new UcApiIssueCommentWebjingComV1alpha1IssueCommentApi(undefined, "", axiosInstance),
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

const issueLabelApiClient = {
  issueLabel: new IssueLabelV1alpha1Api(undefined, "", axiosInstance),
};

const consoleIssueLabelApiClient = {
  issueLabel: new ConsoleApiIssueLabelWebjingComV1alpha1IssueLabelApi(undefined, "", axiosInstance),
};

export {
  issueSubjectApiClient,
  consoleIssueSubjectApiClient,
  issueApiClient,
  consoleIssueApiClient,
  ucIssueApiClient,
  issueCommentApiClient,
  consoleIssueCommentApiClient,
  ucIssueCommentApiClient,
  issueTemplateApiClient,
  consoleIssueTemplateApiClient,
  issueLabelApiClient,
  consoleIssueLabelApiClient
};
