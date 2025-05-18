<script lang="ts" setup>
import {computed, type FunctionalComponent, inject, type Ref, ref, type SVGAttributes} from 'vue';
import MaterialSymbolsLightPostAdd from '~icons/material-symbols-light/post-add';
    import { formatDatetime } from '@/utils/date';
    import type {IssueSubjectSpecSubjectTypeEnum, ListedIssueSubject} from '@/api/generated';
    import {Dialog, Toast, VAvatar} from "@halo-dev/components";
    import { issueSubjectApiClient} from "@/api";
    import { useQueryClient } from "@tanstack/vue-query";
    const queryClient = useQueryClient();
    import {VDropdown, VDropdownItem, VTag } from "@halo-dev/components";
    import BiThreeDots from "~icons/bi/three-dots";
    
    const props = defineProps<{
        listedIssueSubject: ListedIssueSubject;
        isSelected: boolean;
    }>();
    const emit = defineEmits<{
      (event: "update", value: ListedIssueSubject): void;
      (event: "updateSelected", value: string): void;
    }>();
    const selectedIssueSubjectNames = inject<Ref<string[]>>("selectedIssueSubjectNames", ref([]));
    
    const handleEdit = (issueSubject: ListedIssueSubject) => {
        emit('update', issueSubject);
    };

    const handleDelete = (issueSubject: ListedIssueSubject) => {
      Dialog.warning({
        title: "删除issue依托主体",
        description: "该操作会将issue依托主体删除，且其下边所关联的issue都会清空，该操作不可恢复。",
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

    const handlerIssueSubjectType = (subjectType:IssueSubjectSpecSubjectTypeEnum):{name: string, icon: FunctionalComponent<SVGAttributes>} =>{
      switch (subjectType){
        case "POST":
          return {name: "文章", icon: MaterialSymbolsLightPostAdd};
        case "PROJECT":
          return {name: "项目", icon: MaterialSymbolsLightPostAdd};
        case "PRODUCT":
          return {name:"产品", icon: MaterialSymbolsLightPostAdd};
        case "TOPIC":
          return {name:"话题", icon: MaterialSymbolsLightPostAdd};
        case "LEAVE_MESSAGE":
          return {name:"留言", icon: MaterialSymbolsLightPostAdd};
      }
    }
</script>
<template>
    <div class="relative space-y-3 border border-neutral-100 rounded-md p-4 my-2 bg-white transition-shadow duration-300"
        :class="{ 'border-blue-500': isSelected }">
        <div class="flex justify-between items-center">
          <div class="w-full flex items-center gap-x-2">
            <p class="text-sm font-bold text-neutral-700">{{ listedIssueSubject.issueSubject.spec.displayName }}</p>
            <VTag theme="primary">
              {{handlerIssueSubjectType(listedIssueSubject.issueSubject.spec.subjectType).name}}
              <template #leftIcon><component :is="handlerIssueSubjectType(listedIssueSubject.issueSubject.spec.subjectType).icon"/></template>
            </VTag>
          </div>
          <HasPermission :permissions="['plugin:issueSubject:manage']">
            <input v-model="selectedIssueSubjectNames" :value="listedIssueSubject.issueSubject.metadata.name"
                   name="issueSubject-checkbox"
                   type="checkbox" />
          </HasPermission>
        </div>
        <div class="p-2 bg-neutral-50 rounded-md h-12 flex items-center">
          <p class="text-sm" v-if="listedIssueSubject.issueSubject.spec?.description">{{ listedIssueSubject.issueSubject.spec?.description }}</p>
          <p class="text-sm text-gray-500" v-else>暂无描述</p>
        </div>
        <div class="w-full flex justify-between items-center">
          <div class="flex items-center gap-x-2 text-xs text-gray-500">
            <VAvatar v-tooltip="listedIssueSubject.contributorVo.displayName" :src="listedIssueSubject.contributorVo.avatar"
                     :alt="listedIssueSubject.contributorVo.displayName" size="xs" circle></VAvatar>
            <span>{{ formatDatetime(listedIssueSubject.issueSubject.metadata.creationTimestamp) }}</span>
          </div>
          <span class="inline-flex items-center gap-x-1.5 rounded-lg bg-gray-100 px-2 py-1 text-xs font-medium outline-none bg-neutral-100">
              <VDropdown>
                <BiThreeDots v-tooltip="'操作'" class="cursor-pointer text-black outline-none" />
                <template #popper>
                  <VDropdownItem @click="handleEdit(listedIssueSubject)">编辑</VDropdownItem>
                  <VDropdownItem @click="handleDelete(listedIssueSubject)">删除</VDropdownItem>
                </template>
              </VDropdown>
            </span>
        </div>
    </div>
</template>
