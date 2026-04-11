import { ref, type Ref } from "vue";
import { useQuery } from "@tanstack/vue-query";
import { consoleIssueSubjectApiClient } from "@/api";
import type {
  ListedIssueSubject,
  ListIssueSubjectsSubjectTypeEnum
} from "@/api/generated";
export function useIssueSubjectListFetch(
  page: Ref<number>,
  size: Ref<number>,
  keyword?: Ref<string>,
  selectedSort?: Ref<string | undefined>,
  ownerName?: Ref<string | undefined>,
  subjectType?: Ref<ListIssueSubjectsSubjectTypeEnum|undefined>,
) {
  const total = ref(0);
  const {
    data: issueSubjects,
    isLoading,
    isFetching,
    refetch,
  } = useQuery<ListedIssueSubject[]>({
    queryKey: ["issueSubjects", page, size, keyword, ownerName, subjectType],
    queryFn: async () => {
      const { data } = await consoleIssueSubjectApiClient.issueSubject.listIssueSubjects({
        page: page.value,
        size: size.value,
        keyword: keyword?.value,
        sort: [selectedSort?.value].filter(Boolean) as string[],
        owner: ownerName?.value,
        subjectType: subjectType?.value
      });
      total.value = data.total;
      return data.items;
    },
    refetchInterval: (data) => {
      const hasDeletingData = data?.some((item: ListedIssueSubject) => !!item.issueSubject.metadata?.deletionTimestamp);
      return hasDeletingData ? 1000 : false;
    },
    refetchOnWindowFocus: false,
  });
  return {
    issueSubjects,
    isLoading,
    isFetching,
    refetch,
    total
  };
}
