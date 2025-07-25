<script lang="ts" setup>
import { VModal, VButton, VSpace, Toast } from "@halo-dev/components";
import { computed, nextTick, onMounted, ref, toRaw, watchEffect } from "vue";
import type { Issue, IssueLabelOptions, IssueTemplate } from "@/api/generated";
import cloneDeep from "lodash.clonedeep";
import {
  consoleIssueApiClient, consoleIssueLabelApiClient,
  issueApiClient,
  issueTemplateApiClient
} from "@/api";
import { submitForm } from "@formkit/core";
import { useRouteQuery } from "@vueuse/router";
import TextEditor from "@/components/editor/index.vue";
const modalTitle = ref("新增issue");
const saving = ref<boolean>(false);
const props = withDefaults(
  defineProps<{
    visible: boolean;
    issueMessage?: Issue | undefined;
  }>(),
  {
    visible: false,
    issueMessage: undefined,
  },
);
const emit = defineEmits<{
  (event: "update:visible", value: boolean): void;
  (event: "close", value: boolean): void;
  (event: "save", issueMessage: Issue): void;
  (event: "update", issueMessage: Issue): void;
}>();

const currentIssueSubjectName = useRouteQuery<string>("subjectName");
const issueTemplateFilterOptions = ref<
  Array<{ label: string | undefined; value: string }>
>([]);

const initIssue: Issue = {
  kind: "Issue",
  apiVersion: "issue.webjing.com/v1alpha1",
  metadata: {
    generateName: "issue-",
    name: "",
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
    subjectName: currentIssueSubjectName.value,
    top: false
  },
  status: {
    observedVersion: 0,
    permalink: "",
    state: "AWAIT",
  },
};

const formState = ref<Issue>(cloneDeep(initIssue));

watchEffect(() => {
  if (props.issueMessage) {
    formState.value = cloneDeep(props.issueMessage);
    modalTitle.value = "编辑issue";
  }
});

onMounted(() => {
  handlerIssueTemplateOptions();
  handlerLabelOptions();
});

const labelOptions = ref<Array<{ label: string; value: string }>>([]);
const handlerLabelOptions = () => {
  consoleIssueLabelApiClient.issueLabel
    .listSubjectIssueLabels({ subjectName: currentIssueSubjectName.value })
    .then(({ data }) => {
      const labelOptionsData = data as IssueLabelOptions;
      // @ts-ignore
      labelOptions.value = labelOptionsData.issueLabelOptions;
    });
};

const isUpdateMode = computed(
  () => !!formState.value.metadata.creationTimestamp,
);
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
  formState.value = cloneDeep(initIssue);
};
const handleUpdate = async () => {
  const res = await consoleIssueApiClient.issue.updateIssue({
    issue: formState.value,
  });
  if (res.status == 200) {
    Toast.success("更新成功!");
  }
};

//处理issue template的筛选过滤条件
const handlerIssueTemplateOptions = () => {
  issueTemplateApiClient.issueTemplate.listIssueTemplate().then(({ data }) => {
    data.items.forEach((it:IssueTemplate) => {
      const itemOption = { label: it.spec?.name, value: it.metadata.name };
      issueTemplateFilterOptions.value.push(itemOption);
    });
  });
};

// 新增 issue
const handleSave = async (issue: Issue) => {
  issue.spec.releaseTime = new Date().toISOString();
  issue.spec.approved = true;

  const { data } = await consoleIssueApiClient.issue.createIssue({
    issue: issue,
  });
  emit("save", data);
  Toast.success("发布成功");
};
const handleReset = () => {
  formState.value = toRaw(cloneDeep(initIssue));
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
    <div class="md:grid md:grid-cols-4 md:gap-6">
      <div class="mt-2.5 px-3 md:col-span-1">
        <div class="sticky top-0">
          <span class="text-base text-gray-900 font-medium"> Issue详情 </span>
        </div>
      </div>
      <!-- 提交表单  -->
      <div class="divide-gray-25 mt-5 px-3 md:col-span-3 md:mt-3 divide-y">
        <FormKit
          id="issue-message"
          type="form"
          name="issue-message"
          :config="{ validationVisibility: 'submit' }"
          @submit="onSubmit"
        >
          <FormKit
            v-model="formState.spec.title"
            type="text"
            label="标题"
            name="title"
            validation="required"
          />
          <FormKit
            v-model="formState.spec.labels"
            type="select"
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
            v-model="formState.spec.issueTemplate"
            type="select"
            name="issueTemplate"
            clearable
            validation="required"
            label="Issue模版"
            :options="issueTemplateFilterOptions"
          />
          <FormKit
            v-model="formState.spec.assignees"
            name="assignees"
            label="设置经办人"
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
            help="Issue创建者和经办人将会收到和此条Issue相关的所有通知"
          />
        </FormKit>
        <div class="space-y-2 my-2 py-2">
          <p class="text-sm font-bold text-gray-600">Issue内容</p>
          <TextEditor
            v-model:raw="formState.spec.content.raw"
            v-model:html="formState.spec.content.html"
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
    <div class="md:grid md:grid-cols-4 md:gap-6">
      <div class="px-3 md:col-span-1">
        <div class="sticky top-0">
          <span class="text-base text-gray-900 font-medium"> 元数据 </span>
        </div>
      </div>
      <div
        class="divide-gray-25 mt-5 w-full px-3 md:col-span-3 md:mt-0 divide-y"
      >
        <AnnotationsForm
          v-if="visible"
          :key="formState.metadata.name"
          ref="annotationsFormRef"
          :value="formState.metadata.annotations"
          kind="Issue"
          group="issue.webjing.com"
        />
      </div>
    </div>
    <template #footer>
      <VSpace>
        <VButton
          :loading="saving"
          type="secondary"
          @click="submitForm('issue-message')"
        >
          提交
        </VButton>
        <VButton @click="onVisibleChange(false)"> 取消 </VButton>
      </VSpace>
    </template>
  </VModal>
</template>
