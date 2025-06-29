<script lang="ts" setup>
  import { type FunctionalComponent, ref, type SVGAttributes, watchEffect, onBeforeMount } from "vue";
  import draggable from "vuedraggable";
  import FluentMailTemplate20Regular from "~icons/fluent/mail-template-20-regular";
  import {
    VButton,
    VPageHeader,
    VSpace,
    IconSave,
    IconClose,
    IconDeleteBin,
    VTabs,
    VTabItem,
    Dialog,
    Toast,
  } from "@halo-dev/components";
  import { useRouter } from "vue-router";
  import { type IssueTemplate, type TemplateField, TemplateFieldTypeEnum } from "@/api/generated";
  import UilText from "~icons/uil/text";
  import BiTextareaT from "~icons/bi/textarea-t";
  import MingcuteRadioboxLine from "~icons/mingcute/radiobox-line";
  import GgSelect from "~icons/gg/select";
  import MynauiLockPassword from "~icons/mynaui/lock-password";
  import MageEmail from "~icons/mage/email";
  import { useRouteQuery } from "@vueuse/router";

  import { useCurrentUserDetailFetch } from "@/composables/use-consoleApiclient";
  import { consoleIssueTemplateApiClient, issueCommentApiClient, issueTemplateApiClient } from "@/api";
  const currentEditTempalte = useRouteQuery<string | undefined>("name");

  interface Component {
    id: string;
    name: string;
    value: string;
    icon: FunctionalComponent<SVGAttributes>;
    attrs: TemplateField;
  }

  const router = useRouter();
  const initIssueTemplate = ref<IssueTemplate>({
    apiVersion: "issue.webjing.com/v1alpha1",
    kind: "IssueTemplate",
    metadata: {
      generateName: "issue-template-",
      name: "",
    },
    spec: {
      name: "",
      description: "",
      owner: "",
      fields: {},
    },
  });
  // 右侧详情选项卡
  const detailActiveId = ref<string>("templateInfo");

  const { currentUserDetail } = useCurrentUserDetailFetch();
  const fieldList = ref<{ [key: string]: TemplateField }>({});

  const basicComponents = ref<Component[]>([
    {
      id: "formText",
      name: "文本框",
      value: "text",
      icon: UilText,
      attrs: {
        key: "",
        title: "",
        defaultValue: "",
        type: TemplateFieldTypeEnum.Text,
        requiredMode: "",
        placeholder: "",
        fieldOptions: {},
        helpText: "",
        minLength: 0,
        maxLength: 20,
        rows: 0,
        validate: "",
      },
    },
    {
      id: "formTextarea",
      name: "文本域",
      value: "textarea",
      icon: BiTextareaT,
      attrs: {
        key: "",
        title: "",
        defaultValue: "",
        type: TemplateFieldTypeEnum.TextArea,
        requiredMode: "",
        placeholder: "",
        fieldOptions: {},
        helpText: "",
        minLength: 0,
        maxLength: 100,
        rows: 3,
        validate: "",
      },
    },
    {
      id: "formRadio",
      name: "单选框组",
      value: "radio",
      icon: MingcuteRadioboxLine,
      attrs: {
        key: "",
        title: "",
        defaultValue: "",
        type: TemplateFieldTypeEnum.Radio,
        requiredMode: "",
        placeholder: "",
        helpText: "",
        fieldOptions: {},
        minLength: 0,
        maxLength: 0,
        rows: 0,
        validate: "",
      },
    },
    {
      id: "formSelect",
      name: "下拉选择框",
      value: "select",
      icon: GgSelect,
      attrs: {
        key: "",
        title: "",
        defaultValue: "",
        type: TemplateFieldTypeEnum.Select,
        requiredMode: "",
        placeholder: "",
        helpText: "",
        fieldOptions: {},
        minLength: 0,
        maxLength: 0,
        rows: 0,
        validate: "",
      },
    },
    {
      id: "formPassword",
      name: "密码输入框",
      value: "password",
      icon: MynauiLockPassword,
      attrs: {
        key: "",
        title: "",
        defaultValue: "",
        type: TemplateFieldTypeEnum.Password,
        requiredMode: "",
        placeholder: "",
        helpText: "",
        fieldOptions: {},
        minLength: 0,
        maxLength: 0,
        rows: 0,
        validate: "",
      },
    },
    {
      id: "formEmail",
      name: "邮箱",
      value: "email",
      icon: MageEmail,
      attrs: {
        key: "",
        title: "",
        defaultValue: "",
        type: TemplateFieldTypeEnum.Email,
        requiredMode: "",
        placeholder: "",
        helpText: "",
        fieldOptions: {},
        minLength: 0,
        maxLength: 0,
        rows: 0,
        validate: "",
      },
    },
  ]);

  const isUpdateMode = ref(false);
  const templateLayout = ref<Component[]>([]);
  const selectedComponent = ref<Component | null>(null);

  watchEffect(() => {
    if (templateLayout.value.length == 0) {
      selectedComponent.value = null;
    }
    if (currentUserDetail?.value?.metadata.name && initIssueTemplate.value.spec) {
      initIssueTemplate.value.spec.owner = currentUserDetail?.value?.metadata.name;
    }
  });
  const onLayoutChange = (event: any) => {
    if (event.added) {
      const uniqueKey = `${event.added.element.id}${Date.now()}`;
      event.added.element.attrs.key = uniqueKey;
      event.added.element.attrs.title = uniqueKey;
      event.added.element.attrs.fieldOptions = {};
    }
  };

  // 关闭创建issue模版页面
  const handlerCloseTemplate = () => {
    Dialog.warning({
      title: "关闭页面",
      description: "确认关闭当前页面吗",
      confirmType: "primary",
      confirmText: "确定",
      cancelText: "取消",
      onConfirm: async () => {
        router.push({ name: "IssueTemplate" });
      },
    });
  };

  const handlerSelectComponent = (component: Component) => {
    selectedComponent.value = null;
    selectedComponent.value = component;
  };

  /**
   * 新增或更新issue模版
   */
  const handlerSaveTemplate = () => {
    // 获取所有设置的模版字段
    templateLayout.value?.forEach((item) => {
      fieldList.value[item.attrs.key] = item.attrs;
    });
    if (initIssueTemplate.value.spec) {
      initIssueTemplate.value.spec.fields = fieldList.value;
    }
    if (isUpdateMode.value) {
      // 更新issue模版
      initIssueTemplate.value.metadata
      issueTemplateApiClient.issueTemplate.updateIssueTemplate({
        name: currentEditTempalte.value as string,
        issueTemplate: initIssueTemplate.value
      }).then(res => {
        if (res.status == 200) {
          Toast.success("修改模版成功");
          router.push({ name: "IssueTemplate" });
        } else {
          Toast.error(res.statusText);
        }
      })
    } else {
      // 新增issue模版
      consoleIssueTemplateApiClient.issueTemplate
        .createIssueTemplate({
          issueTemplate: initIssueTemplate.value,
        })
        .then((res) => {
          if (res.status == 200) {
            Toast.success("新增模版成功");
            router.push({ name: "IssueTemplate" });
          } else {
            Toast.error(res.statusText);
          }
        });
    }
  };

  const handlerDeleteComponent = (component: Component) => {
    const index = templateLayout.value.findIndex((item) => item.id === component.id);
    if (index !== -1) {
      templateLayout.value.splice(index, 1);
    }
  };

  // Clone function to ensure a new object is created
  const cloneComponent = (original: Component) => {
    return {
      ...original,
      attrs: { ...original.attrs, fieldOptions: { ...original.attrs.fieldOptions } },
    };
  };

  // Function to convert fieldOptions object to array
  const convertFieldOptionsToArray = (fieldOptions: { [key: string]: string }) => {
    return Object.entries(fieldOptions).map(([key, value]) => ({ label: value, value }));
  };

  // Function to add a new option
  const addOption = (component: Component) => {
    if (component.attrs.fieldOptions) {
      const label = `option${Date.now()}`;
      component.attrs.fieldOptions[label] = "选项值";
    }
  };

  // Function to remove an option
  const removeOption = (component: Component, label: string) => {
    if (component.attrs.fieldOptions) {
      delete component.attrs.fieldOptions[label];
    }
  };

  // Function to convert IssueTemplate to Component[]
  const convertIssueTemplateToComponents = (issueTemplate: IssueTemplate): Component[] => {
    const components: Component[] = [];
    if (issueTemplate.spec && issueTemplate.spec.fields) {
      for (const key in issueTemplate.spec.fields) {
        const field = issueTemplate.spec.fields[key];
        const basicComponent = basicComponents.value.find(bc => bc.attrs.type === field.type);
        if (basicComponent) {
          const component: Component = {
            id: basicComponent.id, // Use the id from basicComponents
            name: field.title,
            value: getFormKitType(field.type), // Map to FormKit type
            icon: basicComponent.icon, // Use the icon from basicComponents
            attrs: {
              ...field,
              fieldOptions: field.fieldOptions || {},
            },
          };
          components.push(component);
        }
      }
    }
    return components;
  };

  // Function to map field type to FormKit type
  const getFormKitType = (type: TemplateFieldTypeEnum): string => {
    switch (type) {
      case TemplateFieldTypeEnum.Text:
        return 'text';
      case TemplateFieldTypeEnum.TextArea:
        return 'textarea';
      case TemplateFieldTypeEnum.Radio:
        return 'radio';
      case TemplateFieldTypeEnum.Select:
        return 'select';
      case TemplateFieldTypeEnum.Password:
        return 'password';
      case TemplateFieldTypeEnum.Email:
        return 'email';
      default:
        return 'text'; // Default type
    }
  };

  // Function to get the icon based on the type
  const getIconForType = (type: TemplateFieldTypeEnum): FunctionalComponent<SVGAttributes> => {
    switch (type) {
      case TemplateFieldTypeEnum.Text:
        return UilText;
      case TemplateFieldTypeEnum.TextArea:
        return BiTextareaT;
      case TemplateFieldTypeEnum.Radio:
        return MingcuteRadioboxLine;
      case TemplateFieldTypeEnum.Select:
        return GgSelect;
      case TemplateFieldTypeEnum.Password:
        return MynauiLockPassword;
      case TemplateFieldTypeEnum.Email:
        return MageEmail;
      default:
        return UilText; // Default icon
    }
  };

  // Initialize current edit template
  const initCurEditTemplate = async () => {
    if (currentEditTempalte.value) {
      const result = await issueTemplateApiClient.issueTemplate.getIssueTemplate({ name: currentEditTempalte.value });
      const editIssueTemplate = result.data as IssueTemplate;
      initIssueTemplate.value = editIssueTemplate;
      console.log(editIssueTemplate, "current update template");
      isUpdateMode.value = true;

      // Convert IssueTemplate to Component[]
      const newComponents = convertIssueTemplateToComponents(editIssueTemplate);

      // Merge with existing templateLayout
      templateLayout.value = [...templateLayout.value, ...newComponents];
    } else {
      isUpdateMode.value = false;
    }
  };

  onBeforeMount(async () => {
    await initCurEditTemplate();
  });
