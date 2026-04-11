import { useQuery } from "@tanstack/vue-query";
import { consoleApiClient } from "@halo-dev/api-client";

export function useCurrentUserDetailFetch() {
  const {
    data: currentUserDetail,
    isLoading,
    isFetching,
    refetch
  } = useQuery({
    queryKey: ["currentUserDetail"],
    queryFn: async () => {
      const { data } = await consoleApiClient.user.getCurrentUserDetail();
      return data.user;
    },
    refetchOnWindowFocus: false,
  });
  return {
    currentUserDetail,
    isLoading,
    isFetching,
    refetch
  };
}
