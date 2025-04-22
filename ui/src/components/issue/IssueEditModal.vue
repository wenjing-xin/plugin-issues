<script lang="ts" setup>
import { VModal, VButton, VSpace, Toast } from "@halo-dev/components";
import { computed, nextTick, onMounted, ref, toRaw, watchEffect } from "vue";
import type { IssueMessage } from "@/api/generated";
import cloneDeep from "lodash.clonedeep";
import { consoleIssueMessageApiClient, issueMessageApiClient, issueTemplateApiClient } from "@/api";
import { submitForm } from "@formkit/core";
const modalTitle = ref("新增issue留言");
const saving = ref<boolean>(false);
const props = withDefaults(
  defineProps<{
    visible: boolean;
    issueMessage?: IssueMessage | undefined;
  }>(),
  {
    visible: false,
    issueMessage: undefined,
  },
);
const emit = defineEmits<{
  (event: "update:visible", value: boolean): void;
  (event: "close", value: boolean): void;
  (event: "save", issueMessage: IssueMessage): void;
  (event: "update", issueMessage: IssueMessage): void;
}>();

const issueTemplateFilterOptions = ref<Array<{ label: string | undefined; value: string }>>([]);

const initIssueMessage: IssueMessage = {
  kind: "IssueMessage",
  apiVersion: "microimmersion.webjing.cn/v1alpha1",
  metadata: {
    generateName: "issue-message-",
    name: "",
  },
  spec: {
    title: "",
    content: {
      raw: "",
      html: "",
      medium: [],
    },
    releaseTime: new Date().toISOString(),
    owner: "",
    assignees: [],
    labels: [],
    closedAt: "",
    approved: true,
    approvedTime: "",
  },
  status: {
    closeReason: "",
    observedVersion: 0,
    permalink: "",
    state: "AWAIT",
  },
};

const formState = ref<IssueMessage>(cloneDeep(initIssueMessage));

watchEffect(() => {
  if (props.issueMessage) {
    formState.value = cloneDeep(props.issueMessage);
    modalTitle.value = "编辑issue留言";
  }
});

onMounted(() => {
  handlerIssueTemplateOptions();
  handlerLabelOptions();
});

const labelOptions = ref<Array<{ label: string; value: string }>>([]);
const handlerLabelOptions = () => {
  consoleIssueMessageApiClient.issueMessage
    .listLabels({
      name: "",
    })
    .then((res) => {
      labelOptions.value = res.data.map((itemLabel) => {
        return {
          label: itemLabel,
          value: itemLabel,
        };
      });
    });
};

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
  formState.value = cloneDeep(initIssueMessage);
};
const handleUpdate = async () => {
  let res = await issueMessageApiClient.issueMessage.updateIssueMessage({
    name: formState.value.metadata.name,
    issueMessage: formState.value,
  });
  if (res.status == 200) {
    Toast.success("更新成功!");
  }
};

//处理issue template的筛选过滤条件
const handlerIssueTemplateOptions = () => {
  issueTemplateApiClient.issueTemplate.listIssueTemplate().then(({ data }) => {
    data.items.forEach((it) => {
      const itemOption = { label: it.spec?.name, value: it.metadata.name };
      issueTemplateFilterOptions.value.push(itemOption);
    });
  });
};

// 新增 issue
const handleSave = async (issueMessage: IssueMessage) => {
  issueMessage.spec.releaseTime = new Date().toISOString();
  issueMessage.spec.approved = true;

  const { data } = await consoleIssueMessageApiClient.issueMessage.createIssueMessage({
    issueMessage: issueMessage,
  });
  emit("save", data);
  Toast.success("发布成功");
};
const handleReset = () => {
  formState.value = toRaw(cloneDeep(initIssueMessage));
  isEditorEmpty.value = true;
};
</script>
<template>
  <VModal :title="modalTitle" :visible="visible" :width="720" @update:visible="onVisibleChange">
    <template #actions>
      <slot name="append-actions" />
    </template>
    <!-- 提交表单  -->
    <FormKit
      id="issue-message"
      type="form"
      name="issue-message"
      :config="{ validationVisibility: 'submit' }"
      @submit="onSubmit"
    >
      <div class="md:grid md:grid-cols-4 md:gap-6">
        <div class="mt-2.5 px-3 md:col-span-1">
          <div class="sticky top-0">
            <span class="text-base text-gray-900 font-medium"> Issue详情 </span>
          </div>
        </div>
        <div class="divide-gray-25 mt-5 px-3 md:col-span-3 md:mt-3 divide-y">
          <FormKit type="text" label="标题" v-model="formState.spec.title" name="title" validation="required" />
          <FormKit
            type="select"
            v-model="formState.spec.labels"
            name="labels"
            validation="required"
            label="标签"
            :options="labelOptions"
            multiple
            clearable
            searchable
            allow-create
          />
          <FormKit
            type="select"
            v-model="formState.spec.issueTemplate"
            name="issueTemplate"
            clearable
            validation="required"
            label="Issue留言模版"
            :options="issueTemplateFilterOptions"
         />
          <FormKit label="问题描述" v-model="formState.spec.content.raw" name="raw" type="textarea" rows="5" />
        </div>
      </div>
    </FormKit>
    <div class="py-5">
      <div class="border-t border-gray-200"></div>
    </div>
    <div class="md:grid md:grid-cols-4 md:gap-6">
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
          kind="IssueMessage"
          group="microimmersion.webjing.cn"
        />
      </div>
    </div>
    <template #footer>
      <VSpace>
        <VButton :loading="saving" type="secondary" @click="submitForm('issue-message')"> 提交 </VButton>
        <VButton @click="onVisibleChange(false)"> 取消 </VButton>
      </VSpace>
    </template>
  </VModal>
</template>
