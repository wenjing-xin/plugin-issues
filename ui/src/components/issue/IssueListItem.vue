<script lang="ts" setup>
import { formatDatetime } from "@/utils/date";
import {
  Dialog,
  VDropdownItem,
  VEntity,
  VEntityField,
  Toast,
  VDropdownDivider,
  VAvatar,
  VStatusDot,
  IconExternalLinkLine,
  VSpace,
} from "@halo-dev/components";
import { computed, inject, type Ref, ref } from "vue";
import { useQueryClient } from "@tanstack/vue-query";

import type { Issue, ListedIssue } from "@/api/generated";
import {issueApiClient} from "@/api";

const queryClient = useQueryClient();

const props = withDefaults(
  defineProps<{
    issue: ListedIssue;
    isSelected?: boolean;
  }>(),
  {
    isSelected: false,
  },
);

const emit = defineEmits<{
  (event: "update", value: Issue): void;
}>();
const selectedIssueMessageNames = inject<Ref<string[]>>("selectedIssueMessageNames", ref([]));

const handleDelete = async (issue: ListedIssue) => {
  Dialog.warning({
    title: "删除issue留言",
    description: "该操作会将issue留言删除，该操作不可恢复。",
    confirmType: "danger",
    confirmText: "确定",
    cancelText: "取消",
    onConfirm: async () => {
      try {
        await issueApiClient.issue.deleteIssue({
          name: issue.issue.metadata.name,
        });
        Toast.success("删除成功");
      } catch (error) {
        console.error("Failed to delete issue", error);
      } finally {
        await queryClient.invalidateQueries({ queryKey: ["issueMessages"] });
      }
    },
  });
};

const issueStatus = computed(() => {
  const { status } = props.issue.issue;
  return status?.state === "AWAIT" ? "待处理" : status?.state == "PROGRESS" ? "进行中" : "已关闭";
});

const handlerViewDetail= (issue: ListedIssue) =>{
  
}
// 编辑issue留言
const handlerEditIssueMessage = (issue: ListedIssue)=> {
  emit("update", issue.issue);
}

const handleCloseIssue = async (name:string) => {
  Dialog.warning({
    title: `确定关闭Issue留言「${props.issue.issue.spec.title}」?`,
    confirmType: "primary",
    confirmText: "确定",
    cancelText: "取消",
    onConfirm: async () => {
      try {
        await issueApiClient.issue.patchIssue({
          name: name,
          jsonPatchInner: [
            {
              op: "add",
              path: "/status/state",
              value: "CLOSED",
            },
          ],
        });
        Toast.success("关闭Issue成功");
      } catch (error) {
        console.error("Failed to end issue", error);
      } finally {
        await queryClient.invalidateQueries({ queryKey: ["issueMessages"] });
      }
    },
  });
};

function handleRouteToUserDetail() {}
</script>
<template>
  <VEntity :is-selected="isSelected">
    <template #checkbox>
      <HasPermission :permissions="['plugin:issue:manage']">
        <input
          v-model="selectedIssueMessageNames"
          :value="issue.issue.metadata.name"
          name="issue-checkbox"
          type="checkbox"
        />
      </HasPermission>
    </template>
    <template #start>
      <VEntityField :title="issue.issue.spec.title" width="27rem">
        <template #extra>
          <VSpace class="mt-1 sm:mt-0">
            <a
              target="_blank"
              :href="issue.issue?.status?.permalink"
              class="hidden text-gray-600 transition-all group-hover:inline-block hover:text-gray-900"
            >
              <IconExternalLinkLine class="h-3 w-3" />
            </a>
          </VSpace>
        </template>
        <template #description>
          <div class="flex flex-col gap-1.5">
            <VSpace class="flex-wrap !gap-y-1">
              <span class="text-xs text-gray-500"> 点赞：{{ issue.stats.upvote }} </span>
              <span class="text-xs text-gray-500">评论数：{{ issue.stats.totalComment }}</span>
            </VSpace>
          </div>
        </template>
      </VEntityField>
    </template>
    <template #end>
      <VEntityField>
        <template #description>
          <VStatusDot v-if="issue.issue.status?.state == 'AWAIT'" state="warning" animate>
            <template #text>
              <p class="text-xs">{{ issueStatus }}</p>
            </template>
          </VStatusDot>
          <VStatusDot v-else-if="issue.issue.status?.state == 'PROGRESS'" state="default" animate>
            <template #text>
              <p class="text-xs">{{ issueStatus }}</p>
            </template>
          </VStatusDot>
          <VStatusDot v-else state="success">
            <template #text>
              <p class="text-xs">{{ issueStatus }}</p>
            </template>
          </VStatusDot>
        </template>
      </VEntityField>
      <VEntityField>
        <template #description>
          <VAvatar
            v-tooltip="issue.contributorVo.displayName"
            :src="issue.contributorVo.avatar"
            :alt="issue.contributorVo.displayName"
            size="xs"
            circle
            @click="handleRouteToUserDetail()"
          ></VAvatar>
        </template>
      </VEntityField>
      <VEntityField v-if="issue.issue.metadata.deletionTimestamp">
        <template #description>
          <VStatusDot v-tooltip="`删除中`" state="warning" animate />
        </template>
      </VEntityField>
      <VEntityField v-if="issue.issue.metadata.creationTimestamp">
        <template #description>
          <span class="truncate text-xs text-gray-500 tabular-nums">{{formatDatetime(issue.issue.metadata.creationTimestamp)}}</span>
        </template>
      </VEntityField>
    </template>
    <template #dropdownItems>
      <VDropdownItem @click="handlerViewDetail(issue)"> 详情 </VDropdownItem>
      <VDropdownItem @click="handlerEditIssueMessage(issue)"> 编辑 </VDropdownItem>
      <HasPermission :permissions="['plugin:issues:manage']">
        <VDropdownItem v-if="issue.issue.status?.state != 'CLOSED'" @click="handleCloseIssue(issue.issue.metadata.name)">
          关闭
        </VDropdownItem>
        <VDropdownDivider />
        <VDropdownItem type="danger" @click="handleDelete(issue)"> 删除 </VDropdownItem>
      </HasPermission>
    </template>
  </VEntity>
</template>
