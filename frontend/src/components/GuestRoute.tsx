import { Navigate, Outlet, useLocation, type Location } from 'react-router-dom'
import { useAuthStore } from '../stores/authStore'
import { FullPageSpinner } from './FullPageSpinner'

const DEFAULT_REDIRECT = '/trips'

/**
 * Login / register / forgot-password: a signed-in user is sent on to the page ProtectedRoute bounced them
 * from, or /trips. This is also how a successful login navigates.
 */
export function GuestRoute() {
  const status = useAuthStore((s) => s.status)
  const location = useLocation()

  if (status === 'unknown') return <FullPageSpinner />
  if (status === 'authenticated') {
    const from = (location.state as { from?: Location } | null)?.from
    const target = from ? `${from.pathname}${from.search}${from.hash}` : DEFAULT_REDIRECT
    return <Navigate to={target} replace />
  }
  return <Outlet />
}
