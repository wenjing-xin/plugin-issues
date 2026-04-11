import { computed, ref, type Ref } from "vue";
import { useQuery } from "@tanstack/vue-query";
import {
  consoleIssueApiClient,
  consoleIssueSubjectApiClient,
  consoleIssueCommentApiClient,
} from "@/api";
import type {
  IssueSubjectSpecSubjectTypeEnum,
  IssueSubjectSpecSubjectVisibleEnum,
  ListedIssue,
  ListedIssueSubject,
  ListedIssueComment,
} from "@/api/generated";
export function useIssueListFetch(
  page: Ref<number>,
  size: Ref<number>,
  subjectName: Ref<string>,
  keyword?: Ref<string>,
  selectedSort?: Ref<string | undefined>,
  ownerName?: Ref<string | undefined>,
  selectedApprovedStatus?: Ref<boolean | undefined>,
  startDate?: Ref<string>,
  endDate?: Ref<string>,
  label?: Ref<string | undefined>,
  selectedTemplate?: Ref<string | undefined>,
) {
  const total = ref(0);
  const {
    data: issues,
    isLoading,
    isFetching,
    refetch,
  } = useQuery<ListedIssue[]>({
    queryKey: [
      "issues",
      page,
      size,
      subjectName,
      keyword,
      selectedSort,
      ownerName,
      selectedApprovedStatus,
      startDate,
      endDate,
      label,
      selectedTemplate,
    ],
    queryFn: async () => {
      const { data } = await consoleIssueApiClient.issue.listIssues({
        subjectName: subjectName.value,
        page: page.value,
        size: size.value,
        keyword: keyword?.value,
        sort: [selectedSort?.value].filter(Boolean) as string[],
        ownerName: ownerName?.value,
        label: label?.value,
        startDate: startDate?.value,
        endDate: endDate?.value,
        approved: selectedApprovedStatus?.value,
        issueTemplate: selectedTemplate?.value,
      });
      total.value = data.total;
      return data.items;
    },
    refetchInterval: (data) => {
      const hasDeletingData = data?.some(
        (item: ListedIssue) => !!item.issue.metadata?.deletionTimestamp,
      );
      return hasDeletingData ? 1000 : false;
    },
    refetchOnWindowFocus: false,
  });
  return {
    issues,
    isLoading,
    isFetching,
    refetch,
    total,
  };
}

export function useIssueSubjectListFetch(
  page: Ref<number>,
  size: Ref<number>,
  keyword?: Ref<string>,
  selectedSort?: Ref<string | undefined>,
  ownerName?: Ref<string | undefined>,
  selectedSubjectType?: Ref<IssueSubjectSpecSubjectTypeEnum | undefined>,
  selectedSubjectVisible?: Ref<IssueSubjectSpecSubjectVisibleEnum | undefined>,
) {
  const total = ref(0);
  const {
    data: issueSubjects,
    isLoading,
    isFetching,
    refetch,
  } = useQuery<ListedIssueSubject[]>({
    queryKey: [
      "issueSubjects",
      page,
      size,
      keyword,
      selectedSort,
      ownerName,
      selectedSubjectType,
      selectedSubjectVisible,
    ],
    queryFn: async () => {
      const { data } =
        await consoleIssueSubjectApiClient.issueSubject.listIssueSubjects({
          page: page.value,
          size: size.value,
          sort: [selectedSort?.value].filter(Boolean) as string[],
          keyword: keyword?.value,
          owner: ownerName?.value,
          subjectType: selectedSubjectType?.value,
          subjectVisible: selectedSubjectVisible?.value,
        });
      total.value = data.total;
      return data.items;
    },
    refetchInterval: (data) => {
      const hasDeletingData = data?.some(
        (item: ListedIssueSubject) =>
          !!item.issueSubject.metadata?.deletionTimestamp,
      );
      return hasDeletingData ? 1000 : false;
    },
    refetchOnWindowFocus: false,
  });
  return {
    issueSubjects,
    isLoading,
    isFetching,
    refetch,
    total,
  };
}

export function useIssueCommentListFetch(
  issueName: string,
  showComments: Ref<boolean>
) {
  const {
    data: issueComments,
    isLoading,
    refetch,
  } = useQuery<ListedIssueComment[]>({
    queryKey: ["issueComments", issueName, showComments],
    queryFn: async () => {
      const { data } =
        await consoleIssueCommentApiClient.issueComment.listIssuesComment({
          page: 0,
          size: 0,
          issueName: issueName,
        });
      return data.items;
    },
    refetchInterval(data) {
      const hasDeletingComments = data?.some(
        (issueComment) =>
          !!issueComment.issueComment.metadata.deletionTimestamp,
      );
      return hasDeletingComments ? 1000 : false;
    },
    enabled: computed(() => showComments.value),
  });
  return {
    issueComments,
    isLoading,
    refetch,
  };
}
