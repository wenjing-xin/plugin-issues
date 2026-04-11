<script lang="ts" setup>
import { VModal, VButton, VSpace, Toast } from "@halo-dev/components";
import {
  computed,
  nextTick,
  onMounted,
  ref,
  toRaw,
  watch,
  watchEffect,
} from "vue";
import type { IssueSubject, IssueTemplateItem } from "@/api/generated";
import { subjectTypeOptions } from "@/dictionary";
import cloneDeep from "lodash.clonedeep";
import {
  consoleIssueSubjectApiClient,
  consoleIssueTemplateApiClient,
} from "@/api";
import { accepts } from "@/dictionary/index";
import { utils, type AttachmentLike } from "@halo-dev/ui-shared";
import TextEditor from "@/components/editor/index.vue";
import { submitForm } from "@formkit/core";


const modalTitle = ref("新增 Issue 依托主体");
const saving = ref<boolean>(false);

const props = withDefaults(
  defineProps<{
    visible: boolean;
    issueSubject?: IssueSubject | undefined;
  }>(),
  {
    visible: false,
    issueSubject: undefined,
  },
);
const emit = defineEmits<{
  (event: "update:visible", value: boolean): void;
  (event: "close", value: boolean): void;
  (event: "save", issueSubject: IssueSubject): void;
  (event: "update", issueSubject: IssueSubject): void;
}>();

const issueTemplateFilterOptions = ref<Array<IssueTemplateItem>>([]);
const attachmentSelectorModal = ref(false);

const initIssueSubject: IssueSubject = {
  kind: "IssueSubject",
  apiVersion: "issue.webjing.com/v1alpha1",
  metadata: {
    generateName: "subject-",
    name: "",
  },
  spec: {
    subjectIcon: "",
    displayName: "",
    content: {
      rawContent: "",
      htmlContent: "",
      uid: "",
    },
    subjectType: "PRODUCT",
    issueTemplates: [],
    owner: "",
    description: "",
    participateUsers: [],
    subjectVisible: "PUBLIC",
  },
};
const subjectSelectTypeOptions = computed(() =>
  subjectTypeOptions.value.filter((item) => item.label != "默认"),
);
const formState = ref<IssueSubject>(cloneDeep(initIssueSubject));

watchEffect(() => {
  if (props.issueSubject) {
    formState.value = cloneDeep(props.issueSubject);
    modalTitle.value = "编辑 Issue 依托主体";
  }
});

onMounted(() => {});

const isUpdateMode = computed(
  () => !!formState.value.metadata.creationTimestamp,
);
const isEditorEmpty = ref<boolean>(true);

const onVisibleChange = (visible: boolean) => {
  emit("update:visible", visible);
  if (!visible) {
    emit("close", false);
    handleReset();
  }
};

const annotationsFormRef = ref();
const onSubmit = async () => {
  try {
    saving.value = true;
    annotationsFormRef.value?.handleSubmit();
    await nextTick();

    const {
      customAnnotations,
      annotations,
      customFormInvalid,
      specFormInvalid,
    } = annotationsFormRef.value || {};
    if (customFormInvalid || specFormInvalid) {
      return;
    }
    formState.value.metadata.annotations = {
      ...annotations,
      ...customAnnotations,
    };

    if (isUpdateMode.value) {
      await handleUpdate();
      emit("update", formState.value);
    } else {
      await handleSave(formState.value);
    }
    handleReset();
  } catch (error) {
    console.error(error);
  } finally {
    saving.value = false;
  }
  onVisibleChange(false);
  formState.value = cloneDeep(initIssueSubject);
};
const handleUpdate = async () => {
  const res =
    await consoleIssueSubjectApiClient.issueSubject.updateIssueSubject({
      issueSubject: formState.value,
    });
  if (res.status == 200) {
    Toast.success("更新成功!");
  }
};

// 新增 issue 依托主体对象
const handleSave = async (issueSubject: IssueSubject) => {
  const { data } =
    await consoleIssueSubjectApiClient.issueSubject.createIssueSubject({
      issueSubject: issueSubject,
    });
  emit("save", data);
  Toast.success("操作成功");
};
const handleReset = () => {
  formState.value = toRaw(cloneDeep(initIssueSubject));
  isEditorEmpty.value = true;
};

// 监听 subjectType 变化，debounce 避免频繁请求
watch(
  () => formState.value.spec.subjectType,
  (newVal: string) => {
    consoleIssueTemplateApiClient.issueTemplate
      .listIssueTemplateOptions({
        subjectType: newVal,
        subjectName: props.issueSubject?.metadata.name,
      })
      .then(({ data }) => {
        issueTemplateFilterOptions.value = data.issueTemplateOptions;
      });
  },
);

