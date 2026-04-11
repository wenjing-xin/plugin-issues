<script lang="ts" setup>
import MaterialSymbolsTagRounded from "~icons/material-symbols/tag-rounded";
import MdiProgressClock from "~icons/mdi/progress-clock";
import TdesignCollection from "~icons/tdesign/collection";
import IxProject from "~icons/ix/project";
import CarbonProduct from "~icons/carbon/product";
import IconParkOutlineTopicDiscussion from "~icons/icon-park-outline/topic-discussion";
import EpMessage from "~icons/ep/message";
import SolarLockOutline from "~icons/solar/lock-outline";
import MaterialSymbolsLightPublic from "~icons/material-symbols-light/public";

import {
  type FunctionalComponent,
  inject,
  type Ref,
  ref,
  type SVGAttributes,
} from "vue";
import MaterialSymbolsLightPostAdd from "~icons/material-symbols-light/post-add";
import { formatDatetime } from "@/utils/date";
import type {
  IssueSubjectSpecSubjectTypeEnum,
  ListedIssueSubject,
} from "@/api/generated";
import {
  Dialog,
  Toast,
  VAvatarGroup,
  VAvatar,
  VStatusDot,
  VSpace,
  VDropdown,
  VDropdownItem,
  VTag,
  IconExternalLinkLine,
  VDropdownDivider,
} from "@halo-dev/components";
import { issueSubjectApiClient } from "@/api";
import { useQueryClient } from "@tanstack/vue-query";
const queryClient = useQueryClient();
import BiThreeDots from "~icons/bi/three-dots";

defineProps<{
  listedIssueSubject: ListedIssueSubject;
  isSelected: boolean;
}>();
const emit = defineEmits<{
  (event: "update", value: ListedIssueSubject): void;
  (event: "updateSelected", value: string): void;
}>();
const selectedIssueSubjectNames = inject<Ref<string[]>>(
  "selectedIssueSubjectNames",
  ref([]),
);

const handleEdit = (issueSubject: ListedIssueSubject) => {
  emit("update", issueSubject);
};

