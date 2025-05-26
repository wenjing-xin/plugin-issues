<script lang="ts" setup>
import SystemUiconsMessage from "~icons/system-uicons/message";
import IssueListItem from "../components/issue/IssueListItem.vue";
import {
  IconAddCircle,
  VButton,
  VCard,
  VEmpty,
  VLoading,
  VPageHeader,
  VPagination,
  VSpace,
  IconRefreshLine,
  Dialog,
  Toast,
} from "@halo-dev/components";
import UserFilterDropdown from "@/components/common/UserFilterDropdown.vue";
import { toISODayEndOfTime } from "@/utils/date";
import { useRouteQuery } from "@vueuse/router";
import { computed, provide, type Ref, ref, onMounted } from "vue";
import DatePicker from "vue-datepicker-next";
import "vue-datepicker-next/index.css";
import "vue-datepicker-next/locale/zh-cn.es";
import {useIssueListFetch} from "@/composables/use-consoleIssue";
import IssueEditModal from "@/components/issue/IssueEditModal.vue";
import type { Issue, ListedIssue } from "@/api/generated";
import LabelFilterDropdown from "../components/issue/LabelFilterDropdown.vue";
import { issueApiClient, issueTemplateApiClient } from "@/api";

const label = useRouteQuery<string | undefined>("label");
const ownerName = useRouteQuery<string | undefined>("ownerName");
const selectedTemplate = useRouteQuery<string | undefined>("issueTemplate");
const selectedSort = useRouteQuery<string | undefined>("sort");
const selectedApprovedStatus = useRouteQuery<string | undefined, boolean | undefined>("approved", undefined, {
  transform: (value) => {
    return value ? value === "true" : undefined;
  },
});

const currentIssueSubjectName = useRouteQuery<string>("subjectName");

const hasFilters = computed(() => {
  return (
    selectedApprovedStatus.value == true ||
    selectedApprovedStatus.value == false ||
    label.value ||
    ownerName.value ||
    selectedSort.value ||
    selectedTemplate.value
  );
});
function handleClearFilters() {
  selectedApprovedStatus.value = undefined;
  label.value = undefined;
  ownerName.value = undefined;
  selectedSort.value = undefined;
  selectedTemplate.value = undefined;
}

const updateIssueMessage = ref<Issue>();
const checkedAll = ref(false);
const selectedIssueMessageNames = ref<string[]>([]);
provide<Ref<string[]>>("selectedIssueMessageNames", selectedIssueMessageNames);

const issueTemplateFilterOptions = ref<Array<{ label: string | undefined; value: string }>>([]);

const editingModal = ref(false);
const selectedIssueMessage = ref<Issue>();

const page = ref(1);
const size = ref(20);
const keyword = ref("");
const issueMessagesRangeTime = ref<Array<Date>>([]);
const startDate = computed(() => {
  const date: Date = issueMessagesRangeTime.value[0];
  return toISODayEndOfTime(date);
});

const endDate = computed(() => {
  const endTime: Date = issueMessagesRangeTime.value[1];
  return toISODayEndOfTime(endTime);
});

const { issues, isLoading, isFetching, refetch, total } = useIssueListFetch(
  page,
  size,
  currentIssueSubjectName,
  keyword,
  selectedSort,
  ownerName,
  selectedApprovedStatus,
  startDate,
  endDate,
  label,
  selectedTemplate
);

const handlerNewIssue = () => {
  editingModal.value = true;
};

//处理issue template的筛选过滤条件
const handlerIssueTemplateOptions = () => {
  issueTemplateApiClient.issueTemplate.listIssueTemplate().then(({ data }) => {
    data.items.forEach((it) => {
      const itemOption = { label: it.spec?.name, value: it.metadata.name };
      issueTemplateFilterOptions.value.push(itemOption);
    });
  });
};

const releaseCurIssue = () => {};

const cancelReleaseCurIssue = () => {};

const handleCheckAllChange = (e: Event) => {
  const { checked } = e.target as HTMLInputElement;
  checkedAll.value = checked;
  if (checkedAll.value) {
    selectedIssueMessageNames.value =
      issues.value?.map((listedIssueMessage: ListedIssue) => {
        return listedIssueMessage.issue.metadata.name;
      }) || [];
  } else {
    selectedIssueMessageNames.value.length = 0;
  }
  console.log(selectedIssueMessageNames, "454564564");
};

const onEditingModalClose = async () => {
  selectedIssueMessage.value = undefined;
  editingModal.value = false;
  await refetch();
};
const checkSelection = (listedIssue: ListedIssue) => {
  if (listedIssue.issue.metadata.name) {
    return selectedIssueMessageNames.value.includes(listedIssue.issue.metadata.name);
  }
  return false;
};
const handleEndIssueInBatch = async () => {
  Dialog.warning({
    title: "关闭所选issue",
    description: "关闭所选issue",
    confirmType: "primary",
    confirmText: "确定",
    cancelText: "取消",
    onConfirm: async () => {
      try {
        const promises = selectedIssueMessageNames.value.map((name: string) => {
          return issueApiClient.issue.patchIssue({
            name: name,
            jsonPatchInner: [
              {
                op: "add",
                path: "/status/state",
                value: "CLOSED",
              },
            ],
          });
        });
        await Promise.all(promises);
        selectedIssueMessageNames.value = [];
        Toast.success("关闭issue成功");
      } catch (e) {
        console.error("Failed to end issue in batch", e);
      } finally {
        refetch();
      }
    },
  });
};

