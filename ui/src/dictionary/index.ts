import { ref } from "vue";
import type {
  IssueLabelSpecScopeEnum,
  IssueSubjectSpecSubjectTypeEnum,
} from "@/api/generated";

const subjectTypeOptions = ref<
  Array<{
    label: string | undefined;
    value: IssueSubjectSpecSubjectTypeEnum | undefined;
  }>
>([
  {
    label: "默认",
    value: undefined,
  },
  {
    label: "文章",
    value: "POST",
  },
  {
    label: "项目",
    value: "PROJECT",
  },
  {
    label: "产品",
    value: "PRODUCT",
  },
  {
    label: "话题",
    value: "TOPIC",
  },
  {
    label: "留言",
    value: "LEAVE_MESSAGE",
  },
]);
const supportImageTypes: string[] = [
  "image/apng",
  "image/avif",
  "image/bmp",
  "image/gif",
  "image/x-icon",
  "image/jpg",
  "image/jpeg",
  "image/png",
  "image/svg+xml",
  "image/tiff",
  "image/webp",
];

const supportVideoTypes: string[] = ["video/*"];

const supportAudioTypes: string[] = ["audio/*"];

const accepts = [
  ...supportImageTypes,
  ...supportVideoTypes,
  ...supportAudioTypes,
];

const labelScopeTypeOptions = ref<
  Array<{
    label: string | undefined;
    value: IssueLabelSpecScopeEnum | undefined;
  }>
>([
  {
    label: "默认",
    value: undefined,
  },
  {
    label: "全局",
    value: "GLOBAL",
  },
  {
    label: "特定主体类型",
    value: "SUBJECT_TYPE",
  },
  {
    label: "特定主体",
    value: "SUBJECT",
  },
]);
const templateScopeTypeOptions = ref<
  Array<{
    label: string | undefined;
    value: IssueLabelSpecScopeEnum | undefined;
  }>
>([
  {
    label: "默认",
    value: undefined,
  },
  {
    label: "特定主体类型",
    value: "SUBJECT_TYPE",
  },
  {
    label: "特定主体",
    value: "SUBJECT",
  },
]);
export { subjectTypeOptions, accepts, labelScopeTypeOptions,templateScopeTypeOptions };
