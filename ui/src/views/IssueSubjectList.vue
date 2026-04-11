<script lang="ts" setup>
import PajamasIssueTypeObjective from "~icons/pajamas/issue-type-objective";
import IssueSubjectItem from "../components/issue/IssueSubjectItem.vue";
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
import { useRouteQuery } from "@vueuse/router";
import { computed, provide, type Ref, ref, onMounted, watch } from "vue";
import "vue-datepicker-next/index.css";
import "vue-datepicker-next/locale/zh-cn.es";
import { useIssueSubjectListFetch } from "@/composables/use-consoleIssue";
import type {
  IssueSubject,
  IssueSubjectSpecSubjectTypeEnum,
  IssueSubjectSpecSubjectVisibleEnum,
  ListedIssueSubject,
} from "@/api/generated";
import { issueSubjectApiClient, issueTemplateApiClient } from "@/api";
import IssueSubjectEditModal from "@/components/issue/IssueSubjectEditModal.vue";
import { subjectTypeOptions } from "@/dictionary";

const ownerName = useRouteQuery<string | undefined>("ownerName");
const selectedSubjectType = useRouteQuery<
  IssueSubjectSpecSubjectTypeEnum | undefined
>("subjectType");
const selectedSort = useRouteQuery<string | undefined>("sort");
const selectedSubjectVisible = useRouteQuery<
  IssueSubjectSpecSubjectVisibleEnum | undefined
>("subjectVisible");

const hasFilters = computed(() => {
  return (
    ownerName.value ||
    selectedSort.value ||
    selectedSubjectType.value ||
    selectedSubjectVisible.value
  );
});

const handleClearFilters = () => {
  ownerName.value = undefined;
  selectedSort.value = undefined;
  selectedSubjectType.value = undefined;
  selectedSubjectVisible.value = undefined;
};

const checkedAll = ref(false);
const selectedIssueSubjectNames = ref<string[]>([]);
provide<Ref<string[]>>("selectedIssueSubjectNames", selectedIssueSubjectNames);

const issueTemplateFilterOptions = ref<
  Array<{ label: string | undefined; value: string }>
>([]);

const editingModal = ref(false);
const selectedIssueSubject = ref<IssueSubject>();

const page = ref(1);
const size = ref(20);
const keyword = ref("");

const { issueSubjects, isLoading, isFetching, refetch, total } =
  useIssueSubjectListFetch(
    page,
    size,
    keyword,
    selectedSort,
    ownerName,
    selectedSubjectType,
    selectedSubjectVisible,
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

const handleCheckAllChange = (e: Event) => {
  const { checked } = e.target as HTMLInputElement;
  if (checked) {
    selectedIssueSubjectNames.value =
      issueSubjects.value?.map((listedIssueSubject: ListedIssueSubject) => {
        return listedIssueSubject.issueSubject.metadata.name;
      }) || [];
  } else {
    selectedIssueSubjectNames.value = [];
  }
};

const onEditingModalClose = async () => {
  selectedIssueSubject.value = undefined;
  editingModal.value = false;
  await refetch();
};
const checkSelection = (listedIssueSubject: ListedIssueSubject) => {
  if (listedIssueSubject.issueSubject.metadata.name) {
    return selectedIssueSubjectNames.value.includes(
      listedIssueSubject.issueSubject.metadata.name,
    );
  }
  return false;
};

watch(
  () => selectedIssueSubjectNames.value,
  (newValue) => {
    checkedAll.value = newValue.length === issueSubjects.value?.length;
  },
);
const handleDeleteInBatch = async () => {
  Dialog.warning({
    title: "删除所选issuy依托主体",
    description: "删除所选issue依托主体",
    confirmType: "danger",
    confirmText: "确定",
    cancelText: "取消",
    onConfirm: async () => {
      try {
        const promises = selectedIssueSubjectNames.value.map((name: string) => {
          return issueSubjectApiClient.issueSubject.deleteIssueSubject({
            name: name,
          });
        });
        await Promise.all(promises);
        selectedIssueSubjectNames.value = [];
        Toast.success("删除成功");
      } catch (e) {
        console.error("Failed to delete issueMessage in batch", e);
      } finally {
        refetch();
      }
    },
  });
};

const handlerUpdateIssueSubject = (issueSubject: ListedIssueSubject) => {
  selectedIssueSubject.value = issueSubject.issueSubject;
  editingModal.value = true;
};
const emitUpdateIssueSubject = () => {
  selectedIssueSubject.value = undefined;
  refetch();
};

onMounted(() => {
  handlerIssueTemplateOptions();
});
</script>

<template>
  <IssueSubjectEditModal
    :issue-subject="selectedIssueSubject"
    :visible="editingModal"
    @save="refetch()"
    @update="emitUpdateIssueSubject"
    @close="onEditingModalClose"
  />
  <VPageHeader title="Issue依托主体">
    <template #icon>
      <PajamasIssueTypeObjective class="mr-2 self-center" />
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
                  <VSpace v-if="selectedIssueSubjectNames.length > 0">
                    <VButton type="danger" @click="handleDeleteInBatch" v-permission="['plugin:issues:manage']">
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
                      v-model="selectedSubjectVisible"
                      label="可见性"
                      :items="[
                        {
                          label: '默认',
                        },
                        {
                          label: '公共',
                          value: 'PUBLIC',
                        },
                        {
                          label: '私有',
                          value: 'PRIVATE',
                        },
                      ]"
                    />
                    <FilterDropdown
                      v-model="selectedSubjectType"
                      :items="subjectTypeOptions"
                      label="依托主体类型"
                    />
                    <HasPermission :permissions="['system:users:view']">
                      <UserFilterDropdown
                        v-model="ownerName"
                        :label="'发布者'"
                      />
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
          <Transition v-else-if="!issueSubjects?.length" appear name="fade">
            <VEmpty
              message="你可以尝试刷新或者新建issue依托主体"
              title="当前没有任何Issue依托主体"
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
            <ul
              class=":uno: box-border px-3 h-auto w-full divide-y divide-gray-100 grid grid-cols-1 xl:grid-cols-2 2xl:grid-cols-3 gap-3"
              role="list"
            >
              <li
                v-for="listedIssueSubject in issueSubjects"
                :key="listedIssueSubject.issueSubject.metadata.name"
              >
                <IssueSubjectItem
                  :listed-issue-subject="listedIssueSubject"
                  :is-selected="checkSelection(listedIssueSubject)"
                  @update="handlerUpdateIssueSubject"
                />
              </li>
            </ul>
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
