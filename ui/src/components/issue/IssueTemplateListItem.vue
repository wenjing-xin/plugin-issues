<script lang="ts" setup>
  import { formatDatetime } from "@/utils/date";
  import { Dialog, VDropdownItem, VEntity, VEntityField, Toast, VAvatar, VSpace } from "@halo-dev/components";
  import { inject, type Ref, ref } from "vue";
  import { useQueryClient } from "@tanstack/vue-query";

  import type { ListedIssueTemplate } from "@/api/generated";
  import { consoleIssueTemplateApiClient, issueApiClient, issueTemplateApiClient } from "@/api/index";
  import { useRouter } from "vue-router";
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

  const selectedIssueTemplateNames = inject<Ref<string[]>>("selectedIssueTemplateNames", ref([]));

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
    const curTmeplateIssueList = await issueApiClient.issue.listIssue({ fieldSelector: ["spec.issueTemplate=" + issueTemplate.issueTemplate.metadata.name] });
    if (curTmeplateIssueList.data.items.length > 0) {
      Toast.warning("当前模版下已经存在Issue，无法修改模版，请新建模版或删除该模版下的Issue后进行修改！")
    } else {
      router.push({ name: "IssueTemplateEditor", query: { name: issueTemplate.issueTemplate.metadata.name } });
    }
  };

  function handleRouteToUserDetail() { }
</script>
<template>
  <VEntity :is-selected="isSelected">
    <template #checkbox>
      <HasPermission :permissions="['plugin:issue:manage']">
        <input v-model="selectedIssueTemplateNames" :value="issueTemplate.issueTemplate.metadata.name"
          name="issue-checkbox" type="checkbox" />
      </HasPermission>
    </template>
    <template #start>
      <VEntityField width="27rem" :title="issueTemplate.issueTemplate.spec?.name" :route="{
        name: 'IssueTemplateEditor',
        query: { name: issueTemplate.issueTemplate.metadata.name },
      }">
        <template #description>
          <div class="flex flex-col gap-1.5">
            <VSpace class="flex-wrap !gap-y-1"></VSpace>
          </div>
        </template>
      </VEntityField>
    </template>
    <template #end>
      <VEntityField>
        <template #description>
          <VAvatar v-tooltip="issueTemplate.contributorVo.displayName" :src="issueTemplate.contributorVo.avatar"
            :alt="issueTemplate.contributorVo.displayName" size="xs" circle @click="handleRouteToUserDetail()"></VAvatar>
        </template>
      </VEntityField>
      <VEntityField v-if="issueTemplate.issueTemplate.metadata.deletionTimestamp">
        <template #description>
          <VStatusDot v-tooltip="`删除中`" state="warning" animate />
        </template>
      </VEntityField>
      <VEntityField v-if="issueTemplate.issueTemplate.metadata.creationTimestamp">
        <template #description>
          <span class="truncate text-xs text-gray-500 tabular-nums">{{
            formatDatetime(issueTemplate.issueTemplate.metadata.creationTimestamp)
          }}</span>
        </template>
      </VEntityField>
    </template>
    <template #dropdownItems>
      <VDropdownItem @click="handleEditIssueTemplate(issueTemplate)"> 编辑 </VDropdownItem>
      <HasPermission :permissions="['plugin:issues:manage']">
        <VDropdownItem type="danger" @click="handleDelete((issueTemplate))"> 删除 </VDropdownItem>
      </HasPermission>
    </template>
  </VEntity>
</template>