const handleDelete = (issueSubject: ListedIssueSubject) => {
  Dialog.warning({
    title: "删除issue依托主体",
    description:
      "该操作会将issue依托主体删除，且其下边所关联的issue都会清空，该操作不可恢复。",
    confirmType: "danger",
    confirmText: "确定",
    cancelText: "取消",
    onConfirm: async () => {
      try {
        await issueSubjectApiClient.issueSubject.deleteIssueSubject({
          name: issueSubject.issueSubject.metadata.name,
        });
        Toast.success("删除成功");
      } catch (error) {
        console.error("Failed to delete issueSubject", error);
      } finally {
        await queryClient.invalidateQueries({ queryKey: ["issueSubjects"] });
      }
    },
  });
};
const copySubjectLink = (subjectName: string) => {
  navigator.clipboard
    .writeText(window.location.origin + "/subject/" + subjectName)
    .then(() => {
      Toast.success("复制成功");
    })
    .catch((error) => {
      Toast.error("复制失败：" + error);
    });
};
const handlerIssueSubjectType = (
  subjectType: IssueSubjectSpecSubjectTypeEnum,
): { name: string; icon: FunctionalComponent<SVGAttributes> } => {
  switch (subjectType) {
    case "POST":
      return { name: "文章", icon: MaterialSymbolsLightPostAdd };
    case "PROJECT":
      return { name: "项目", icon: IxProject };
    case "PRODUCT":
      return { name: "产品", icon: CarbonProduct };
    case "TOPIC":
      return { name: "话题", icon: IconParkOutlineTopicDiscussion };
    case "LEAVE_MESSAGE":
      return { name: "留言", icon: EpMessage };
  }
};
</script>
<template>
  <div
    class=":uno: relative space-y-3 border border-neutral-100 rounded-md p-4 my-2 bg-white transition-shadow duration-300"
    :class="{ 'border border-neutral-300': isSelected }"
  >
    <div class=":uno: flex justify-between items-center">
      <div class=":uno: w-full flex items-center">
        <VSpace>
          <VAvatar
            size="xs"
            :src="listedIssueSubject.issueSubject.spec.subjectIcon"
            :alt="listedIssueSubject.issueSubject.spec.displayName"
          />
          <RouterLink
            :to="{
              name: 'Issue',
              query: {
                subjectName: listedIssueSubject.issueSubject.metadata.name,
              },
            }"
            v-tooltip="`查看改依托主体下的Issue`"
            class=":uno: flex items-center"
          >
            <p
              class=":uno: text-sm font-bold text-neutral-700 hover:cursor-pointer hover:text-neutral-500 transition-all duration-300"
            >
              {{ listedIssueSubject.issueSubject.spec.displayName }}
            </p>
          </RouterLink>
          <a
            v-tooltip="'点击前往主题端访问'"
            target="_blank"
            :href="'/subject/' + listedIssueSubject.issueSubject.metadata.name"
            class=":uno: text-gray-600 transition-all hover:text-gray-900 group-hover:inline-block"
          >
            <IconExternalLinkLine class="h-3.5 w-3.5" />
          </a>
          <VTag theme="default" class="cursor-auto">
            {{
              handlerIssueSubjectType(
                listedIssueSubject.issueSubject.spec.subjectType,
              ).name
            }}
            <template #leftIcon
              ><component
                :is="
                  handlerIssueSubjectType(
                    listedIssueSubject.issueSubject.spec.subjectType,
                  ).icon
                "
            /></template>
          </VTag>
          <VTag
            v-if="
              listedIssueSubject.issueSubject.spec.subjectVisible == 'PRIVATE'
            "
            theme="default"
            class="cursor-auto"
          >
            <template #leftIcon>
              <SolarLockOutline />
            </template>
            私密
          </VTag>
          <VTag
            v-if="
              listedIssueSubject.issueSubject.spec.subjectVisible == 'PUBLIC'
            "
            theme="default"
            class="cursor-auto"
          >
            <template #leftIcon>
              <MaterialSymbolsLightPublic />
            </template>
            公开
          </VTag>
        </VSpace>
      </div>
      <HasPermission :permissions="['plugin:issueSubject:manage']">
        <input
          v-model="selectedIssueSubjectNames"
          :value="listedIssueSubject.issueSubject.metadata.name"
          name="issueSubject-checkbox"
          type="checkbox"
        />
      </HasPermission>
    </div>
    <div class=":uno: p-2 bg-neutral-50 rounded-md h-12 flex items-center">
      <p
        v-if="listedIssueSubject.issueSubject.spec?.description"
        class=":uno: text-sm line-clamp-2"
      >
        {{ listedIssueSubject.issueSubject.spec?.description }}
      </p>
      <p v-else class=":uno: text-sm text-gray-500">暂无描述</p>
    </div>
    <!--  统计  -->
    <div class=":uno: flex flex-wrap gap-2 py-1">
      <div
        class=":uno: bg-gray-100 rounded-md px-3 py-1 text-sm font-medium flex items-center"
      >
        <TdesignCollection class="mr-1.5" />
        <span class="mr-1 text-sm">Total</span>
        <span class="text-sm">{{
          listedIssueSubject.issueSubjectStats.totalIssue
        }}</span>
      </div>
      <div
        class=":uno: bg-blue-100 text-blue-800 rounded-md px-3 py-1 text-sm font-medium flex items-center"
      >
        <MdiProgressClock class="mr-1.5" />
        <span class="mr-1 text-sm">进行中</span>
        <span class="text-sm">{{
          listedIssueSubject.issueSubjectStats.progressIssue
        }}</span>
      </div>
      <div
        class=":uno: bg-yellow-100 text-yellow-800 rounded-md px-3 py-1 text-sm font-medium flex items-center"
      >
        <VStatusDot state="warning" animate>
          <template #text>
            <span class="mr-1 text-sm">待处理</span>
            <span class="text-sm">{{
              listedIssueSubject.issueSubjectStats.awaitIssue
            }}</span>
          </template>
        </VStatusDot>
      </div>
      <div
        class=":uno: bg-green-100 text-green-800 rounded-md px-3 py-1 text-sm font-medium flex items-center"
      >
        <VStatusDot state="success">
          <template #text>
            <span class="mr-1 text-sm">已关闭</span>
            <span class="text-sm">{{
              listedIssueSubject.issueSubjectStats.closedIssue
            }}</span>
          </template>
        </VStatusDot>
      </div>
      <div
        class=":uno: bg-purple-100 text-purple-800 rounded-md px-3 py-1 text-sm font-medium flex items-center"
      >
        <span class="mr-1 text-sm">待审核</span>
        <span class="text-sm">{{
          listedIssueSubject.issueSubjectStats.awaitApproved
        }}</span>
      </div>
      <div
        class=":uno: bg-gray-100 rounded-md px-3 py-1 text-sm font-medium flex items-center"
      >
        <MaterialSymbolsTagRounded class="mr-1.5" />
        <span class="text-sm">{{
          listedIssueSubject.issueSubjectStats.labels
        }}</span>
        <span class="ml-1 text-sm">个标签</span>
      </div>
    </div>
    <div class=":uno: w-full flex justify-between items-center">
      <div class=":uno: flex items-center gap-x-2 text-xs text-gray-500">
        <VAvatarGroup size="xs" circle>
          <VAvatar
            v-tooltip="'创建者：' + listedIssueSubject.createOwner.displayName"
            :src="listedIssueSubject.createOwner.avatar"
            :alt="listedIssueSubject.createOwner.displayName"
          />
          <VAvatar
            v-for="contributor in listedIssueSubject.participateUsers"
            :key="contributor.name"
            v-tooltip="'参与者：' + contributor.displayName"
            :src="contributor.avatar"
            :alt="contributor.displayName"
          ></VAvatar>
        </VAvatarGroup>
        <span>{{
          formatDatetime(
            listedIssueSubject.issueSubject.metadata.creationTimestamp,
          )
        }}</span>
      </div>
      <span
        class=":uno: inline-flex items-center gap-x-1.5 rounded-base bg-neutral-100 px-2 py-1 text-xs font-medium outline-none bg-neutral-100"
      >
        <VDropdown>
          <BiThreeDots
            v-tooltip="'操作'"
            class=":uno: cursor-pointer text-black outline-none"
          />
          <template #popper>
            <VDropdownItem @click="handleEdit(listedIssueSubject)"
              >编辑</VDropdownItem
            >
            <VDropdownItem @click="handleDelete(listedIssueSubject)"
              >删除</VDropdownItem
            >
            <VDropdownDivider />
            <VDropdownItem
              @click="
                copySubjectLink(listedIssueSubject.issueSubject.metadata.name)
              "
              >复制链接</VDropdownItem
            >
          </template>
        </VDropdown>
      </span>
    </div>
  </div>
</template>
