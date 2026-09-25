import { useMutation, useQueryClient } from '@tanstack/react-query'
import { logout } from '../../api/auth'
import { useAuthStore } from '../../stores/authStore'

/**
 * POST /auth/logout needs a live access token: the client interceptor refreshes an expired one first.
 * If that refresh also fails the server has no session left, so the local state is cleared either way.
 */
export function useLogout() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: logout,
    onSettled: () => {
      useAuthStore.getState().clearSession()
      queryClient.clear()
    },
  })
}
