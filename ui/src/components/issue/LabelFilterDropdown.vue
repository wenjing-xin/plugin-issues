<script lang="ts" setup>
  import { VEntity, VDropdown, IconArrowDown, IconClose, VEntityField } from "@halo-dev/components";
  import { computed, ref } from "vue";
  import { useConsoleLabelQueryFetch } from "@/composables/use-issueLabels";

  const props = withDefaults(
    defineProps<{
      modelValue?: string;
      label: string;
    }>(),
    {
      modelValue: undefined,
    }
  );

  const emit = defineEmits<{
    (event: "update:modelValue", value?: string): void;
  }>();

  const keyword = ref(undefined);

  const { data: labels, refetch } = useConsoleLabelQueryFetch({
    keyword,
  });

  const searchResults = computed(() => labels.value || []);

  const dropdown = ref();

  const handleSelect = (tag: string) => {
    if (tag === props.modelValue) {
      emit("update:modelValue", undefined);
    } else {
      emit("update:modelValue", tag);
    }
    dropdown.value.hide();
  };

  const handleCloseTag = (event: Event) => {
    emit("update:modelValue", undefined);
    event.stopPropagation();
  };
</script>

<template>
  <VDropdown ref="dropdown" :classes="['!p-0']" @show="refetch">
    <div
      class="group flex cursor-pointer select-none items-center px-2 text-sm text-gray-700 leading-9 hover:text-black"
      :class="{ 'font-semibold text-gray-700': modelValue !== undefined }">
      <span v-if="!modelValue" class="mr-0.5">
        {{ label }}
      </span>
      <span v-else class="mr-0.5"> {{ label }}：{{ modelValue }} </span>
      <span class="text-base">
        <IconArrowDown :class="{ 'group-hover:hidden': modelValue }" />
        <IconClose v-if="modelValue" class="hidden group-hover:block" @click="handleCloseTag" />
      </span>
    </div>
    <template #popper>
      <div class="h-96 w-80">
        <div class="border-b border-b-gray-100 bg-white p-4">
          <FormKit id="tagFilterDropdownInput" v-model="keyword" :placeholder="`输入${label}搜索`" type="text"></FormKit>
        </div>
        <div>
          <ul class="box-border size-full divide-y divide-gray-100" role="list">
            <li v-for="(label, index) in searchResults" :key="index" @click="handleSelect(label)">
              <VEntity :is-selected="modelValue === label">
                <template #start>
                  <VEntityField>
                    <template #title>
                      <p class="text-sm"> {{ label }}</p>
                    </template>
                  </VEntityField>
                </template>
              </VEntity>
            </li>
          </ul>
        </div>
      </div>
    </template>
  </VDropdown>
</template>
