import type { Issue } from "../types";
import { createIssue } from "../api";
import message from "./message";

const messageUtils = message();

export default (subjectName:string)=> ({

    issueForm: {
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
        },
    } as Issue,

    saveLoading: false,

    submitText: '创建 Issue',

    submitForm(rawContent:string, preview:string){
        this.issueForm.spec.content.raw = rawContent;
        this.issueForm.spec.content.html = preview;
        console.log(this.issueForm,8989);
        this.saveLoading = true;
        this.submitText = 'Issue 创建中...';
        createIssue(this.issueForm).then((res)=>{
            if(res.status == 200){
                messageUtils.showMessage("success", '成功创建Issue', 2000);
                window.location.href = `${window.location.origin}/subject/${subjectName}/issues`;
            }else{
                messageUtils.showMessage("error", res.statusText, 2000);
            }
        }).catch( error => {
            messageUtils.showMessage("error", error.message, 2000);
        }).finally(()=> {
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
    cancelCreate(){
        window.location.href = `${window.location.origin}/subject/${subjectName}/issues`;
    },


})