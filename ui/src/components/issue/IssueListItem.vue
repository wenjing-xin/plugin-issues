<script lang="ts" setup>
import { formatDatetime } from "@/utils/date";
import {
  Dialog,
  VEntityContainer,
  VDropdownItem,
  VEntity,
  VEntityField,
  Toast,
  VDropdownDivider,
  VAvatar,
  VStatusDot,
  IconExternalLinkLine,
  VSpace,
  VModal,
  VButton,
  VAlert,
  IconAddCircle,
  VLoading,
  VEmpty,
} from "@halo-dev/components";
import { computed, inject, provide, type Ref, ref } from "vue";
import { useQueryClient } from "@tanstack/vue-query";
import IssueCommentItem from "@/components/issue/IssueCommentItem.vue";

import type { Issue, ListedIssue, ListedIssueComment } from "@/api/generated";
import { issueApiClient, consoleIssueApiClient } from "@/api";
import { submitForm } from "@formkit/core";
import { useIssueCommentListFetch } from "@/composables/use-consoleIssue";

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

const closedVisibleModal = ref(false);
const closedComment = ref("");
const closing = ref(false);
const showTips = ref(true);
const selectedIssueMessageNames = inject<Ref<string[]>>(
  "selectedIssueMessageNames",
  ref([]),
);
// 展示评论
const showComments = ref(false);

const { issueComments, refetch, isLoading } = useIssueCommentListFetch(
  props.issue.issue.metadata.name,
  showComments,
);
const hoveredReply = ref<ListedIssueComment>();
provide<Ref<ListedIssueComment | undefined>>(
  "hoveredIssueComment",
  hoveredReply,
);

const handleDelete = async (issue: ListedIssue) => {
  Dialog.warning({
    title: "删除issue",
    description: "该操作会将issue和下边的所有评论都删除，该操作不可恢复。",
    confirmType: "danger",
    confirmText: "确定",
    cancelText: "取消",
    onConfirm: async () => {
      try {
        await consoleIssueApiClient.issue.deleteIssue({
          name: issue.issue.metadata.name,
        });
        Toast.success("删除成功");
      } catch (error) {
        console.error("Failed to delete issue", error);
      } finally {
        await queryClient.invalidateQueries({ queryKey: ["issues"] });
      }
    },
  });
};
const issueStatus = computed(() => {
  const { status } = props.issue.issue;
  return status?.state === "AWAIT"
    ? "待处理"
    : status?.state == "PROGRESS"
      ? "进行中"
      : "已关闭";
});

//审核 issue
const handlerAuditIssue = async (issue: Issue) => {
  // 审核逻辑
  await issueApiClient.issue.patchIssue({
    name: issue.metadata.name,
    jsonPatchInner: [
      {
        op: "add",
        path: "/spec/approved",
        value: true,
      },
    ],
  });
  Toast.success("审核成功");
  await queryClient.invalidateQueries({ queryKey: ["issues"] });
};

// 编辑issue
const handlerEditIssue = (issue: ListedIssue) => {
  emit("update", issue.issue);
};
const onSubmitClose = async () => {
  try {
    closing.value = true;
    await consoleIssueApiClient.issue.updateIssueStatus({
      issueStatusChangeParam: {
        issueName: props.issue.issue.metadata.name,
        issueState: "CLOSED",
        changeComment: closedComment.value,
      },
    });
    Toast.success("关闭Issue成功");
  } catch (error) {
    console.error("Failed to end issue", error);
  } finally {
    await queryClient.invalidateQueries({ queryKey: ["issues"] });
    closedComment.value = "";
    showTips.value = true;
    closedVisibleModal.value = false;
    closing.value = false;
  }
};

const reopenCurIssue = async (issueName: string) => {
  await consoleIssueApiClient.issue.updateIssueStatus({
    issueStatusChangeParam: {
      issueName: issueName,
      issueState: "PROGRESS",
      changeComment: "重新打开Issue",
    },
  });

  await queryClient.invalidateQueries({ queryKey: ["issues"] });
  Toast.success("操作成功");
};

