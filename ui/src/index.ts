import { definePlugin} from "@halo-dev/console-shared";
import PajamasIssueTypeRequirements from '~icons/pajamas/issue-type-requirements';
import FluentMailTemplate20Regular from "~icons/fluent/mail-template-20-regular";
import { markRaw} from "vue";
import IssueMessageList from "@/views/IssueMessageList.vue";
import IssueTemplateList from "@/views/IssueTemplateList.vue";
import IssueTemplateEditor from "@/views/IssueTemplateEditor.vue";
import "./styles/index.css";

export default definePlugin({
  components: {},
  routes: [
    {
      parentName: "Root",
      route: {
        path: "issue",
        name: "IssueRoot",
        meta: {
          title: "Issue留言",
          searchable: true,
          mobile: true,
          permissions: ["plugin:issues:manage"],
          menu: {
            name: "Issue留言",
            group: "content",
            icon: markRaw(PajamasIssueTypeRequirements),
            mobile: true,
          },
        },
        children: [
          {
            path: "",
            name: "IssueMessage",
            component: IssueMessageList,
          },
          {
            path: "template",
            name: "IssueTemplate",
            component: IssueTemplateList,
            meta: {
              title: "Issue留言模版",
              searchable: true,
              permissions: ["plugin:issueTemplates:manage"],
              menu: {
                name: "Issue留言模版",
                group: "content",
                icon: markRaw(FluentMailTemplate20Regular),
              },
            },
          },
          {
            path: "editor",
            name: "IssueTemplateEditor",
            component: IssueTemplateEditor,
            meta: {
              title: "编辑issue留言模版",
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
