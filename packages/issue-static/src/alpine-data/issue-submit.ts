import type { Issue, IssueTemplateRender } from "../types";
import { createIssue, fetchIssueTemplateDetails } from "../api";
import message from "./message";

const messageUtils = message();

export default (subjectName: string, templateName: string) => ({

    issueForm: {
        kind: "Issue",
        apiVersion: "issue.webjing.com/v1alpha1",
        metadata: {
            name: "",
            generateName: "issue-",
            annotations: {},
        },
        spec: {
            title: "",
            content: {
                raw: "",
                html: "",
                medium: []
            },
            releaseTime: new Date().toISOString(),
            owner: "",
            assignees: [],
            labels: [],
            closedAt: "",
            approved: true,
            approvedTime: "",
            subjectName: subjectName,
            top: false
        },
        status: {
            observedVersion: 0,
            permalink: "",
            state: "AWAIT",
        },
    } as Issue,

    saveLoading: false,

    submitText: '创建 Issue',

    issueTemplateRender: {
        displayName: "",
        components: [],
        annotationFields: []
    } as IssueTemplateRender,

    init() {
        if (templateName) {
            this.getIssueTemplateOptions(templateName);
        }
    },

    submitForm(rawContent: string, preview: string) {
        this.issueForm.spec.content.raw = rawContent;
        this.issueForm.spec.content.html = preview;
        this.saveLoading = true;
        this.submitText = 'Issue 创建中...';
        this.issueForm.spec.issueTemplate = templateName;
        for (let key in this.issueForm.metadata.annotations) {
            if (Object.prototype.toString.call(this.issueForm.metadata.annotations[key]) === '[object Array]') {
                this.issueForm.metadata.annotations[key] = JSON.stringify(this.issueForm.metadata.annotations[key]) + '';
            }
        }
        createIssue(this.issueForm).then((res) => {
            if (res.status == 200) {
                messageUtils.showMessage("success", '成功创建Issue', 2000);
            } else {
                messageUtils.showMessage("error", res.statusText, 2000);
            }
            window.location.href = `${window.location.origin}/subject/${subjectName}/issues`;
        }).catch(error => {
            messageUtils.showMessage("error", error.message, 2000);
        }).finally(() => {
            // 置空issue
            this.issueForm = {
                kind: "Issue",
                apiVersion: "issue.webjing.com/v1alpha1",
                metadata: {
                    name: "",
                    generateName: "issue-"
                },
                spec: {
                    title: "",
                    content: {
                        raw: "",
                        html: "",
                        medium: []
                    },
                    releaseTime: new Date().toISOString(),
                    owner: "",
                    assignees: [],
                    labels: [],
                    closedAt: "",
                    approved: true,
                    approvedTime: "",
                    subjectName: subjectName,
                    top: false
                },
                status: {
                    observedVersion: 0,
                    permalink: "",
                    state: "AWAIT",
                }
            }
            this.saveLoading = false;
            this.submitText = '创建 Issue';
        });
    },

    cancelCreate() {
        window.location.href = `${window.location.origin}/subject/${subjectName}/issues`;
    },

    getIssueTemplateOptions(templateName: string) {
        fetchIssueTemplateDetails(templateName).then(res => {
            if (res.status == 200) {
                this.issueTemplateRender = res.data;
                this.issueTemplateRender.annotationFields?.forEach(filed => {
                    // @ts-ignore
                    this.issueForm.metadata.annotations[filed] = "";
                })
            }
        })
    },

})