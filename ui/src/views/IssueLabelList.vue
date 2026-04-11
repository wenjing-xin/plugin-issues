<script lang="ts" setup>
import PepiconsPrintLabelCircle from '~icons/pepicons-print/label-circle';
import IssueLabelListItem from "../components/issue/IssueLabelListItem.vue";
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
  VEntityContainer,
} from "@halo-dev/components";
import { useRouteQuery } from "@vueuse/router";
import { computed, provide, type Ref, ref, onMounted, watch, nextTick } from "vue";
import "vue-datepicker-next/index.css";
import "vue-datepicker-next/locale/zh-cn.es";
import { useIssueLabels } from "@/composables/use-issueLabels";
import type {
  IssueLabel, IssueLabelSpecScopeEnum,
  IssueSubject,
  IssueSubjectSpecSubjectTypeEnum,
  ListedIssueLabel
} from "@/api/generated";
import { issueLabelApiClient, issueSubjectApiClient } from "@/api";
import IssueLabelEditModal from "@/components/issue/IssueLabelEditModal.vue";
import { labelScopeTypeOptions, subjectTypeOptions } from "@/dictionary";

const selectedSubjectName = useRouteQuery<string | undefined>("subjectName");
const selectedSort = useRouteQuery<string | undefined>("sort");
const selectedLabelScope = useRouteQuery<IssueLabelSpecScopeEnum | undefined>("labelScope");
const selectedSubjectType = useRouteQuery<IssueSubjectSpecSubjectTypeEnum | undefined>("subjectType");

const hasFilters = computed(() => {
  return (
    selectedSubjectName.value || selectedLabelScope.value || selectedSort.value || selectedSubjectType.value
  );
});

const handleClearFilters = () => {
  selectedSubjectName.value = undefined;
  selectedLabelScope.value = undefined;
  selectedSort.value = undefined;
  selectedSubjectType.value = undefined;
};

const subjectOptions = ref<Array<{ label: string | undefined; value: string }>>(
  [],
);

const handlerIssueSubjectOptions = () => {
  issueSubjectApiClient.issueSubject.listIssueSubject().then(({ data }) => {
    data.items.forEach((it: IssueSubject) => {
      const itemOption = {
        label: it.spec.displayName,
        value: it.metadata.name,
      };
      subjectOptions.value.push(itemOption);
    });
  });
};
const selectedIssueLabel = ref<IssueLabel>();
const checkedAll = ref(false);
const selectedIssueLabelNames = ref<string[]>([]);
provide<Ref<string[]>>("selectedIssueLabelNames", selectedIssueLabelNames);

const editingModal = ref(false);

const page = ref(1);
const size = ref(20);
const keyword = ref("");

const { issueLabels, isLoading, isFetching, refetch, total } = useIssueLabels(
  page,
  size,
  keyword,
  selectedSort,
  selectedSubjectName,
  selectedLabelScope,
  selectedSubjectType
);

const handlerNewIssue = () => {
  editingModal.value = true;
};

const handleCheckAllChange = (e: Event) => {
  const { checked } = e.target as HTMLInputElement;
  if (checked) {
    selectedIssueLabelNames.value =
      issueLabels.value?.map((listedIssueLabel: ListedIssueLabel) => {
        return listedIssueLabel.issueLabel.metadata.name;
      }) || [];
  } else {
    selectedIssueLabelNames.value = [];
  }
};

const onEditingModalClose = async () => {
  selectedIssueLabel.value = undefined;
  editingModal.value = false;
  await refetch();
};
const checkSelection = (listedIssueLabel: ListedIssueLabel) => {
  if (listedIssueLabel.issueLabel.metadata.name) {
    return selectedIssueLabelNames.value.includes(
      listedIssueLabel.issueLabel.metadata.name,
    );
  }
  return false;
};

