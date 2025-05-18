<script lang="ts" setup>
import SystemUiconsMessage from "~icons/system-uicons/message";
import IssueTemplateListItem from "../components/issue/IssueTemplateListItem.vue";
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
import { useRouter } from "vue-router";
import UserFilterDropdown from "@/components/common/UserFilterDropdown.vue";
import { useRouteQuery } from "@vueuse/router";
import {computed, provide, type Ref, ref, watch} from "vue";
import type {ListedIssueSubject, ListedIssueTemplate} from "@/api/generated";
import { issueTemplateApiClient } from "@/api";
import { useIssueTemplateListFetch } from "@/composables/use-consoleIssueTemplate";

const router = useRouter();

const ownerName = useRouteQuery<string | undefined>("ownerName");
const selectedSort = useRouteQuery<string | undefined>("sort");

const hasFilters = computed(() => {
  return selectedSort.value || ownerName.value;
});
function handleClearFilters() {
  selectedSort.value = undefined;
  ownerName.value = undefined;
}

const checkedAll = ref(false);
const selectedIssueTemplateNames = ref<string[]>([]);
provide<Ref<string[]>>("selectedIssueTemplateNames", selectedIssueTemplateNames);

const editingModal = ref(false);
const selectedIssueTemplate = ref<ListedIssueTemplate>();

const page = ref(1);
const size = ref(20);
const keyword = ref("");
const { issueTemplates, isLoading, isFetching, refetch, total } = useIssueTemplateListFetch(
  page,
  size,
  keyword,
  selectedSort,
  ownerName
);

const handlerNewIssueTemplate = () => {
  //新建issue模版
  router.push({ name: "IssueTemplateEditor"});
};
const handleCheckAllChange = (e: Event) => {
  const { checked } = e.target as HTMLInputElement;
  if (checked) {
    selectedIssueTemplateNames.value =
      issueTemplates.value?.map((listedIssueTemplate: ListedIssueTemplate) => {
        return listedIssueTemplate.issueTemplate.metadata.name;
      }) || [];
  } else {
    selectedIssueTemplateNames.value = [];
  }
};
watch(
  () => selectedIssueTemplateNames.value,
  (newValue) => {
    checkedAll.value = newValue.length === issueTemplates.value?.length;
  }
);
const onEditingModalClose = async () => {
  selectedIssueTemplate.value = undefined;
  editingModal.value = false;
  await refetch();
};
const checkSelection = (listedIssueTemplate: ListedIssueTemplate) => {
  return (
    listedIssueTemplate.issueTemplate.metadata.name === selectedIssueTemplate.value?.issueTemplate.metadata.name ||
    selectedIssueTemplateNames.value.includes(listedIssueTemplate.issueTemplate.metadata.name)
  );
};

const handleDeleteInBatch = async () => {
  Dialog.warning({
    title: "删除所选模版",
    description: "删除所选模版",
    confirmType: "danger",
    confirmText: "确定",
    cancelText: "取消",
    onConfirm: async () => {
      try {
        const promises = selectedIssueTemplateNames.value.map((name: string) => {
          return issueTemplateApiClient.issueTemplate.deleteIssueTemplate({
            name: name,
          });
        });
        await Promise.all(promises);
        selectedIssueTemplateNames.value = [];
        Toast.success("删除成功");
      } catch (e) {
        console.error("Failed to delete issueMessage in batch", e);
      } finally {
        refetch();
      }
    },
  });
};
</script>

<template>
  <VPageHeader title="Issue留言模版">
    <template #icon>
      <SystemUiconsMessage class="mr-2 self-center" />
    </template>
    <template #actions>
      <VSpace v-permission="['plugin:issueTemplates:manage']">
        <VButton type="primary" size="md" @click="handlerNewIssueTemplate">
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
                  <VButton
                    v-if="selectedIssueTemplateNames.length > 0"
                    type="danger"
                    size="md"
                    @click="handleDeleteInBatch"
                  >
                    删除
                  </VButton>
                  <SearchInput v-else v-model="keyword" />
                </div>
                <div class="w-auto sm:w-auto">
                  <VSpace spacing="sm" class="flex flex-wrap">
                    <FilterCleanButton v-if="hasFilters" @click="handleClearFilters" />
                    <HasPermission :permissions="['system:users:view']">
                      <UserFilterDropdown v-model="ownerName" :label="'创建者'" />
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
          <Transition v-else-if="!issueTemplates?.length" appear name="fade">
            <VEmpty message="你可以尝试刷新或者新建issue留言模版" title="当前没有任何Issue留言模版">
              <template #actions>
                <VSpace>
                  <VButton @click="refetch"> 刷新</VButton>
                  <VButton
                    v-permission="['plugin:issueTemplates:manage']"
                    type="primary"
                    @click="handlerNewIssueTemplate"
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
            <ul class="box-border h-auto w-full divide-y divide-gray-100" role="list">
              <li v-for="listedIssueTemplate in issueTemplates" :key="listedIssueTemplate.issueTemplate.metadata.name">
                <IssueTemplateListItem :issue-template="listedIssueTemplate" :is-selected="checkSelection(listedIssueTemplate)" />
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
