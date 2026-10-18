import type { ReactNode } from 'react'
import { useQuery } from '@tanstack/react-query'
import { useParams } from 'react-router-dom'
import { CalendarDays, Clock, Link2Off, MapPin, TriangleAlert } from 'lucide-react'
import { getApiError, getErrorMessage } from '../api/errors'
import { getPublicTrip } from '../api/public'
import { Button } from '../components/Button'
import { ExpandableText } from '../components/ExpandableText'
import { LinkButton } from '../components/LinkButton'
import { Logo } from '../components/Logo'
import { Skeleton } from '../components/Skeleton'
import { PublicDay } from '../features/public/PublicDay'
import { PublicTripMap } from '../features/public/PublicTripMap'
import { countDays, formatDateRange } from '../lib/format'
import { isShareToken } from '../lib/shareUrl'

/**
 * The trip behind a share link, for anybody, without an account (design.md 15, UI_GUIDE 8.5): one column of
 * 760px, a small logo instead of the app bar, the summary, one map of every place, then every day on one page with
 * read-only cards, and at the end an invitation to plan one's own trip. Outside every guard: signed in or not, the
 * page is the same.
 * <p>
 * Each successful load counts one view of the link (design.md 10.2), so the page asks once: no reload when the tab
 * comes back into focus or the network returns, and the answer is kept while the page stays open.
 */
export function PublicTripPage() {
  const { token } = useParams()
  const validToken = isShareToken(token)
  const { data: trip, error, isPending, isFetching, refetch } = useQuery({
    queryKey: ['public-trip', token],
    queryFn: () => getPublicTrip(token!),
    enabled: validToken,
    staleTime: Infinity,
    refetchOnWindowFocus: false,
    refetchOnReconnect: false,
  })

  if (!validToken || getApiError(error)?.errorCode === 'RESOURCE_NOT_FOUND') {
    return (
      <PublicFrame>
        <StatusCard
          icon={<Link2Off aria-hidden className="size-8 text-gray-500" />}
          title="Liên kết không còn hiệu lực"
          text="Liên kết đã bị thu hồi, đã hết hạn, hoặc chuyến đi không còn."
        >
          <LinkButton variant="secondary" to="/trips/new" className="w-full">
            Tạo chuyến đi của bạn
          </LinkButton>
        </StatusCard>
      </PublicFrame>
    )
  }

  if (isPending) {
    return (
      <PublicFrame>
        <div role="status" aria-label="Đang tải chuyến đi" className="mx-auto max-w-[760px] space-y-4">
          <Skeleton className="h-10 w-2/3" />
          <Skeleton className="h-5 w-1/2" />
          {Array.from({ length: 3 }, (_, i) => (
            <Skeleton key={i} className="h-24 w-full" />
          ))}
        </div>
      </PublicFrame>
    )
  }

  if (!trip) {
    // Server unreachable or failing: nothing was counted, trying again is fine
    return (
      <PublicFrame>
        <StatusCard
          icon={<TriangleAlert aria-hidden className="size-8 text-gray-500" />}
          title="Chưa tải được chuyến đi"
          text={getErrorMessage(error)}
        >
          <Button variant="secondary" isLoading={isFetching} onClick={() => refetch()}>
            Thử lại
          </Button>
        </StatusCard>
      </PublicFrame>
    )
  }

  return (
    <PublicFrame>
      <article className="mx-auto max-w-[760px] space-y-8">
        <header className="space-y-3">
          <h1 className="text-[32px] leading-10 font-bold tracking-[-0.02em] wrap-anywhere text-ink">{trip.title}</h1>
          <ul className="flex flex-wrap gap-x-5 gap-y-1 text-sm text-gray-600">
            {trip.destinationName && (
              <li className="inline-flex min-w-0 items-center gap-1.5 wrap-anywhere">
                <MapPin aria-hidden className="size-4 shrink-0 text-gray-400" />
                {trip.destinationName}
              </li>
            )}
            <li className="tabular inline-flex items-center gap-1.5">
              <CalendarDays aria-hidden className="size-4 shrink-0 text-gray-400" />
              {formatDateRange(trip.startDate, trip.endDate)}
            </li>
            <li className="inline-flex items-center gap-1.5">
              <Clock aria-hidden className="size-4 shrink-0 text-gray-400" />
              {countDays(trip.startDate, trip.endDate)} ngày
            </li>
          </ul>
          {trip.description && <ExpandableText text={trip.description} className="text-gray-600" />}
        </header>

        <PublicTripMap
          days={trip.days}
          destination={
            trip.destinationLat !== null && trip.destinationLng !== null
              ? { lat: trip.destinationLat, lng: trip.destinationLng }
              : null
          }
        />

        {trip.days.map((day) => (
          <PublicDay key={day.dayIndex} day={day} />
        ))}

        <footer className="space-y-3 border-t border-tide pt-8 text-center">
          <LinkButton variant="secondary" to="/trips/new">
            Tạo chuyến đi của bạn
          </LinkButton>
          <p className="text-[13px] text-gray-500">Được chia sẻ qua Smart Trip Planner</p>
          {/* The places come from OpenStreetMap: its terms ask for the credit (design.md 7.2, UI_GUIDE 8.0) */}
          <p className="text-xs text-gray-500">
            Bản đồ và địa điểm{' '}
            <a
              href="https://www.openstreetmap.org/copyright"
              target="_blank"
              rel="noopener noreferrer"
              className="rounded-control font-medium text-gray-700 underline hover:text-jade-dark focus-visible:ring-[3px] focus-visible:ring-jade/25 focus-visible:outline-none"
            >
              © OpenStreetMap
            </a>
          </p>
        </footer>
      </article>
    </PublicFrame>
  )
}

/** No app bar for a guest: the small logo at the top left, then the content (UI_GUIDE 8.5). */
function PublicFrame({ children }: { children: ReactNode }) {
  return (
    <div className="min-h-screen px-4 pb-16 sm:px-6">
      <div className="py-5">
        <Logo />
      </div>
      <main className="pt-4">{children}</main>
    </div>
  )
}

interface StatusCardProps {
  icon: ReactNode
  title: string
  text: string
  children: ReactNode
}

/** A small card centred in the page, as in the Stitch mockup "public-trip-error". */
function StatusCard({ icon, title, text, children }: StatusCardProps) {
  return (
    <div className="mx-auto mt-16 max-w-[420px] space-y-4 rounded-card bg-white p-8 text-center shadow-md">
      <div className="flex justify-center">{icon}</div>
      <h1 className="text-lg leading-[26px] font-semibold text-ink">{title}</h1>
      <p className="text-sm leading-5 text-gray-600">{text}</p>
      {children}
    </div>
  )
}