</script>

<template>
  <VPageHeader title="Issue 模版">
    <template #icon>
      <FluentMailTemplate20Regular class="mr-2 self-center" />
    </template>
    <template #actions>
      <VSpace v-permission="['plugin:issueTemplates:manage']">
        <VButton type="default" size="sm" @click="handlerCloseTemplate">
          <template #icon>
            <IconClose class="h-full w-full" />
          </template>
          关闭页面
        </VButton>
        <VButton type="primary" size="md" @click="handlerSaveTemplate">
          <template #icon>
            <IconSave class="h-full w-full" />
          </template>
          保存
        </VButton>
      </VSpace>
    </template>
  </VPageHeader>
  <div class="h-full w-full p-0 md:p-4">
    <div class="h-full w-full flex rounded bg-white divide-x">
      <!-- 左侧基础组件列表 -->
      <div class="h-full w-1/4 p-4">
        <h3 class="mb-2 text-lg font-bold">基础组件</h3>
        <draggable v-model="basicComponents" class="grid grid-cols-2 gap-2"
          :group="{ name: 'components', pull: 'clone', put: false }" item-key="id" :clone="cloneComponent">
          <template #item="{ element }">
            <div
              class="flex cursor-move items-center gap-x-2 border rounded-md border-dashed bg-indigo-50 p-2 hover:border-dotted">
              <div class="rounded-full bg-indigo-100 p-1.5">
                <component :is="element.icon" class="text-sm" />
              </div>
              <span class="text-sm text-slate-6">{{ element.name }}</span>
            </div>
          </template>
        </draggable>
      </div>

      <!-- 中间布局区域 -->
      <div class="h-full w-1/2 overflow-y-hidden p-4">
        <h3 class="mb-2 text-base font-bold">布局区域</h3>
        <draggable v-model="templateLayout" class="h-full w-full space-y-2"
          :group="{ name: 'components', pull: 'clone', put: true }" @change="onLayoutChange">
          <template #item="{ element }">
            <div :class="{ 'bg-indigo-50': selectedComponent?.attrs.key === element?.attrs.key }"
              class="relative w-full flex items-center justify-center border rounded-md border-dashed py-2 hover:border-dotted hover:bg-indigo-50"
              @click="handlerSelectComponent(element)">
              <FormKit v-if="
                (element.attrs && element.attrs.type == 'TEXT') ||
                element.attrs.type == 'EMAIL' ||
                element.attrs.type == 'PASSWORD'
              " v-model="element.attrs.defaultValue" outer-class="w-[91%] mx-auto" :disabled="true"
                :label="element.attrs.title" name="content" :validation="element.attrs.required" :type="element.value"
                :placeholder="element.attrs.placeholder" :help="element.attrs?.helpText">
              </FormKit>
              <FormKit v-else-if="element.attrs && element.attrs.type == 'TEXT_AREA'"
                v-model="element.attrs.defaultValue" outer-class="w-[91%] mx-auto" :disabled="true"
                :label="element.attrs.title" name="content" :validation="element.attrs.required"
                :rows="element.attrs.rows" :type="element.value" :placeholder="element.attrs.placeholder"
                :help="element.attrs?.helpText"></FormKit>
              <FormKit v-else v-model="element.attrs.defaultValue" outer-class="w-[91%] mx-auto"
                :label="element.attrs?.title" :help="element.attrs?.helpText" name="content"
                :validation="element.attrs.required" :type="element.value" :placeholder="element.attrs?.placeholder"
                :options="convertFieldOptionsToArray(element.attrs.fieldOptions)"></FormKit>
              <IconDeleteBin class="absolute right-1 top-1 cursor-pointer text-red-600"
                @click.stop="handlerDeleteComponent(element)" />
            </div>
          </template>
        </draggable>
      </div>

      <!--  右侧属性布局 -->
      <div class="h-full w-1/4 overflow-y-auto p-1">
        <VTabs v-model:active-id="detailActiveId" type="outline">
          <VTabItem id="templateInfo" label="模版信息">
            <div class="p-2">
              <FormKit v-if="initIssueTemplate.spec" v-model="initIssueTemplate.spec.name" outer-class="w-full"
                label="模版名称" name="name" type="text" />
              <FormKit v-if="initIssueTemplate.spec" v-model="initIssueTemplate.spec.description" outer-class="w-full"
                label="模版描述" name="name" type="textarea" />
              <FormKit v-if="initIssueTemplate.spec" v-model="initIssueTemplate.spec.owner" outer-class="w-full"
                disabled label="创建用户" name="owner" type="text" />
            </div>
          </VTabItem>
          <VTabItem id="componentProperties" label="组件属性">
            <div v-if="selectedComponent" class="p-2">
              <FormKit v-model="selectedComponent.attrs.title" outer-class="w-full" label="标题" name="title" type="text">
              </FormKit>
              <FormKit v-model="selectedComponent.attrs.defaultValue" outer-class="w-full" label="默认值"
                name="defaultValue" type="text"></FormKit>
              <FormKit v-model="selectedComponent.attrs.requiredMode" outer-class="w-full" label="是否必填字段"
                name="required" type="radio" :options="[
                  { label: '必填', value: 'required' },
                  { label: '选填', value: '' },
                ]"></FormKit>
              <FormKit v-if="selectedComponent.id !== 'formRadio'" v-model="selectedComponent.attrs.placeholder"
                outer-class="w-full" label="占位文本" name="placeholder" type="text"></FormKit>
              <FormKit v-model="selectedComponent.attrs.helpText" outer-class="w-full" label="帮助文本" name="helpText"
                type="text"></FormKit>
              <!--   针对文本元素的最大和最小长度设置  -->
              <FormKit v-if="selectedComponent.id === 'formText' || selectedComponent.id === 'formTextarea'"
                v-model="selectedComponent.attrs.minLength" type="number" label="最小长度" name="minLength" step="1" />
              <FormKit v-if="selectedComponent.id === 'formText' || selectedComponent.id === 'formTextarea'"
                v-model="selectedComponent.attrs.maxLength" type="number" label="最大长度" name="maxLength" step="1" />
              <FormKit v-if="selectedComponent.id === 'formTextarea'" v-model="selectedComponent.attrs.rows"
                type="number" help="最大长度" label="行数" name="rows" step="1" />
              <!-- 添加选项 -->
              <div v-if="selectedComponent.id === 'formRadio' || selectedComponent.id === 'formSelect'"
                class="space-y-2">
                <h4 class="font-bold">选项设置</h4>
                <div v-if="selectedComponent.attrs.fieldOptions" class="space-y-2">
                  <div v-for="(label, key) in selectedComponent.attrs.fieldOptions" :key="key"
                    class="flex items-center justify-between gap-x-2 border rounded px-2">
                    <input v-model="selectedComponent.attrs.fieldOptions[key]"
                      class="border-none p-1 text-sm outline-none" placeholder="选项值" />
                    <VButton type="danger" size="xs" @click="removeOption(selectedComponent, key as string)">删除
                    </VButton>
                  </div>
                </div>
                <VButton size="sm" @click="addOption(selectedComponent)">添加选项</VButton>
              </div>
            </div>
          </VTabItem>
        </VTabs>
      </div>
    </div>
  </div>
</template>
