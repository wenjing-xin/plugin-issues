<script lang="ts" setup>
import { VModal, VButton, VSpace, Toast } from "@halo-dev/components";
import { computed, nextTick, onMounted, ref, toRaw, watchEffect } from "vue";
import type { IssueLabel, IssueSubject } from "@/api/generated";
import cloneDeep from "lodash.clonedeep";
import {
  issueSubjectApiClient,
  issueLabelApiClient,
  consoleIssueLabelApiClient,
} from "@/api";
import { submitForm } from "@formkit/core";
import { labelScopeTypeOptions, subjectTypeOptions } from "@/dictionary";
const modalTitle = ref("新增 issue 标签");
const saving = ref<boolean>(false);
const props = withDefaults(
  defineProps<{
    visible: boolean;
    issueLabel?: IssueLabel | undefined;
  }>(),
  {
    visible: false,
    issueLabel: undefined,
  },
);

const labelScopeSelectionOptions = computed(() =>
  labelScopeTypeOptions.value.filter((item) => item.label != "默认"),
);
const subjectSelectTypeOptions = computed(() =>
  subjectTypeOptions.value.filter((item) => item.label != "默认"),
);
const emit = defineEmits<{
  (event: "update:visible", value: boolean): void;
  (event: "close", value: boolean): void;
  (event: "save", issueLabel: IssueLabel): void;
  (event: "update", issueLabel: IssueLabel): void;
}>();

const subjectOptions = ref<Array<{ label: string | undefined; value: string }>>(
  [],
);

const initIssueLabel: IssueLabel = {
  kind: "IssueLabel",
  apiVersion: "issue.webjing.com/v1alpha1",
  metadata: {
    generateName: "label-",
    name: "",
  },
  spec: {
    labelName: "",
    description: "",
    color: "#71C8A3",
    slug: "",
    scope: "GLOBAL",
    subjectType: "PROJECT",
    subjectName: "",
  },
};

const formState = ref<IssueLabel>(cloneDeep(initIssueLabel));

watchEffect(() => {
  if (props.issueLabel) {
    formState.value = cloneDeep(props.issueLabel);
    modalTitle.value = "编辑 issue 标签";
  }
});

onMounted(() => {
  handlerIssueSubjectOptions();
});

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
  formState.value = cloneDeep(initIssueLabel);
};
const handleUpdate = async () => {
  const res = await issueLabelApiClient.issueLabel.updateIssueLabel({
    name: formState.value.metadata.name,
    issueLabel: formState.value,
  });
  if (res.status == 200) {
    Toast.success("更新成功!");
  }
};

const handlerIssueSubjectOptions = () => {
  issueSubjectApiClient.issueSubject.listIssueSubject().then(({ data }) => {
    data.items.forEach((it: IssueSubject) => {
      const itemOption = {
        label: it.spec.displayName,
        value: it.metadata.name,
      };
      subjectOptions.value.push(itemOption);
    });
  });
};

// 新增 issue
const handleSave = async (issueLabel: IssueLabel) => {
  const { data } = await consoleIssueLabelApiClient.issueLabel.createIssueLabel(
    {
      issueLabel: issueLabel,
    },
  );
  emit("save", data);
  Toast.success("成功新增标签");
};
const handleReset = () => {
  formState.value = toRaw(cloneDeep(initIssueLabel));
  isEditorEmpty.value = true;
};
</script>
<template>
  <VModal
    :title="modalTitle"
    :visible="visible"
    :width="720"
    @update:visible="onVisibleChange"
  >
    <template #actions>
      <slot name="append-actions" />
    </template>
    <div class=":uno: md:grid md:grid-cols-4 md:gap-6">
      <div class=":uno: mt-2.5 px-3 md:col-span-1">
        <div class="sticky top-0">
          <span class="text-base text-gray-900 font-medium"> Issue详情 </span>
        </div>
      </div>
      <!-- 提交表单  -->
      <div class=":uno: divide-gray-25 mt-5 px-3 md:col-span-3 md:mt-3 divide-y">
        <FormKit
          id="issue-label"
          v-model="formState.spec"
          type="form"
          name="issue-label"
          :config="{ validationVisibility: 'submit' }"
          @submit="onSubmit"
        >
          <FormKit
            type="text"
            label="标签名称"
            name="labelName"
            validation="required"
            modifiers="trim"
          />
          <FormKit name="color" label="标签颜色" type="color"></FormKit>
          <FormKit
            type="textarea"
            rows="3"
            name="description"
            label="标签描述"
          />
          <FormKit
            type="radio"
            name="scope"
            clearable
            validation="required"
            label="标签作用范围"
            modifiers="trim"
            help="同名标签生效范围为全局<特定主体类型<特定主体"
            :options="labelScopeSelectionOptions"
          />
          <FormKit
            v-if="formState.spec.scope == 'SUBJECT_TYPE'"
            type="select"
            name="subjectType"
            clearable
            label="标签归属的主体类型"
            :options="subjectSelectTypeOptions"
          />
          <FormKit
            v-if="formState.spec.scope == 'SUBJECT'"
            type="select"
            name="subjectName"
            clearable
            label="标签归属的主体"
            :options="subjectOptions"
          />
        </FormKit>
      </div>
    </div>
    <div class="py-5">
      <div class="border-t border-gray-200"></div>
    </div>
    <div class=":uno: md:grid md:grid-cols-4 md:gap-6">
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
          kind="IssueLabel"
          group="issue.webjing.com"
        />
      </div>
    </div>
    <template #footer>
      <VSpace>
        <VButton
          :loading="saving"
          type="secondary"
          @click="submitForm('issue-label')"
        >
          提交
        </VButton>
        <VButton @click="onVisibleChange(false)"> 取消 </VButton>
      </VSpace>
    </template>
  </VModal>
</template>
