import { ref, type Ref } from "vue";
import { useQuery } from "@tanstack/vue-query";
import { consoleIssueMessageApiClient } from "@/api";
import type { ListedIssueMessage } from "@/api/generated";
export function useIssueMessageListFetch(
  page: Ref<number>,
  size: Ref<number>,
  keyword?: Ref<string>,
  selectedSort?: Ref<string | undefined>,
  ownerName?: Ref<string | undefined>,
  selectedApprovedStatus?: Ref<boolean | undefined>,
  startDate?: Ref<string>,
  endDate?: Ref<string>,
  label?: Ref<string | undefined>,
  selectedTempalte?: Ref<string | undefined>,
) {
  const total = ref(0);
  const {
    data: issueMessages,
    isLoading,
    isFetching,
    refetch,
  } = useQuery<ListedIssueMessage[]>({
    queryKey: ["issueMessages", page, size, keyword, selectedSort, ownerName, selectedApprovedStatus, startDate, endDate, label, selectedTempalte],
    queryFn: async () => {
      const { data } = await consoleIssueMessageApiClient.issueMessage.listIssueMessages({
        page: page.value,
        size: size.value,
        keyword: keyword?.value,
        sort: [selectedSort?.value].filter(Boolean) as string[],
        ownerName: ownerName?.value,
        label: label?.value,
        startDate: startDate?.value,
        endDate: endDate?.value,
        approved: selectedApprovedStatus?.value,
        issueTemplate: selectedTempalte?.value
      });
      total.value = data.total;
      return data.items;
    },
    refetchInterval: (data) => {
      const hasDeletingData = data?.some((item: ListedIssueMessage) => !!item.issueMessage.metadata?.deletionTimestamp);
      return hasDeletingData ? 1000 : false;
    },
    refetchOnWindowFocus: false,
  });
  return {
    issueMessages,
    isLoading,
    isFetching,
    refetch,
    total
  };
}
