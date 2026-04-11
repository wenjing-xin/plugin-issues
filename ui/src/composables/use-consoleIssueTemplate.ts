import { ref, type Ref } from "vue";
import { useQuery } from "@tanstack/vue-query";
import { consoleIssueTemplateApiClient } from "@/api";
import type { IssueSubjectSpecSubjectTypeEnum, IssueTemplateSpecScopeEnum, ListedIssueTemplate } from "@/api/generated";
export function useIssueTemplateListFetch(
  page: Ref<number>,
  size: Ref<number>,
  keyword?: Ref<string>,
  selectedSort?: Ref<string | undefined>,
  ownerName?: Ref<string | undefined>,
  selectedTemplateScope?: Ref<IssueTemplateSpecScopeEnum | undefined>,
  selectedSubjectType?: Ref<IssueSubjectSpecSubjectTypeEnum | undefined>,
  selectedSubjectName?: Ref<string | undefined>
) {
  const total = ref(0);
  const {
    data: issueTemplates,
    isLoading,
    isFetching,
    refetch,
  } = useQuery<ListedIssueTemplate[]>({
    queryKey: ["issueTemplates", page, size, keyword, ownerName, selectedSort, selectedTemplateScope, selectedSubjectType, selectedSubjectName],
    queryFn: async () => {
      const { data } =
        await consoleIssueTemplateApiClient.issueTemplate.listIssueTemplates({
          page: page.value,
          size: size.value,
          keyword: keyword?.value,
          sort: [selectedSort?.value].filter(Boolean) as string[],
          owner: ownerName?.value,
          scope: selectedTemplateScope?.value,
          subjectType: selectedSubjectType?.value,
          subjectName: selectedSubjectName?.value,
        });
      total.value = data.total;
      return data.items;
    },
    refetchInterval: (data) => {
      const hasDeletingData = data?.some(
        (item: ListedIssueTemplate) =>
          !!item.issueTemplate.metadata?.deletionTimestamp,
      );
      return hasDeletingData ? 1000 : false;
    },
    refetchOnWindowFocus: false,
  });
  return {
    issueTemplates,
    isLoading,
    isFetching,
    refetch,
    total,
  };
}
