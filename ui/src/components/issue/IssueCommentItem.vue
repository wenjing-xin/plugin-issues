<script setup lang="ts">
import {
  VEntity,
  VEntityField,
  IconReplyLine,
  VDropdownItem,
  VDropdownDivider,
  VAvatar,
} from "@halo-dev/components";
import { computed, inject, ref, type Ref } from "vue";
import type { IssueComment, ListedIssueComment } from "@/api/generated";
import { formatDatetime, relativeTimeTo } from "@/utils/date";
const props = defineProps<{
  comment: ListedIssueComment;
  comments: ListedIssueComment[];
}>();

// Show hovered reply
const hoveredIssueComment = inject<Ref<ListedIssueComment | undefined>>(
  "hoveredIssueComment",
);
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

function handleApprove(comment: IssueComment) {
  // 审核逻辑
}

function handleDeleteComment(comment: IssueComment) {
  // 删除逻辑
}
</script>
<template>
  <VEntity
    v-bind="$attrs"
    class="border-l border-dashed !border-gray-200"
    :class="{ 'animate-breath': isHoveredReply }"
  >
    <template #start>
      <VEntityField width="100%">
        <template #description>
          <div class="flex flex-col gap-2">
            <div class="mb-1 flex items-center gap-2">
              <div
                class="-m-1 p-1 inline-flex items-center gap-1.5 hover:bg-gray-100 rounded-lg cursor-pointer transition-colors"
              >
                <VAvatar
                  circle
                  :src="comment.contributorVo.avatar"
                  :alt="comment.contributorVo.displayName"
                  size="xs"
                />
                <span class="text-sm font-medium text-gray-900">
                  {{ comment.contributorVo.displayName }}
                </span>
              </div>
              <span class="text-sm text-gray-900 whitespace-nowrap">
                回复
              </span>
            </div>
            <div class="space-y-1 text-sm text-gray-900">
              <a
                v-if="quoteIssueComment"
                class="mr-1 inline-flex flex-row items-center gap-1 rounded bg-slate-100 px-1 py-0.5 text-xs font-medium text-slate-700 hover:bg-slate-200 hover:text-slate-800 hover:underline"
                href="javascript:void(0)"
                @mouseenter="handleShowQuoteReply(true)"
                @mouseleave="handleShowQuoteReply(false)"
              >
                <IconReplyLine />
                <span>{{ quoteIssueComment.contributorVo.displayName }}</span>
              </a>
              <br v-if="quoteIssueComment" />
              <div
                class="prose !max-w-none break-words prose-pre:p-0 tracking-wider"
                v-html="comment?.issueComment.spec.content.html"
              ></div>
            </div>
            <!--            <HasPermission :permissions="['system:comments:manage']">-->
            <div class="flex items-center gap-3 text-xs">
              <span
                class="select-none cursor-pointer text-gray-700 hover:text-gray-900"
              >
                回复
              </span>
            </div>
            <!--            </HasPermission>-->
          </div>
        </template>
      </VEntityField>
    </template>
    <template #end>
      <VEntityField v-if="!comment?.issueComment.spec.approved">
        <template #description>
          <VStatusDot state="warning" animate text="审核" />
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
        v-permission="['plugin:issues:comment:manage']"
        @click="handleApprove(comment.issueComment)"
        >审核</VDropdownItem
      >
      <VDropdownItem
        v-permission="['plugin:issues:comment:manage']"
        @click="handleDeleteComment(comment.issueComment)"
        >删除</VDropdownItem
      >
      <VDropdownDivider />
    </template>
  </VEntity>
</template>
