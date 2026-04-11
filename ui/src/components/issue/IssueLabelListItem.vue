<script lang="ts" setup>
import { formatDatetime } from "@/utils/date";
import TablerCategoryFilled from '~icons/tabler/category-filled';
import {
  Dialog,
  VDropdownItem,
  VEntity,
  VEntityField,
  Toast,
  VTag,
  VStatusDot, VSpace, IconExternalLinkLine
} from "@halo-dev/components";
import { computed, inject, type Ref, ref } from "vue";
import { useQueryClient } from "@tanstack/vue-query";
import HugeiconsGlobal from "~icons/hugeicons/global";

import type { IssueLabel, ListedIssueLabel } from "@/api/generated";
import { issueLabelApiClient } from "@/api";
import { subjectTypeOptions } from "@/dictionary";

const queryClient = useQueryClient();

const props = withDefaults(
  defineProps<{
    issueLabel: ListedIssueLabel;
    isSelected?: boolean;
  }>(),
  {
    isSelected: false,
  },
);

const emit = defineEmits<{
  (event: "update", value: IssueLabel): void;
}>();

const selectedIssueMessageNames = inject<Ref<string[]>>(
  "selectedIssueLabelNames",
  ref([]),
);
const handleDelete = async (issueLabel: ListedIssueLabel) => {
  if (issueLabel.issueNumber > 0) {
    return Toast.warning("当前标签已经关联Issue，无法删除");
  }
  Dialog.warning({
    title: "删除issue标签",
    description: "该操作会将issue标签删除，该操作不可恢复。",
    confirmType: "danger",
    confirmText: "确定",
    cancelText: "取消",
    onConfirm: async () => {
      try {
        await issueLabelApiClient.issueLabel.deleteIssueLabel({
          name: issueLabel.issueLabel.metadata.name,
        });
        Toast.success("删除成功");
      } catch (error) {
        console.error("Failed to delete issue", error);
      } finally {
        await queryClient.invalidateQueries({ queryKey: ["issueLabels"] });
      }
    },
  });
};

// 编辑issue留言
const handlerEditIssueLabel = (issueLabel: ListedIssueLabel) => {
  emit("update", issueLabel.issueLabel);
};

const subjectTypeParseName = computed(()=> {
  const filterRes = subjectTypeOptions.value.filter((item) => item.value == props.issueLabel.issueLabel.spec.subjectType);
  return filterRes[0]?.label;
})
</script>
<template>
  <VEntity :is-selected="isSelected">
    <template #checkbox>
      <HasPermission :permissions="['plugin:issue:labels:manage']">
        <input
          v-model="selectedIssueMessageNames"
          :value="issueLabel.issueLabel.metadata.name"
          name="issue-label-checkbox"
          type="checkbox"
        />
      </HasPermission>
    </template>
    <template #start>
      <VEntityField>
        <template #description>
          <div
            class="rounded-md px-2 py-1 flex items-center justify-center"
            :style="{ backgroundColor: issueLabel.issueLabel.spec.color }"
          >
            <p class="text-xs text-white">
              {{ issueLabel.issueLabel.spec.labelName }}
            </p>
          </div>
        </template>
      </VEntityField>
      <VEntityField>
        <template #description>
          <p
            v-if="issueLabel.issueLabel.spec.description"
            class="text-sm text-gray-500"
          >
            暂无描述
          </p>
          <p
            v-else
            v-tooltip="issueLabel.issueLabel.spec.description"
            class="text-sm text-gray-500"
          >
            {{ issueLabel.issueLabel.spec?.description?.substring(0, 15) }}
          </p>
        </template>
      </VEntityField>
    </template>
    <template #end>
      <VEntityField>
        <template #description>
          <VTag
            v-if="issueLabel.issueLabel.spec.scope == 'GLOBAL'"
            theme="primary"
            class="cursor-auto"
          >
            <template #leftIcon>
              <HugeiconsGlobal />
            </template>
            全局标签
          </VTag>
          <VTag v-else-if="issueLabel.issueLabel.spec.scope == 'SUBJECT_TYPE'" theme="secondary" class="cursor-auto">
            <template #leftIcon>
              <TablerCategoryFilled />
            </template>
            主体类型标签 
          </VTag>
          <VTag v-else theme="default" class="cursor-auto"> 特定主体标签 </VTag>
        </template>
      </VEntityField>
      <!--   针对主体类型和主体名称显示   -->
      <VEntityField v-if="issueLabel.issueLabel.spec.scope !== 'GLOBAL'">
        <template #extra>
          <VSpace class="mt-1 sm:mt-0" v-if="issueLabel.issueLabel.spec.scope == 'SUBJECT'">
            <a
              target="_blank"
              :href="'/subject/' + issueLabel.issueLabel?.spec?.subjectName"
              class="hidden text-gray-600 transition-all group-hover:inline-block hover:text-gray-900"
            >
              <IconExternalLinkLine class="h-3 w-3" />
            </a>
          </VSpace>
        </template>
        <template #description>
          <p v-if="issueLabel.issueLabel.spec.scope == 'SUBJECT'" class="px-2 py-0.5 text-xs rounded bg-gray-100" v-tooltip="issueLabel.subjectDisplayName">
            {{ issueLabel?.subjectDisplayName?.substring(0, 8) }}
          </p>
          <p v-else-if="issueLabel.issueLabel.spec.scope == 'SUBJECT_TYPE'" class="px-2 py-0.5 text-xs rounded bg-gray-100">
            {{subjectTypeParseName}}
          </p>
        </template>
      </VEntityField>
      <VEntityField v-if="issueLabel.issueLabel.metadata.deletionTimestamp">
        <template #description>
          <VStatusDot v-tooltip="`删除中`" state="warning" animate />
        </template>
      </VEntityField>
      <VEntityField v-if="issueLabel.issueLabel.metadata.creationTimestamp">
        <template #description>
          <span class="truncate text-xs text-gray-500 tabular-nums">{{
            formatDatetime(issueLabel.issueLabel.metadata.creationTimestamp)
          }}</span>
        </template>
      </VEntityField>
    </template>
    <template #dropdownItems>
      <HasPermission :permissions="['plugin:issue:labels:manage']">
        <VDropdownItem @click="handlerEditIssueLabel(issueLabel)">
          编辑
        </VDropdownItem>
        <VDropdownItem type="danger" @click="handleDelete(issueLabel)">
          删除
        </VDropdownItem>
      </HasPermission>
    </template>
  </VEntity>
</template>
