import { definePlugin} from "@halo-dev/ui-shared";
import PajamasIssueTypeObjective from '~icons/pajamas/issue-type-objective';
import FluentMailTemplate20Regular from "~icons/fluent/mail-template-20-regular";
import PepiconsPrintLabelCircle from '~icons/pepicons-print/label-circle';
import { markRaw } from "vue";
import IssueSubjectList from "@/views/IssueSubjectList.vue";
import IssueList from "@/views/IssueList.vue";
import IssueTemplateList from "@/views/IssueTemplateList.vue";
import IssueTemplateEditor from "@/views/IssueTemplateEditor.vue";
import IssueLabelList from "@/views/IssueLabelList.vue";
import "./styles/index.scss";
export default definePlugin({
  components: {},
  routes: [
    {
      parentName: "Root",
      route: {
        path: "/issueSubject",
        name: "IssueRoot",
        meta: {
          title: "Issue",
          searchable: true,
          mobile: true,
          permissions: ["plugin:issues:manage"],
          menu: {
            name: "言答Issue",
            group: "content",
            icon: markRaw(PajamasIssueTypeObjective),
            mobile: true,
          },
        },
        children: [
          {
            path: "",
            name: "IssueSubject",
            component: IssueSubjectList,
          },
          {
            path: "issues",
            name: "Issue",
            component: IssueList,
            meta: {
              title: "Issue列表",
              searchable: true,
              permissions: ["plugin:issues:manage"],
            },
          },
          {
            path: "template",
            name: "IssueTemplate",
            component: IssueTemplateList,
            meta: {
              title: "Issue模版",
              searchable: true,
              permissions: ["plugin:issueTemplates:manage"],
              menu: {
                name: "Issue模版",
                group: "content",
                icon: markRaw(FluentMailTemplate20Regular),
              },
            },
          },
          {
            path: "issueLabel",
            name: "IssueLabel",
            component: IssueLabelList,
            meta: {
              title: "Issue标签管理",
              searchable: true,
              permissions: ["plugin:issue:labels:manage"],
              menu: {
                name: "Issue标签管理",
                group: "content",
                icon: markRaw(PepiconsPrintLabelCircle),
              },
            },
          },
          {
            path: "editor",
            name: "IssueTemplateEditor",
            component: IssueTemplateEditor,
            meta: {
              title: "编辑issue模版",
              searchable: true,
              permissions: ["plugin:issueTemplates:manage"],
            },
          },
        ],
      },
    },
  ],
  extensionPoints: {
  },
});
