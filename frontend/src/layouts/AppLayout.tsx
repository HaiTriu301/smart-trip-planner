import { Link, Outlet } from 'react-router-dom'
import { LogOut } from 'lucide-react'
import { Button } from '../components/Button'
import { Logo } from '../components/Logo'
import { useLogout } from '../features/auth/useLogout'
import { useAuthStore } from '../stores/authStore'

/**
 * Shell of every signed-in page (UI_GUIDE 5.4, 8): 56px ink top bar with the logo and the user,
 * content up to 1280px wide, and a quiet ink footer. No placeholders for features that do not exist yet
 * (notifications, global search).
 */
export function AppLayout() {
  const user = useAuthStore((s) => s.user)
  const logout = useLogout()

  return (
    <div className="flex min-h-screen flex-col">
      <header className="bg-ink">
        <div className="mx-auto flex h-14 max-w-[1280px] items-center justify-between gap-4 px-4 sm:px-6 lg:px-8">
          <Link to="/trips" className="rounded-control focus-visible:ring-[3px] focus-visible:ring-jade/40 focus-visible:outline-none">
            <Logo tone="dark" />
          </Link>
          <div className="flex items-center gap-2">
            <span className="hidden text-sm text-gray-300 sm:inline">{user?.fullName}</span>
            <Button
              variant="ghost-inverse"
              fullWidth={false}
              isLoading={logout.isPending}
              onClick={() => logout.mutate()}
            >
              {!logout.isPending && <LogOut aria-hidden className="size-4" />}
              Đăng xuất
            </Button>
          </div>
        </div>
      </header>
      <main className="mx-auto w-full max-w-[1280px] flex-1 px-4 py-8 sm:px-6 lg:px-8">
        <Outlet />
      </main>
      <footer className="bg-ink">
        <p className="mx-auto max-w-[1280px] px-4 py-4 text-xs text-gray-400 sm:px-6 lg:px-8">
          © 2026 Smart Trip Planner
        </p>
      </footer>
    </div>
  )
}
