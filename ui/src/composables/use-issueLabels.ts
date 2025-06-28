import {
  consoleIssueApiClient,
  ucIssueApiClient,
  consoleIssueLabelApiClient
} from "@/api";
import { useQuery } from "@tanstack/vue-query";
import type { Ref } from "vue";
import { ref } from "vue";
import type {
  IssueLabelSpecScopeEnum,
  IssueSubjectSpecSubjectTypeEnum,
  ListedIssueLabel
} from "@/api/generated";

export interface useLabelQueryFetchProps {
  keyword?: Ref<string | undefined>;
}

export function useSubjectLabelQueryFetch(
  props: useLabelQueryFetchProps,
): ReturnType<typeof useQuery> {
  return useLabelQueryFetch("console", props);
}

export function useUCLabelQueryFetch(
  props: useLabelQueryFetchProps,
): ReturnType<typeof useQuery> {
  return useLabelQueryFetch("uc", props);
}

export function useLabelQueryFetch(
  group: "console" | "uc",
  props: useLabelQueryFetchProps,
): ReturnType<typeof useQuery> {
  return useQuery<string[]>({
    queryKey: ["issues-labels", props.keyword],
    queryFn: async () => {
      if (group === "console") {
        const { data } = await consoleIssueApiClient.issue.listSubjectLabels({
          name: props.keyword?.value,
        });
        return data;
      }

      if (group === "uc") {
        const { data } = await ucIssueApiClient.issue.listMyLabels({
          name: props.keyword?.value,
        });
        return data;
      }

      throw new Error("Invalid group");
    },
  });
}

export function useIssueLabels(
  page: Ref<number>,
  size: Ref<number>,
  keyword?: Ref<string>,
  selectedSort?: Ref<string | undefined>,
  subjectName?: Ref<string | undefined>,
  labelScope?: Ref<IssueLabelSpecScopeEnum | undefined>,
  subjectType?: Ref<IssueSubjectSpecSubjectTypeEnum | undefined>
) {
  const total = ref(0);
  const {
    data: issueLabels,
    isLoading,
    isFetching,
    refetch,
  } = useQuery<ListedIssueLabel[]>({
    queryKey: ["issueLabels", page, size, keyword, subjectName, selectedSort, labelScope, subjectType],
    queryFn: async () => {
      const { data } =
        await consoleIssueLabelApiClient.issueLabel.listIssueLabels({
          page: page.value,
          size: size.value,
          sort: [selectedSort?.value].filter(Boolean) as string[],
          keyword: keyword?.value,
          subjectName: subjectName?.value,
          scope: labelScope?.value,
          subjectType: subjectType?.value
        });
      total.value = data.total;
      return data.items;
    },
    refetchInterval: (data) => {
      const hasDeletingData = data?.some(
        (item: ListedIssueLabel) =>
          !!item.issueLabel.metadata?.deletionTimestamp,
      );
      return hasDeletingData ? 1000 : false;
    },
    refetchOnWindowFocus: false,
  });
  return {
    issueLabels,
    isLoading,
    isFetching,
    refetch,
    total,
  };
}
