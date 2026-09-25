import { Button } from '../components/Button'
import { useLogout } from '../features/auth/useLogout'
import { useAuthStore } from '../stores/authStore'

/** Placeholder until Task 2.5 builds the trip list; proves the protected route + logout work. */
export function TripsPage() {
  const user = useAuthStore((s) => s.user)
  const logout = useLogout()

  return (
    <div className="min-h-screen bg-slate-50">
      <header className="border-b border-slate-200 bg-white">
        <div className="mx-auto flex max-w-5xl items-center justify-between gap-4 px-4 py-3">
          <span className="font-bold text-sky-700">Smart Trip Planner</span>
          <div className="flex items-center gap-3">
            <span className="hidden text-sm text-slate-600 sm:inline">{user?.fullName}</span>
            <Button
              variant="secondary"
              className="w-auto"
              isLoading={logout.isPending}
              onClick={() => logout.mutate()}
            >
              Đăng xuất
            </Button>
          </div>
        </div>
      </header>
      <main className="mx-auto max-w-5xl px-4 py-8">
        <h1 className="text-2xl font-bold text-slate-800">Chuyến đi của tôi</h1>
        <p className="mt-4 text-slate-500">Bạn chưa có chuyến đi nào.</p>
      </main>
    </div>
  )
}
