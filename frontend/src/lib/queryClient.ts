import { QueryClient } from '@tanstack/react-query'
import { isRejectedByServer } from '../api/errors'

/** Attempts after the first one, for failures that may pass on their own (network down, timeout, 5xx). */
const MAX_RETRIES = 3

/**
 * The one QueryClient of the app. A request the server answered with 4xx is final: the trip does not exist,
 * is not ours, the input is wrong. Asking again cannot change the answer, it only keeps the user in front of a
 * skeleton for about 7 seconds (TanStack's default: 3 retries after 1, 2 and 4 seconds) before the message shows.
 */
export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: (failureCount, error) => !isRejectedByServer(error) && failureCount < MAX_RETRIES,
    },
  },
})