watch(
  () => selectedIssueLabelNames.value,
  (newValue) => {
    checkedAll.value = newValue.length === issueLabels.value?.length;
  },
);
const handleDeleteInBatch = async () => {
  Dialog.warning({
    title: "删除所选issu标签",
    description: "删除所选issue标签",
    confirmType: "danger",
    confirmText: "确定",
    cancelText: "取消",
    onConfirm: async () => {
      try {
        const promises = selectedIssueLabelNames.value.map((name: string) => {
          return issueLabelApiClient.issueLabel.deleteIssueLabel({
            name: name,
          });
        });
        await Promise.all(promises);
        selectedIssueLabelNames.value = [];
        Toast.success("删除成功");
      } catch (e) {
        console.error("Failed to delete issue label in batch", e);
      } finally {
        refetch();
      }
    },
  });
};

const handlerUpdateIssueLabel = (issueLabel: IssueLabel) => {
  nextTick(()=> {
    selectedIssueLabel.value = issueLabel;
  })
  editingModal.value = true;
};
const emitUpdateIssueLabel = () => {
  selectedIssueLabel.value = undefined;
  refetch();
};

onMounted(() => {
  handlerIssueSubjectOptions();
});
</script>

<template>
  <IssueLabelEditModal
    :issue-label="selectedIssueLabel"
    :visible="editingModal"
    @save="refetch()"
    @update="emitUpdateIssueLabel"
    @close="onEditingModalClose"
  />
  <VPageHeader title="Issue标签管理">
    <template #icon>
      <PepiconsPrintLabelCircle class="mr-2 self-center" />
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
  <div class=":uno: m-0 md:m-4">
    <div class=":uno: flex flex-col gap-2 sm:flex-row">
      <div class="w-full">
        <VCard :body-class="['!p-0']">
          <template #header>
            <div class="block w-full divide-y">
              <div
                class=":uno: relative flex flex-col flex-wrap items-start gap-4 bg-gray-50 px-4 py-3 sm:flex-row sm:items-center"
              >
                <div class=":uno: mr-1 hidden items-center sm:flex">
                  <input
                    v-model="checkedAll"
                    type="checkbox"
                    @change="handleCheckAllChange"
                  />
                </div>
                <div class=":uno: w-full flex flex-1 sm:w-auto">
                  <VSpace v-if="selectedIssueLabelNames.length > 0">
                    <VButton type="danger" @click="handleDeleteInBatch">
                      删除
                    </VButton>
                  </VSpace>
                  <SearchInput v-else v-model="keyword" />
                </div>
                <div class=":uno: w-auto sm:w-auto">
                  <VSpace spacing="sm" class="flex flex-wrap">
                    <FilterCleanButton
                      v-if="hasFilters"
                      @click="handleClearFilters"
                    />
                    <FilterDropdown
                      v-model="selectedLabelScope"
                      label="标签作用范围"
                      :items="labelScopeTypeOptions"
                    />
                    <FilterDropdown
                      v-model="selectedSubjectType"
                      :items="subjectTypeOptions"
                      label="依托主体类型"
                    />
                    <FilterDropdown
                      v-model="selectedSubjectName"
                      :items="subjectOptions"
                      label="依托主体"
                    />
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
                    <div class="flex flex-row gap-2">
                      <div
                        class="group cursor-pointer rounded p-1 hover:bg-gray-200"
                        @click="refetch()"
                      >
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
          <Transition v-else-if="!issueLabels?.length" appear name="fade">
            <VEmpty
              message="你可以尝试刷新或者新建issue标签"
              title="当前没有任何Issue标签"
            >
              <template #actions>
                <VSpace>
                  <VButton @click="refetch"> 刷新</VButton>
                  <VButton
                    v-permission="['plugin:issueSubject:manage']"
                    type="primary"
                    @click="handlerNewIssue"
                  >
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
            <VEntityContainer>
              <IssueLabelListItem
                v-for="listedIssueLabel in issueLabels"
                :key="listedIssueLabel.issueLabel.metadata.name"
                :issue-label="listedIssueLabel"
                :is-selected="checkSelection(listedIssueLabel)"
                @update="handlerUpdateIssueLabel"
              />
            </VEntityContainer>
          </Transition>
          <template #footer>
            <VPagination
              v-model:page="page"
              v-model:size="size"
              :total="total"
              :size-options="[20, 30, 50, 100]"
            />
          </template>
        </VCard>
      </div>
    </div>
  </div>
</template>