const handleDeleteInBatch = async () => {
  Dialog.warning({
    title: "删除所选issue",
    description: "删除所选issue",
    confirmType: "danger",
    confirmText: "确定",
    cancelText: "取消",
    onConfirm: async () => {
      try {
        const promises = selectedIssueMessageNames.value.map((name: string) => {
          return issueApiClient.issue.deleteIssue({
            name: name,
          });
        });
        await Promise.all(promises);
        selectedIssueMessageNames.value = [];
        Toast.success("删除成功");
      } catch (e) {
        console.error("Failed to delete issueMessage in batch", e);
      } finally {
        refetch();
      }
    },
  });
};

//更新issue
const handlerUpdateIssue = (issue: Issue) => {
  updateIssueMessage.value = issue;
  editingModal.value = true;
};
// 更新issue
const handlerUpdateIssueMessage  = ()=>{
  updateIssueMessage.value = undefined;
  refetch();
}
onMounted(() => {
  handlerIssueTemplateOptions();
});
</script>

<template>
  <IssueEditModal
    :visible="editingModal"
    :issue-message="updateIssueMessage"
    @save="refetch()"
    @update="handlerUpdateIssueMessage"
    @close="onEditingModalClose"
  />
  <VPageHeader title="全部 Issue">
    <template #icon>
      <SystemUiconsMessage class="mr-2 self-center" />
    </template>
    <template #actions>
      <VSpace v-permission="['plugin:issues:manage']">
        <VButton type="secondary" size="md" @click="handlerNewIssue">
          <template #icon>
            <IconAddCircle class="h-full w-full" />
          </template>
          新建
        </VButton>
      </VSpace>
    </template>
  </VPageHeader>
  <div class="m-0 md:m-4">
    <div class="flex flex-col gap-2 sm:flex-row">
      <div class="w-full">
        <VCard :body-class="['!p-0']">
          <template #header>
            <div class="block w-full divide-y">
              <div
                class="relative flex flex-col flex-wrap items-start gap-4 bg-gray-50 px-4 py-3 sm:flex-row sm:items-center"
              >
                <div class="mr-1 hidden items-center sm:flex">
                  <input v-model="checkedAll" type="checkbox" @change="handleCheckAllChange" />
                </div>
                <div class="w-full flex flex-1 sm:w-auto">
                  <VSpace v-if="selectedIssueMessageNames.length > 0">
                    <VButton size="sm" type="primary" @click="handleEndIssueInBatch"> 关闭Issue </VButton>
                    <VButton type="danger" size="sm" @click="handleDeleteInBatch"> 删除 </VButton>
                  </VSpace>
                  <SearchInput v-else v-model="keyword" />
                </div>
                <div class="w-auto sm:w-auto">
                  <VSpace spacing="sm" class="flex flex-wrap">
                    <FilterCleanButton v-if="hasFilters" @click="handleClearFilters" />
                    <FilterDropdown v-model="selectedTemplate" label="模版" :items="issueTemplateFilterOptions" />
                    <LabelFilterDropdown v-model="label" :label="'标签'" />
                    <FilterDropdown
                      v-model="selectedApprovedStatus"
                      label="状态"
                      :items="[
                        {
                          label: '全部',
                        },
                        {
                          label: '已审核',
                          value: true,
                        },
                        {
                          label: '待审核',
                          value: false,
                        },
                      ]"
                    />
                    <HasPermission :permissions="['system:users:view']">
                      <UserFilterDropdown v-model="ownerName" :label="'发布者'" />
                    </HasPermission>
                    <FilterDropdown
                      v-model="selectedSort"
                      label="排序"
                      :items="[
                        {
                          label: '默认',
                        },
                        {
                          label: '较近创建',
                          value: 'metadata.creationTimestamp,desc',
                        },
                        {
                          label: '较早创建',
                          value: 'metadata.creationTimestamp,asc',
                        },
                      ]"
                    />
                    <div class="right-0 flex !ml-0">
                      <DatePicker
                        v-model:value="issueMessagesRangeTime"
                        input-class="mx-input rounded text-sm"
                        class="max-w-[13rem] cursor-pointer md:max-w-[15rem]"
                        range
                        :editable="false"
                        placeholder="筛选日期范围"
                      />
                    </div>
                    <div class="flex flex-row gap-2">
                      <div class="group cursor-pointer rounded p-1 hover:bg-gray-200" @click="refetch()">
                        <IconRefreshLine
                          v-tooltip="'刷新'"
                          :class="{ 'animate-spin text-gray-900': isFetching }"
                          class="h-4 w-4 text-gray-600 group-hover:text-gray-900"
                        />
                      </div>
                    </div>
                  </VSpace>
                </div>
              </div>
            </div>
          </template>
          <VLoading v-if="isLoading" />
          <Transition v-else-if="!issues?.length" appear name="fade">
            <VEmpty message="你可以尝试刷新或者新建issue" title="当前没有任何Issue">
              <template #actions>
                <VSpace>
                  <VButton @click="refetch"> 刷新</VButton>
                  <VButton v-permission="['plugin:pasteContents:manage']" type="primary" @click="handlerNewIssue">
                    <template #icon>
                      <IconAddCircle class="size-full" />
                    </template>
                    新建
                  </VButton>
                </VSpace>
              </template>
            </VEmpty>
          </Transition>
          <Transition v-else appear name="fade">
            <ul class="box-border h-auto w-full divide-y divide-gray-100" role="list">
              <li v-for="listedIssue in issues" :key="listedIssue.issue.metadata.name">
                <IssueListItem
                  :issue="listedIssue"
                  :is-selected="checkSelection(listedIssue)"
                  @update="handlerUpdateIssue"
                />
              </li>
            </ul>
          </Transition>
          <template #footer>
            <VPagination v-model:page="page" v-model:size="size" :total="total" :size-options="[20, 30, 50, 100]" />
          </template>
        </VCard>
      </div>
    </div>
  </div>
</template>
