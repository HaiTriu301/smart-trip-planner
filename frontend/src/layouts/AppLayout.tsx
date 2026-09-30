import { Link, Outlet } from 'react-router-dom'
import { Button } from '../components/Button'
import { useLogout } from '../features/auth/useLogout'
import { useAuthStore } from '../stores/authStore'

/** Shell of every signed-in page: header with the user's name and logout, page content below. */
export function AppLayout() {
  const user = useAuthStore((s) => s.user)
  const logout = useLogout()

  return (
    <div className="min-h-screen bg-slate-50">
      <header className="border-b border-slate-200 bg-white">
        <div className="mx-auto flex max-w-6xl items-center justify-between gap-4 px-4 py-3">
          <Link to="/trips" className="font-bold text-sky-700">
            Smart Trip Planner
          </Link>
          <div className="flex items-center gap-3">
            <span className="hidden text-sm text-slate-600 sm:inline">{user?.fullName}</span>
            <Button
              variant="secondary"
              fullWidth={false}
              isLoading={logout.isPending}
              onClick={() => logout.mutate()}
            >
              Đăng xuất
            </Button>
          </div>
        </div>
      </header>
      <main className="mx-auto max-w-6xl px-4 py-8">
        <Outlet />
      </main>
    </div>
  )
}
