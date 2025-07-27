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
import type {
  Issue,
  IssueLabelItem,
  IssueLabelOptions,
  IssueTemplate,
  TemplateField,
} from "@/api/generated";
import cloneDeep from "lodash.clonedeep";
import {
  consoleIssueApiClient,
  consoleIssueLabelApiClient,
  issueTemplateApiClient,
  ucIssueApiClient,
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
    defaultTemplate: string;
  }>(),
  {
    visible: false,
    issueMessage: undefined,
    defaultTemplate: "",
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

const isUpdateMode = computed(
  () => !!formState.value.metadata.creationTimestamp,
);
const initIssue: Issue = {
  kind: "Issue",
  apiVersion: "issue.webjing.com/v1alpha1",
  metadata: {
    generateName: "issue-",
    name: "",
    annotations: {},
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
    subjectName: currentIssueSubjectName.value,
    top: false,
    issueTemplate: "",
  },
  status: {
    observedVersion: 0,
    permalink: "",
    state: "AWAIT",
  },
};

const formState = ref<Issue>(cloneDeep(initIssue));

watch(
  () => props.visible,
  (visible) => {
    if (visible) {
      if (props.issueMessage) {
        formState.value = cloneDeep(props.issueMessage);
        modalTitle.value = "编辑issue";
      } else {
        formState.value = cloneDeep(initIssue);
        formState.value.spec.issueTemplate = props.defaultTemplate;
        modalTitle.value = "新增issue";
      }
    }
  },
  { immediate: true }
);
watch(
  () => formState.value.spec.issueTemplate,
  async (newVal, oldVal) => {
    if (newVal && newVal !== oldVal) {
      await nextTick();
      handlerRenderTemplate(newVal);
    }
  },
  {
    immediate: true,
  },
);
const issueTemplateRenderData = ref<Array<TemplateField>>();
const handlerTemplateChange = (selectedOption: any) => {
  nextTick(() => {
    if (selectedOption.length) {
      handlerRenderTemplate(selectedOption[0].value);
    } else {
      issueTemplateRenderData.value = [];
    }
  });
};

const handlerRenderTemplate = (templateName: string) => {
  ucIssueApiClient.issue
    .fetchIssueTemplateData({ templateName })
    .then((res) => {
      if (res.status == 200) {
        // 新增模式下，先初始化 customAnnotations
        if (!isUpdateMode.value) {
          nextTick(() => {
            if (annotationsFormRef.value) {
              if (!annotationsFormRef.value.customAnnotations) {
                annotationsFormRef.value.customAnnotations = {};
              }
              (res.data.annotationFields || []).forEach((field) => {
                annotationsFormRef.value.customAnnotations[field] = "";
              });
              // 先初始化 customAnnotations，再赋值，保证渲染时 v-model 一定有对象
              issueTemplateRenderData.value = res.data.components;
            } else {
              // 如果还没挂载，延迟再试
              setTimeout(() => handlerRenderTemplate(templateName), 50);
            }
          });
        } else {
          // 编辑模式直接赋值
          issueTemplateRenderData.value = res.data.components;
        }
      } else {
        issueTemplateRenderData.value = [];
      }
    });
};

onMounted(() => {
  handlerIssueTemplateOptions();
  handlerLabelOptions();
});

const labelOptions = ref<Array<IssueLabelItem>>([]);
const handlerLabelOptions = () => {
  consoleIssueLabelApiClient.issueLabel
    .listSubjectIssueLabels({ subjectName: currentIssueSubjectName.value })
    .then(({ data }) => {
      const labelOptionsData = data as IssueLabelOptions;
      labelOptions.value =
        labelOptionsData.issueLabelOptions as Array<IssueLabelItem>;
    });
};

const isEditorEmpty = ref<boolean>(true);

const onVisibleChange = (visible: boolean) => {
  emit("update:visible", visible);
  if (!visible) {
    emit("close", false);
    issueTemplateRenderData.value = []
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
    data.items.forEach((it: IssueTemplate) => {
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
            :validation="
              issueTemplateFilterOptions.length > 0 ? 'required' : ''
            "
            :disabled="isUpdateMode"
            label="Issue模版"
            :options="issueTemplateFilterOptions"
            @change="handlerTemplateChange"
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
    <div v-if="formState.spec.issueTemplate" class="py-5">
      <div class="border-t border-gray-200"></div>
    </div>
    <!--  动态渲染的模版  -->
    <div
      v-if="formState.spec.issueTemplate"
      class="md:grid md:grid-cols-4 md:gap-6"
    >
      <div class="px-3 md:col-span-1">
        <div class="sticky top-0">
          <span class="text-base text-gray-900 font-medium"> Issue模版 </span>
        </div>
      </div>
      <div
        class="divide-gray-25 mt-5 w-full px-3 md:col-span-3 md:mt-0 divide-y"
      >
        <template
          v-for="itemComponent in issueTemplateRenderData"
          :key="itemComponent.key"
        >
          <FormKit
            v-if="
              itemComponent.type === 'TEXT' &&
              annotationsFormRef?.customAnnotations
            "
            v-model="annotationsFormRef.customAnnotations[itemComponent.key]"
            type="text"
            :label="itemComponent.title"
            :placeholder="itemComponent.placeholder"
            :help="itemComponent.helpText"
            :validation="
              itemComponent.requiredMode === 'REQUIRED' ? 'required' : ''
            "
            :min="itemComponent.minLength"
            :max="itemComponent.maxLength"
          />
          <FormKit
            v-else-if="
              itemComponent.type === 'SELECT' &&
              annotationsFormRef?.customAnnotations &&
              itemComponent.fieldOptions
            "
            v-model="annotationsFormRef.customAnnotations[itemComponent.key]"
            type="select"
            :label="itemComponent.title"
            :options="
              itemComponent?.fieldOptions.map((o) => ({
                label: o.label,
                value: o.generateVal,
              }))
            "
            clearable
          />
          <FormKit
            v-else-if="
              itemComponent.type === 'RADIO' &&
              annotationsFormRef?.customAnnotations &&
              itemComponent.fieldOptions
            "
            v-model="annotationsFormRef.customAnnotations[itemComponent.key]"
            type="radio"
            :label="itemComponent.title"
            :options="
              itemComponent?.fieldOptions.map((o) => ({
                label: o.label,
                value: o.generateVal,
              }))
            "
          />
          <FormKit
            v-else-if="
              itemComponent.type === 'TEXT_AREA' &&
              annotationsFormRef?.customAnnotations &&
              itemComponent.fieldOptions
            "
            v-model="annotationsFormRef.customAnnotations[itemComponent.key]"
            outer-class="w-[91%] mx-auto"
            :label="itemComponent.title"
            name="content"
            :validation="itemComponent.requiredMode"
            :rows="itemComponent.rows"
            type="textarea"
            :placeholder="itemComponent.placeholder"
            :help="itemComponent.helpText"
          ></FormKit>
          <FormKit
            v-else-if="
              itemComponent.type === 'PASSWORD' &&
              annotationsFormRef?.customAnnotations &&
              itemComponent.fieldOptions
            "
            v-model="annotationsFormRef.customAnnotations[itemComponent.key]"
            outer-class="w-[91%] mx-auto"
            :label="itemComponent.title"
            name="content"
            :validation="itemComponent.requiredMode"
            type="password"
            :placeholder="itemComponent.placeholder"
            :help="itemComponent.helpText"
          />
          <FormKit
            v-else-if="
              itemComponent.type === 'EMAIL' &&
              annotationsFormRef?.customAnnotations &&
              itemComponent.fieldOptions
            "
            v-model="annotationsFormRef.customAnnotations[itemComponent.key]"
            outer-class="w-[91%] mx-auto"
            :label="itemComponent.title"
            name="content"
            :validation="itemComponent.requiredMode"
            type="email"
            :placeholder="itemComponent.placeholder"
            :help="itemComponent.helpText"
          />
        </template>
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
