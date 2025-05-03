import { consoleIssueApiClient, ucIssueApiClient } from "@/api";
import { useQuery } from "@tanstack/vue-query";
import type { Ref } from "vue";

export interface useLabelQueryFetchProps {
  keyword?: Ref<string | undefined>;
}

export function useConsoleLabelQueryFetch(
  props: useLabelQueryFetchProps
): ReturnType<typeof useQuery> {
  return useLabelQueryFetch("console", props);
}

export function useUCLabelQueryFetch(
  props: useLabelQueryFetchProps
): ReturnType<typeof useQuery> {
  return useLabelQueryFetch("uc", props);
}

export function useLabelQueryFetch(
  group: "console" | "uc",
  props: useLabelQueryFetchProps
): ReturnType<typeof useQuery> {
  return useQuery<string[]>({
    queryKey: ["issues-labels", props.keyword],
    queryFn: async () => {
      if (group === "console") {
        const { data } = await consoleIssueApiClient.issue.listLabels({
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