const onAttachmentsSelect = async (attachments: AttachmentLike[]) => {
  const firstAttachment = attachments[0];
  if (!firstAttachment) {
    return;
  }

  formState.value.spec.subjectIcon =
    utils.attachment.getUrl(firstAttachment) || "";
};
</script>
<template>
  <VModal
    :title="modalTitle"
    :visible="visible"
    :width="760"
    @update:visible="onVisibleChange"
  >
    <template #actions>
      <slot name="append-actions" />
    </template>
    <!-- 提交表单  -->
    <div class=":uno: md:grid md:grid-cols-4 md:gap-2">
      <div class=":uno: mt-2.5 px-3 md:col-span-1">
        <div class="sticky top-0">
          <span class="text-base text-gray-900 font-medium">依托主体详情</span>
        </div>
      </div>
      <div class=":uno: divide-gray-25 mt-5 px-3 md:col-span-3 md:mt-3 divide-y">
        <FormKit
          id="issue-subject"
          type="form"
          name="issue-subject"
          :config="{ validationVisibility: 'submit' }"
          @submit="onSubmit"
        >
          <FormKit
            v-model="formState.spec.displayName"
            type="text"
            label="展示名称"
            validation="required"
          />
          <FormKit
            v-model="formState.spec.subjectIcon"
            type="attachment"
            :accepts="['image/*']"
            validation="length:0,1024"
            label="主体展示图标"
          />
          <FormKit
            v-model="formState.spec.subjectType"
            type="select"
            validation="required"
            label="Issue依托主体类型"
            :options="subjectSelectTypeOptions"
            clearable
          />
          <template v-if="formState.spec.subjectType == 'POST'">
            <FormKit
              v-model="formState.spec.content.uid"
              placeholder="请选择文章"
              label="依托的文章内容"
              type="postSelect"
            />
          </template>
          <FormKit
            v-model="formState.spec.issueTemplates"
            type="select"
            clearable
            label="Issue模版"
            multiple
            :options="issueTemplateFilterOptions"
          />
          <AttachmentSelectorModal
            v-model:visible="attachmentSelectorModal"
            v-permission="['system:attachments:view']"
            :min="1"
            :max="9"
            :accepts="accepts"
            @select="onAttachmentsSelect"
          />
          <FormKit
            v-model="formState.spec.description"
            label="描述"
            type="textarea"
            rows="1"
          />
          <FormKit
            v-model="formState.spec.participateUsers"
            label="参与者"
            type="select"
            multiple
            clearable
            searchable
            action="/apis/api.console.halo.run/v1alpha1/users?fieldSelector=name!=anonymousUser&fieldSelector=name!=ghost"
            :request-option="{
              method: 'get',
              pageField: 'page',
              sizeField: 'size',
              totalField: 'total',
              itemsField: 'items',
              labelField: 'user.spec.displayName',
              valueField: 'user.metadata.name',
              fieldSelectorKey: 'metadata.name',
            }"
            help="创建者和参与者将会在有新issue时收到通知"
          />
          <FormKit
            v-model="formState.spec.subjectVisible"
            type="select"
            clearable
            validation="required"
            label="可见性"
            :options="[
              { label: '公共', value: 'PUBLIC' },
              { label: '私有', value: 'PRIVATE' },
            ]"
          />
        </FormKit>
        <div
          v-if="formState.spec.subjectType !== 'POST'"
          class="space-y-2 my-2 py-2"
        >
          <p class="text-sm font-bold text-gray-600">主体内容</p>
          <TextEditor
            v-model:raw="formState.spec.content.rawContent"
            v-model:html="formState.spec.content.htmlContent"
            v-model:is-empty="isEditorEmpty"
            class="min-h-[15rem] p-3.5 rounded-md"
            tabindex="-1"
          />
        </div>
      </div>
    </div>

    <div class="py-5">
      <div class="border-t border-gray-200"></div>
    </div>
    <div class=":uno: md:grid md:grid-cols-4 md:gap-2">
      <div class=":uno: px-3 md:col-span-1">
        <div class="sticky top-0">
          <span class="text-base text-gray-900 font-medium"> 元数据 </span>
        </div>
      </div>
      <div
        class=":uno: divide-gray-25 mt-5 w-full px-3 md:col-span-3 md:mt-0 divide-y"
      >
        <AnnotationsForm
          v-if="visible"
          :key="formState.metadata.name"
          ref="annotationsFormRef"
          :value="formState.metadata.annotations"
          kind="IssueSubject"
          group="issue.webjing.com"
        />
      </div>
    </div>
    <template #footer>
      <VSpace>
        <VButton
          :loading="saving"
          type="secondary"
          @click="submitForm('issue-subject')"
        >
          提交
        </VButton>
        <VButton @click="onVisibleChange(false)"> 取消 </VButton>
      </VSpace>
    </template>
  </VModal>
</template>
