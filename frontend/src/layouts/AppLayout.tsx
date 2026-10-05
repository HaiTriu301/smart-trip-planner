import { Link, Outlet } from 'react-router-dom'
import { LogOut } from 'lucide-react'
import { Button } from '../components/Button'
import { Logo } from '../components/Logo'
import { Toaster } from '../components/Toaster'
import { useLogout } from '../features/auth/useLogout'
import { TripSearchBox } from '../features/trips/TripSearchBox'
import { useAuthStore } from '../stores/authStore'

/**
 * Shell of every signed-in page (UI_GUIDE 5.4, 8): 64px white top bar with the logo, the trip search and the
 * user, content up to 1280px wide, and a quiet footer on the page background. The bar is white on a page that
 * is almost white, so a line under it marks where it ends. On phones the search drops to its own row under
 * the logo. No placeholders for features that do not exist yet (notifications).
 */
export function AppLayout() {
  const user = useAuthStore((s) => s.user)
  const logout = useLogout()

  return (
    <div className="flex min-h-screen flex-col">
      <header className="border-b border-tide bg-white">
        <div className="mx-auto flex max-w-[1280px] flex-wrap items-center justify-between gap-x-6 gap-y-2 px-4 py-2 sm:px-6 md:h-16 md:flex-nowrap md:py-0 lg:px-8">
          <Link
            to="/trips"
            className="shrink-0 rounded-control focus-visible:ring-[3px] focus-visible:ring-jade/40 focus-visible:outline-none"
          >
            <Logo />
          </Link>
          {/* One search box: last on its own row on phones, between logo and user from md up */}
          <TripSearchBox className="order-last w-full md:order-none md:max-w-md md:flex-1" />
          <div className="flex shrink-0 items-center gap-2">
            <span className="hidden text-sm font-medium text-gray-700 sm:inline">{user?.fullName}</span>
            <Button
              variant="ghost"
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
      <footer className="border-t border-tide">
        <p className="mx-auto max-w-[1280px] px-4 py-4 text-xs text-gray-500 sm:px-6 lg:px-8">
          © 2026 Smart Trip Planner
        </p>
      </footer>
      <Toaster />
    </div>
  )
}
