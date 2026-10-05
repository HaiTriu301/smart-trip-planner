import { QueryClient } from '@tanstack/react-query'
import { isWorthRetrying } from '../api/errors'
import { useAuthStore } from '../stores/authStore'

/** Attempts after the first one, for failures that may pass on their own (isWorthRetrying). */
const MAX_RETRIES = 3

/**
 * The one QueryClient of the app. A request the server answered with 4xx is final: the trip does not exist,
 * is not ours, the input is wrong. Asking again cannot change the answer, it only keeps the user in front of a
 * skeleton for about 7 seconds (TanStack's default: 3 retries after 1, 2 and 4 seconds) before the message shows.
 * The same goes for a 503 PROVIDER_UNAVAILABLE: the backend already retried the outside service (Task 3.8).
 */
export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: (failureCount, error) => isWorthRetrying(error) && failureCount < MAX_RETRIES,
    },
  },
})

// What was loaded belongs to the signed-in user. However the session ends (the logout button, a rejected
// refresh, a 401 on a token we sent), the cached data leaves with it; otherwise the next person to sign in
// on this tab sees the previous user's trips until the server refuses them.
useAuthStore.subscribe((state, previous) => {
  if (previous.status === 'authenticated' && state.status !== 'authenticated') queryClient.clear()
})
