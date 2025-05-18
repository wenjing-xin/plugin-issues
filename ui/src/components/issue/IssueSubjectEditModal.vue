<script lang="ts" setup>
import { VModal, VButton, VSpace, Toast } from "@halo-dev/components";
import { computed, nextTick, onMounted, ref, toRaw, watchEffect } from "vue";
import type { IssueSubject} from "@/api/generated";
import { subjectTypeOptions } from "@/dictionary";
import cloneDeep from "lodash.clonedeep";
import { issueSubjectApiClient, consoleIssueSubjectApiClient } from "@/api";
import TextEditor from "@/components/editor/index.vue";
import { submitForm } from "@formkit/core";
const modalTitle = ref("新增 Issue 依托主体");
import {accepts} from "@/dictionary/index";
import type {AttachmentLike} from "@halo-dev/console-shared";
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

const attachmentSelectorModal = ref(false);

const initIssueSubject: IssueSubject = {
  kind: "IssueSubject",
  apiVersion: "issue.webjing.com/v1alpha1",
  metadata: {
    generateName: "issueSubject-",
    name: "",
  },
  spec: {
    displayName: "",
    content: {
      rawContent: "",
      htmlContent: ""
    },
    subjectType: "TOPIC",
    issueTemplates: [],
    owner: "",
    description: "",
  }
};

const formState = ref<IssueSubject>(cloneDeep(initIssueSubject));

watchEffect(() => {
  if (props.issueSubject) {
    formState.value = cloneDeep(props.issueSubject);
    modalTitle.value = "编辑 Issue 依托主体";
  }
});

onMounted(() => {
 
});


const isUpdateMode = computed(() => !!formState.value.metadata.creationTimestamp);
const isEditorEmpty = ref<boolean>(true);

const onVisibleChange = (visible: boolean) => {
  emit("update:visible", visible);
  if (!visible) {
    emit("close", false);
  }
};

const annotationsFormRef = ref();
const onSubmit = async () => {
  try {
    saving.value = true;
    annotationsFormRef.value?.handleSubmit();
    await nextTick();

    const { customAnnotations, annotations, customFormInvalid, specFormInvalid } = annotationsFormRef.value || {};
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
  let res = await issueSubjectApiClient.issueSubject.updateIssueSubject({
    name: formState.value.metadata.name,
    issueSubject: formState.value,
  });
  if (res.status == 200) {
    Toast.success("更新成功!");
  }
};

// 新增 issue 依托主体对象
const handleSave = async (issueSubject: IssueSubject) => {
  const { data } = await consoleIssueSubjectApiClient.issueSubject.createIssueSubject({
    issueSubject: issueSubject,
  });
  emit("save", data);
  Toast.success("操作成功");
};
const handleReset = () => {
  formState.value = toRaw(cloneDeep(initIssueSubject));
  isEditorEmpty.value = true;
};
const onAttachmentsSelect = async (attachments: AttachmentLike[]) => {
  
}
</script>
<template>
  <VModal :title="modalTitle" :visible="visible" :width="760" @update:visible="onVisibleChange">
    <template #actions>
      <slot name="append-actions" />
    </template>
    <!-- 提交表单  -->
    <div class="md:grid md:grid-cols-4 md:gap-2">
      <div class="mt-2.5 px-3 md:col-span-1">
        <div class="sticky top-0">
          <span class="text-base text-gray-900 font-medium">依托主体详情</span>
        </div>
      </div>
      <div class="divide-gray-25 mt-5 px-3 md:col-span-3 md:mt-3 divide-y">
        <FormKit
          id="issue-subject"
          type="form"
          name="issue-subject"
          v-model="formState.spec"
          :config="{ validationVisibility: 'submit' }"
          @submit="onSubmit"
        >
          <FormKit type="text" label="展示名称" name="displayName" validation="required" />
          <FormKit
            type="select"
            name="subjectType"
            validation="required"
            label="Issue依托主体类型"
            :options="subjectTypeOptions"
            multiple
            clearable
            searchable
            allow-create
          />
          <AttachmentSelectorModal
            v-model:visible="attachmentSelectorModal"
            v-permission="['system:attachments:view']"
            :min="1"
            :max="9"
            :accepts="accepts"
            @select="onAttachmentsSelect"
          />
          <FormKit label="描述" name="description" type="textarea" rows="1" />
        </FormKit>
        <div class="space-y-2 my-2 py-2">
          <p class="text-sm font-bold text-gray-600">主体内容</p>
          <TextEditor
            v-model:raw="formState.spec.content.rawContent"
            v-model:html="formState.spec.content.htmlContent"
            v-model:isEmpty="isEditorEmpty"
            class="min-h-[15rem] p-3.5 rounded-md"
            tabindex="-1"
          />
        </div>
      </div>
    </div>
  
    <div class="py-5">
      <div class="border-t border-gray-200"></div>
    </div>
    <div class="md:grid md:grid-cols-4 md:gap-2">
      <div class="px-3 md:col-span-1">
        <div class="sticky top-0">
          <span class="text-base text-gray-900 font-medium"> 元数据 </span>
        </div>
      </div>
      <div class="divide-gray-25 mt-5 w-full px-3 md:col-span-3 md:mt-0 divide-y">
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
        <VButton :loading="saving" type="secondary" @click="submitForm('issue-subject')"> 提交 </VButton>
        <VButton @click="onVisibleChange(false)"> 取消 </VButton>
      </VSpace>
    </template>
  </VModal>
</template>