function handleRouteToUserDetail() {}
</script>
<template>
  <VModal title="关闭Issue" :visible="closedVisibleModal" :width="420">
    <template #actions>
      <slot name="append-actions" />
    </template>
    <div class="space-y-3">
      <VAlert
        v-if="showTips"
        type="info"
        title="提示"
        :description="
          '确认关闭此Issue：' + issue.issue.spec.title + '，关闭后可重新打开'
        "
        @close="showTips = false"
      />
      <FormKit v-model.trim="closedComment" type="text" label="关闭原因" />
    </div>
    <template #footer>
      <VSpace>
        <VButton :loading="closing" type="secondary" @click="onSubmitClose()">
          提交
        </VButton>
        <VButton
          @click="
            closedVisibleModal = false;
            showTips = true;
          "
        >
          取消
        </VButton>
      </VSpace>
    </template>
  </VModal>
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
              <span class="text-xs text-gray-500">
                点赞：{{ issue.issueStats.upvote }}
              </span>
              <span class="text-xs text-gray-500"
                >总评论数：{{ issue.issueStats.totalIssueComment }}</span
              >
              <span
                v-if="
                  issue.issueStats.awaitApproveIssueComment &&
                  issue.issueStats.awaitApproveIssueComment > 0
                "
                class="text-xs text-gray-500"
                @click="showComments = true"
              >
                <VStatusDot
                  v-bind="{
                    state: 'warning',
                    text:
                      '待审核评论：' +
                      issue.issueStats?.awaitApproveIssueComment,
                    animate: true,
                  }"
                />
              </span>
              <span
                v-if="showComments"
                class="hover:cursor-pointer"
                @click="showComments = false"
              >
                <VStatusDot
                  v-bind="{ state: 'success', text: '关闭详情', animate: true }"
                />
              </span>
              <span
                v-if="!showComments"
                class="hover:cursor-pointer text-xs text-gray-500"
                @click="showComments = !showComments"
              >
                查看详情
              </span>
            </VSpace>
          </div>
        </template>
      </VEntityField>
    </template>
    <template #end>
      <VEntityField>
        <template #description>
          <VStatusDot
            v-if="issue.issue.status?.state == 'AWAIT'"
            state="warning"
            animate
          >
            <template #text>
              <p class="text-xs">{{ issueStatus }}</p>
            </template>
          </VStatusDot>
          <VStatusDot
            v-else-if="issue.issue.status?.state == 'PROGRESS'"
            state="default"
            animate
          >
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
      <VEntityField v-if="!issue.issue.spec.approved">
        <template #description>
          <VStatusDot
            v-tooltip="`等待审核`"
            state="warning"
            animate
            text="等待审核"
          />
        </template>
      </VEntityField>
      <VEntityField v-if="issue.issue.metadata.deletionTimestamp">
        <template #description>
          <VStatusDot v-tooltip="`删除中`" state="warning" animate />
        </template>
      </VEntityField>
      <VEntityField v-if="issue.issue.metadata.creationTimestamp">
        <template #description>
          <span class="truncate text-xs text-gray-500 tabular-nums">{{
            formatDatetime(issue.issue.metadata.creationTimestamp)
          }}</span>
        </template>
      </VEntityField>
    </template>
    <template #dropdownItems>
      <VDropdownItem
        v-if="!issue.issue.spec.approved"
        @click="handlerAuditIssue(issue.issue)"
      >
        审核
      </VDropdownItem>
      <VDropdownItem @click="handlerEditIssue(issue)"> 编辑 </VDropdownItem>
      <HasPermission :permissions="['plugin:issues:manage']">
        <VDropdownItem
          v-if="issue.issue.status?.state != 'CLOSED'"
          @click="closedVisibleModal = true"
        >
          关闭
        </VDropdownItem>
        <VDropdownItem
          v-if="issue.issue.status?.state == 'CLOSED'"
          @click="reopenCurIssue(issue.issue.metadata.name)"
        >
          重新打开
        </VDropdownItem>
        <VDropdownItem
          v-if="issue.issue.status?.state == 'AWAIT'"
          v-tooltip="'设置当前Issue状态为处理中'"
          @click="reopenCurIssue(issue.issue.metadata.name)"
        >
          Issue处理中
        </VDropdownItem>
        <VDropdownDivider />
        <VDropdownItem type="danger" @click="handleDelete(issue)">
          删除
        </VDropdownItem>
      </HasPermission>
    </template>
    <!-- issue评论回复 -->
    <template v-if="showComments" #footer>
      <div class="pl-8">
        <VLoading v-if="isLoading" />
        <Transition v-else-if="!issueComments?.length" appear name="fade">
          <VEmpty
            message="你可以尝试刷新或者创建新的Issue评论"
            title="此Issue下暂无评论"
          >
            <template #actions>
              <VSpace>
                <VButton size="sm" @click="refetch()"> 刷新 </VButton>
                <VButton type="secondary" size="sm">
                  <template #icon>
                    <IconAddCircle class="h-full w-full" />
                  </template>
                  创建新的Issue评论
                </VButton>
              </VSpace>
            </template>
          </VEmpty>
        </Transition>
        <Transition v-else appear name="fade">
          <VEntityContainer>
            <IssueCommentItem
              v-for="comment in issueComments"
              :key="comment.issueComment.metadata.name"
              :comment="comment"
              :comments="issueComments"
              @update-issue-comments="refetch()"
            ></IssueCommentItem>
          </VEntityContainer>
        </Transition>
      </div>
    </template>
  </VEntity>
</template>
