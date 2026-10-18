import type { ReactNode } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { Navigate, useSearchParams } from 'react-router-dom'
import { CloudOff, Link2Off, LoaderCircle, UserX } from 'lucide-react'
import { acceptInvitation } from '../api/members'
import { getApiError, getErrorMessage } from '../api/errors'
import { getTrip } from '../api/trips'
import { Button } from '../components/Button'
import { LinkButton } from '../components/LinkButton'
import { Logo } from '../components/Logo'
import { useLogout } from '../features/auth/useLogout'
import { parseInviteParams } from '../lib/inviteParams'
import { useAuthStore } from '../stores/authStore'
import { toast } from '../stores/toastStore'

type Outcome = 'joined' | 'already-member'

/**
 * Landing page of the invitation mail: /invite?trip={id}&token=... (design.md 15, UI_GUIDE 15.3). It sits behind
 * the sign-in, so whoever opens it is a known account; it accepts at once, with no form, then opens the trip.
 * Seen only for a moment, unless the invitation cannot be accepted:
 * - 400 (used, expired, cancelled, not for this trip) and 404 (trip deleted): no longer valid. A 400 may also mean
 *   the person already accepted and opened the mail again: when the trip opens for them, they go there instead
 * - 403: the account signed in is not the invited email (design rule 14.24); signing out brings the sign-in
 *   page, which comes back here afterwards
 */
export function InvitePage() {
  const [search] = useSearchParams()
  const params = parseInviteParams(search)
  const queryClient = useQueryClient()
  const email = useAuthStore((s) => s.user?.email)
  const logout = useLogout()

  // useQuery, not a mutation in an effect: the cache sends the one-shot token exactly once even when StrictMode
  // mounts the page twice (same as /verify-email). The session ending clears the cache, so after signing in with
  // another account the invitation is tried again
  const { data, error, isPending, isFetching, refetch } = useQuery({
    queryKey: ['invite', params?.tripId, params?.token],
    queryFn: async (): Promise<Outcome> => {
      if (!params) throw new Error('unreachable: the query only runs with valid params')
      try {
        await acceptInvitation(params.tripId, params.token)
      } catch (failure) {
        if (getApiError(failure)?.errorCode !== 'INVALID_TOKEN') throw failure
        // Already a member opening the mail again: the token was used by them. Anyone else gets the 400 back
        await getTrip(params.tripId).catch(() => {
          throw failure
        })
        toast.success('Bạn đã là thành viên của chuyến đi này')
        return 'already-member'
      }
      // The trip now belongs in the list of this account
      await queryClient.invalidateQueries({ queryKey: ['trips'] })
      toast.success('Đã nhận lời mời tham gia chuyến đi')
      return 'joined'
    },
    enabled: params !== null,
    retry: false,
    staleTime: Infinity,
    gcTime: Infinity,
    refetchOnWindowFocus: false,
  })

  if (params === null) return <NoLongerValid />
  if (data) return <Navigate to={`/trips/${params.tripId}`} replace />
  if (isPending && isFetching) {
    return (
      <InviteCard>
        <div role="status" className="flex flex-col items-center gap-4 py-6">
          <LoaderCircle aria-hidden className="size-8 animate-spin text-jade" />
          <p className="text-[15px] text-ink">Đang nhận lời mời…</p>
        </div>
      </InviteCard>
    )
  }

  const code = getApiError(error)?.errorCode
  if (code === 'INVALID_TOKEN' || code === 'RESOURCE_NOT_FOUND') return <NoLongerValid />
  if (code === 'FORBIDDEN') {
    return (
      <InviteCard
        icon={<UserX aria-hidden className="size-8 text-gray-500" />}
        title="Lời mời này dành cho email khác"
        text={email ? `Bạn đang đăng nhập bằng ${email}.` : undefined}
      >
        <Button variant="secondary" isLoading={logout.isPending} onClick={() => logout.mutate()}>
          Đăng xuất để đổi tài khoản
        </Button>
      </InviteCard>
    )
  }

  // The server could not be reached or failed: the token was not used, trying again is safe
  return (
    <InviteCard
      icon={<CloudOff aria-hidden className="size-8 text-gray-500" />}
      title="Chưa nhận được lời mời"
      text={getErrorMessage(error)}
    >
      <Button variant="secondary" isLoading={isFetching} onClick={() => refetch()}>
        Thử lại
      </Button>
    </InviteCard>
  )
}

function NoLongerValid() {
  return (
    <InviteCard
      icon={<Link2Off aria-hidden className="size-8 text-gray-500" />}
      title="Lời mời không còn hiệu lực"
      text="Lời mời đã được dùng, đã hết hạn hoặc đã bị huỷ. Nhờ chủ chuyến đi gửi lại."
    >
      <LinkButton variant="secondary" to="/trips" className="w-full">
        Về danh sách chuyến đi
      </LinkButton>
    </InviteCard>
  )
}

interface InviteCardProps {
  icon?: ReactNode
  title?: string
  text?: string
  children?: ReactNode
}

/** The logo above a small white card centred on the page (UI_GUIDE 15.3, Stitch mockup "invite-page"). */
function InviteCard({ icon, title, text, children }: InviteCardProps) {
  return (
    <main className="flex min-h-screen items-center justify-center px-4 py-12">
      <div className="w-full max-w-[420px] space-y-6">
        <div className="flex justify-center">
          <Logo />
        </div>
        <div className="space-y-4 rounded-card bg-white p-8 text-center shadow-md">
          {icon && <div className="flex justify-center">{icon}</div>}
          {title && <h1 className="text-lg leading-[26px] font-semibold text-ink">{title}</h1>}
          {text && <p className="text-sm leading-5 wrap-anywhere text-gray-600">{text}</p>}
          {children}
        </div>
      </div>
    </main>
  )
}
