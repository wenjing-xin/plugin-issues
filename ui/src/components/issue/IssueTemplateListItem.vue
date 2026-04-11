<script lang="ts" setup>
import { formatDatetime } from "@/utils/date";
import {
  Dialog,
  VDropdownItem,
  VEntity,
  VEntityField,
  Toast,
  VAvatar,
  VSpace,
  VTag,
  IconExternalLinkLine,
} from "@halo-dev/components";
import { computed, inject, type Ref, ref } from "vue";
import { useQueryClient } from "@tanstack/vue-query";

import type { ListedIssueTemplate } from "@/api/generated";
import {
  issueSubjectApiClient,
  issueTemplateApiClient,
} from "@/api/index";
import { useRouter } from "vue-router";
import { subjectTypeOptions } from "@/dictionary";
const router = useRouter();

const queryClient = useQueryClient();

const props = withDefaults(
  defineProps<{
    issueTemplate: ListedIssueTemplate;
    isSelected?: boolean;
  }>(),
  {
    isSelected: false,
  },
);

const selectedIssueTemplateNames = inject<Ref<string[]>>(
  "selectedIssueTemplateNames",
  ref([]),
);
const subjectTypeParseName = computed(() => {
  const filterRes = subjectTypeOptions.value.filter(
    (item) => item.value == props.issueTemplate.issueTemplate.spec?.subjectType,
  );
  return filterRes[0]?.label;
});
const handleDelete = async (issueTemplate: ListedIssueTemplate) => {
  Dialog.warning({
    title: "删除Issue模版",
    description: "该操作会将issue模版，该操作不可恢复。",
    confirmType: "danger",
    confirmText: "确定",
    cancelText: "取消",
    onConfirm: async () => {
      try {
        await issueTemplateApiClient.issueTemplate.deleteIssueTemplate({
          name: issueTemplate.issueTemplate.metadata.name,
        });
        Toast.success("删除成功");
      } catch (error) {
        console.error("Failed to delete issue", error);
      } finally {
        await queryClient.invalidateQueries({ queryKey: ["issueTemplates"] });
      }
    },
  });
};

const handleEditIssueTemplate = async (issueTemplate: ListedIssueTemplate) => {
  const curTemplateSubjectList =
    await issueSubjectApiClient.issueSubject.listIssueSubject({
      fieldSelector: [
        "spec.issueTemplates=(" +
          issueTemplate.issueTemplate.metadata.name +
          ")",
      ],
    });
  if (curTemplateSubjectList.data.items.length > 0) {
    Toast.warning(
      "当前模版已有依托主体在使用，无法修改模版，请新建模版或删除该模版下的主体后进行修改！",
    );
  } else {
    router.push({
      name: "IssueTemplateEditor",
      query: { name: issueTemplate.issueTemplate.metadata.name },
    });
  }
};

function handleRouteToUserDetail() {}
</script>
<template>
  <VEntity :is-selected="isSelected">
    <template #checkbox>
      <HasPermission :permissions="['plugin:issue:manage']">
        <input
          v-model="selectedIssueTemplateNames"
          :value="issueTemplate.issueTemplate.metadata.name"
          name="issue-checkbox"
          type="checkbox"
        />
      </HasPermission>
    </template>
    <template #start>
      <VEntityField
        width="27rem"
        :title="issueTemplate.issueTemplate.spec?.name"
        @click="handleEditIssueTemplate(issueTemplate)"
      >
        <template #description>
          <p
            v-tooltip="issueTemplate.issueTemplate.spec?.description"
            class="text-xs font-bold text-gray-400 tracking-wider"
          >
            {{
              issueTemplate.issueTemplate.spec?.description?.substring(0, 12)
            }}
          </p>
        </template>
      </VEntityField>
    </template>
    <template #end>
      <!--   issue模版作用范围   -->
      <VEntityField>
        <template #description>
          <VTag
            v-if="issueTemplate.issueTemplate.spec?.scope == 'SUBJECT_TYPE'"
            theme="primary"
            class="cursor-auto"
          >
            <template #leftIcon>
              <TablerCategoryFilled />
            </template>
            主体类型模版
          </VTag>
          <VTag v-else theme="secondary" class="cursor-auto">
            特定主体模版
          </VTag>
        </template>
      </VEntityField>
      <VEntityField>
        <template #description>
          <VAvatar
            v-tooltip="issueTemplate.contributorVo.displayName"
            :src="issueTemplate.contributorVo.avatar"
            :alt="issueTemplate.contributorVo.displayName"
            size="xs"
            circle
            @click="handleRouteToUserDetail()"
          ></VAvatar>
        </template>
      </VEntityField>
      <!--   针对主体类型和主体名称显示   -->
      <VEntityField>
        <template #extra>
          <VSpace
            v-if="issueTemplate.issueTemplate.spec?.scope == 'SUBJECT'"
            class="mt-1 sm:mt-0"
          >
            <a
              target="_blank"
              :href="
                '/subject/' + issueTemplate.issueTemplate?.spec?.subjectName
              "
              class="hidden text-gray-600 transition-all group-hover:inline-block hover:text-gray-900"
            >
              <IconExternalLinkLine class="h-3 w-3" />
            </a>
          </VSpace>
        </template>
        <template #description>
          <p
            v-if="issueTemplate.issueTemplate.spec?.scope == 'SUBJECT'"
            v-tooltip="issueTemplate.subjectDisplayName"
            class="px-2 py-0.5 text-xs rounded bg-gray-100"
          >
            {{ issueTemplate?.subjectDisplayName?.substring(0, 8) }}
          </p>
          <p
            v-else-if="
              issueTemplate.issueTemplate.spec?.scope == 'SUBJECT_TYPE'
            "
            class="px-2 py-0.5 text-xs rounded bg-gray-100"
          >
            {{ subjectTypeParseName }}
          </p>
        </template>
      </VEntityField>
      <VEntityField
        v-if="issueTemplate.issueTemplate.metadata.deletionTimestamp"
      >
        <template #description>
          <VStatusDot v-tooltip="`删除中`" state="warning" animate />
        </template>
      </VEntityField>
      <VEntityField
        v-if="issueTemplate.issueTemplate.metadata.creationTimestamp"
      >
        <template #description>
          <span class="truncate text-xs text-gray-500 tabular-nums">{{
            formatDatetime(
              issueTemplate.issueTemplate.metadata.creationTimestamp,
            )
          }}</span>
        </template>
      </VEntityField>
    </template>
    <template #dropdownItems>
      <VDropdownItem @click="handleEditIssueTemplate(issueTemplate)">
        编辑
      </VDropdownItem>
      <HasPermission :permissions="['plugin:issues:manage']">
        <VDropdownItem type="danger" @click="handleDelete(issueTemplate)">
          删除
        </VDropdownItem>
      </HasPermission>
    </template>
  </VEntity>
</template>
