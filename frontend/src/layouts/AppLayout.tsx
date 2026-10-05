import { Link, Outlet } from 'react-router-dom'
import { LogOut } from 'lucide-react'
import { Button } from '../components/Button'
import { Logo } from '../components/Logo'
import { Toaster } from '../components/Toaster'
import { useLogout } from '../features/auth/useLogout'
import { TripSearchBox } from '../features/trips/TripSearchBox'
import { useAuthStore } from '../stores/authStore'

/**
 * Where the data shown in the app comes from. The public services ask for this credit in their terms
 * (design.md 7.2): places and map data are OpenStreetMap's, travel legs come from OSRM, forecasts from
 * Open-Meteo. Shown on every page, also when the server runs on its bundled data: the frontend does not know
 * which source the server uses, and the bundled places are OpenStreetMap data too.
 */
const DATA_SOURCES = [
  { label: 'Bản đồ và địa điểm', name: '© OpenStreetMap', href: 'https://www.openstreetmap.org/copyright' },
  { label: 'Quãng đường', name: 'OSRM', href: 'https://project-osrm.org/' },
  { label: 'Thời tiết', name: 'Open-Meteo', href: 'https://open-meteo.com/' },
] as const

/**
 * Shell of every signed-in page (UI_GUIDE 5.4, 8): 64px white top bar with the logo, the trip search and the
 * user, content up to 1280px wide, and a quiet footer on the page background that credits the data sources.
 * The bar is white on a page that is almost white, so a line under it marks where it ends. On phones the
 * search drops to its own row under the logo. No placeholders for features that do not exist yet
 * (notifications).
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
        <div className="mx-auto flex max-w-[1280px] flex-col gap-x-6 gap-y-1 px-4 py-4 text-xs text-gray-500 sm:flex-row sm:items-center sm:justify-between sm:px-6 lg:px-8">
          <p>© 2026 Smart Trip Planner</p>
          <p className="flex flex-wrap gap-x-3 gap-y-1">
            <span>Nguồn dữ liệu:</span>
            {DATA_SOURCES.map((source) => (
              <span key={source.name}>
                {source.label}{' '}
                <a
                  href={source.href}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="rounded-control font-medium text-gray-700 underline hover:text-jade-dark focus-visible:ring-[3px] focus-visible:ring-jade/25 focus-visible:outline-none"
                >
                  {source.name}
                </a>
              </span>
            ))}
          </p>
        </div>
      </footer>
      <Toaster />
    </div>
  )
}
