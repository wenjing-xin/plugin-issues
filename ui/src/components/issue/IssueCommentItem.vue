<script setup lang="ts">
import {
  VEntity,
  VEntityField,
  IconReplyLine,
  VDropdownItem,
  VDropdownDivider,
  VAvatar,
  Dialog,
  Toast,
  VTag,
  VStatusDot
} from "@halo-dev/components";
import { computed, inject, type Ref } from "vue";

import type { IssueComment, ListedIssueComment } from "@/api/generated";
import { useUserAgent } from "@/composables/use-user-agent";
import { issueCommentApiClient } from "@/api";
import { formatDatetime, relativeTimeTo } from "@/utils/date";
import { useQueryClient } from "@tanstack/vue-query";

const props = defineProps<{
  comment: ListedIssueComment;
  comments: ListedIssueComment[];
}>();
const emit = defineEmits<{
  (event: "updateIssueComments"): void;
}>();
const queryClient = useQueryClient();
const { os, browser } = useUserAgent(props.comment.issueComment.spec.userAgent);

// Show hovered reply
const hoveredReply = inject<Ref<ListedIssueComment | undefined>>(
  "hoveredIssueComment",
);
const isHoveredReply = computed(() => {
  return (
    hoveredReply?.value?.issueComment.metadata.name ===
    props.comment.issueComment.metadata.name
  );
});
const creationTime = computed(() => {
  return props.comment?.issueComment.metadata.creationTimestamp;
});

const quoteIssueComment = computed(() => {
  const { quoteCommentUid: replyName } = props.comment.issueComment.spec;

  if (!replyName) {
    return undefined;
  }

  return props.comments?.find(
    (reply) => reply.issueComment.metadata.name === replyName,
  );
});

const handleShowQuoteReply = (show: boolean) => {
  if (hoveredReply) {
    hoveredReply.value = show ? quoteIssueComment.value : undefined;
  }
};

async function handleApprove(comment: IssueComment) {
  // 审核逻辑
  await issueCommentApiClient.issueComment.patchIssueComment({
    name: comment.metadata.name,
    jsonPatchInner: [
      {
        op: "add",
        path: "/spec/approved",
        value: true,
      },
    ],
  });
  Toast.success('审核成功');
  emit("updateIssueComments");
  await queryClient.invalidateQueries({ queryKey: ["issues"] });
}

function handleDeleteComment(comment: IssueComment) {
  Dialog.warning({
    title: "删除所选Issue评论",
    description: "此操作将会删除此Issue评论",
    confirmType: "danger",
    confirmText: "确定",
    cancelText: "取消",
    onConfirm: async () => {
      try {
        await issueCommentApiClient.issueComment.deleteIssueComment({
          name: comment.metadata.name,
        });
        Toast.success("删除成功");
      } catch (e) {
        console.error("Failed to delete issueMessage in batch", e);
      } finally {
        emit("updateIssueComments");
      }
    },
  });
}
</script>
<template>
  <VEntity
    v-bind="$attrs"
    class=":uno: border-l border-dashed !border-gray-200"
    :class="{ 'animate-bounce': isHoveredReply }"
  >
    <template #start>
      <VEntityField width="100%">
        <template #description>
          <div class=":uno: flex flex-col gap-2">
            <div class=":uno: mb-1 flex items-center gap-2">
              <div
                class=":uno: -m-1 p-1 inline-flex items-center gap-1.5 hover:bg-gray-100 rounded-lg cursor-pointer transition-colors"
              >
                <VAvatar
                  circle
                  :src="comment.contributorVo.avatar"
                  :alt="comment.contributorVo.displayName"
                  size="xs"
                />
                <span class=":uno: text-sm font-medium text-gray-900">
                  {{ comment.contributorVo.displayName }}
                </span>
              </div>
              <span class=":uno: text-sm text-gray-900 whitespace-nowrap">
                回复：
              </span>
            </div>
            <div class=":uno: space-y-1 text-sm text-gray-900">
              <a
                v-if="quoteIssueComment"
                class=":uno: mr-1 inline-flex flex-row items-center gap-1 rounded bg-slate-100 px-1 py-0.5 text-xs font-medium text-slate-700 hover:bg-slate-200 hover:text-slate-800 hover:underline"
                href="javascript:void(0)"
                @mouseenter="handleShowQuoteReply(true)"
                @mouseleave="handleShowQuoteReply(false)"
              >
                <IconReplyLine />
                <span>{{ quoteIssueComment.contributorVo.displayName }}</span>
              </a>
              <br v-if="quoteIssueComment" />
              <div
                class=":uno: prose !max-w-none break-words prose-pre:p-0 tracking-wider"
                v-html="comment?.issueComment.spec.content.html"
              ></div>
            </div>
            <div class=":uno: inline-flex items-center gap-1.5">
              <VTag v-bind="{theme:'primary'}" v-tooltip="comment.issueComment.spec.userAgent">
                {{ os }} {{ browser }}
              </VTag>
              <VTag v-bind="{theme:'secondary'}" v-tooltip="'IP地址'" v-if="comment.issueComment.spec.ipAddress">
                {{ comment.issueComment.spec.ipAddress }}
              </VTag>
            </div>
          </div>
        </template>
      </VEntityField>
    </template>
    <template #end>
      <VEntityField v-if="!comment?.issueComment.spec.approved">
        <template #description>
          <VStatusDot state="warning" animate text="待审核" />
        </template>
      </VEntityField>
      <VEntityField v-if="comment?.issueComment.metadata.deletionTimestamp">
        <template #description>
          <VStatusDot v-tooltip="'删除中'" state="warning" animate />
        </template>
      </VEntityField>
      <VEntityField
        v-tooltip="formatDatetime(creationTime)"
        :description="relativeTimeTo(creationTime)"
      />
    </template>

    <template #dropdownItems>
      <VDropdownItem
        v-if="!comment.issueComment.spec.approved"
        v-permission="['plugin:issues:comment:manage']"
        @click="handleApprove(comment.issueComment)"
        >审核</VDropdownItem
      >
      <VDropdownItem v-permission="['plugin:issues:comment:manage']">
        回复
      </VDropdownItem>
      <VDropdownDivider />
      <VDropdownItem
        v-permission="['plugin:issues:comment:manage']"
        @click="handleDeleteComment(comment.issueComment)"
        >删除</VDropdownItem
      >
    </template>
  </VEntity>
</template>
