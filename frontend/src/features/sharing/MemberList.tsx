import { useId } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { inviteMember, inviteMemberKey } from '../../api/members'
import { getErrorMessage } from '../../api/errors'
import { Avatar } from '../../components/Avatar'
import { Badge } from '../../components/Badge'
import { Button } from '../../components/Button'
import { formatDayMonthIn } from '../../lib/format'
import { DEFAULT_TIME_ZONE } from '../../lib/today'
import { useAuthStore } from '../../stores/authStore'
import { toast } from '../../stores/toastStore'
import type { Member } from '../../types/member'
import { TRIP_ROLE_LABELS, TRIP_ROLE_TONES } from './tripRole'

interface MemberListProps {
  tripId: number
  members: Member[]
  /** The owner looking: pending invitations get "Gửi lại" */
  canManage: boolean
}

/**
 * Everyone on the trip, in the order of GET /trips/{id} (owner first, then members by invitation date): picture
 * or initials, name and email, role, and "Đang chờ" while an invitation is not accepted (UI_GUIDE 15.3).
 * Someone invited without an account has no name yet: the email is the only line.
 */
export function MemberList({ tripId, members, canManage }: MemberListProps) {
  const headingId = useId()
  return (
    <section aria-labelledby={headingId} className="space-y-2">
      <h3 id={headingId} className="tabular text-[15px] leading-6 font-semibold text-ink">
        Thành viên ({members.length})
      </h3>
      <ul className="divide-y divide-tide rounded-card border border-tide">
        {members.map((member) => (
          // The owner has no member row, hence no memberId; there is only one owner
          <MemberRow key={member.memberId ?? 'owner'} tripId={tripId} member={member} canManage={canManage} />
        ))}
      </ul>
    </section>
  )
}

interface MemberRowProps {
  tripId: number
  member: Member
  canManage: boolean
}

function MemberRow({ tripId, member, canManage }: MemberRowProps) {
  const pending = member.status === 'PENDING'
  const timeZone = useAuthStore((state) => state.user?.timezone) ?? DEFAULT_TIME_ZONE
  // The invitation date of a pending row, in the account's time zone: the mail is good for 7 days (design 5.2),
  // and the server does not send when it runs out, so the owner judges from this when to send it again
  const invitedOn = pending && member.invitedAt ? `Đã mời ${formatDayMonthIn(member.invitedAt, timeZone)}` : null
  return (
    <li className="flex items-center gap-3 px-4 py-3">
      <Avatar name={member.fullName} email={member.email} url={member.avatarUrl} pending={pending} />
      <div className="min-w-0 flex-1">
        <p className={`truncate text-[15px] leading-6 text-ink ${member.fullName ? 'font-medium' : ''}`}>
          {member.fullName ?? member.email}
        </p>
        {member.fullName && <p className="truncate text-[13px] leading-5 text-gray-600">{member.email}</p>}
        {invitedOn && <p className="tabular text-[13px] leading-5 text-gray-500">{invitedOn}</p>}
      </div>
      <div className="flex shrink-0 flex-wrap items-center justify-end gap-2">
        {pending && <Badge tone="warning">Đang chờ</Badge>}
        <Badge tone={TRIP_ROLE_TONES[member.role]}>{TRIP_ROLE_LABELS[member.role]}</Badge>
        {pending && canManage && <ResendButton tripId={tripId} member={member} />}
      </div>
    </li>
  )
}

/**
 * "Gửi lại" sends the invitation again: the same POST /members with the same email and role, which issues a
 * new token and a new mail; the link of the earlier mail stops working (design.md 10.2). It carries the invite
 * mutation key, so the dialog stays open until the server has answered.
 */
function ResendButton({ tripId, member }: { tripId: number; member: Member }) {
  const queryClient = useQueryClient()
  const mutation = useMutation({
    mutationKey: inviteMemberKey(tripId),
    mutationFn: () => {
      // A pending row is never the owner's: the role is one an invitation can carry
      const role = member.role === 'VIEWER' ? 'VIEWER' : 'EDITOR'
      return inviteMember(tripId, { email: member.email, role })
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['trip', tripId] })
      toast.success(`Đã gửi lại lời mời tới ${member.email}`)
    },
    onError: async (error) => {
      // The person may have accepted in the meantime (409): the reloaded list shows them as a member
      await queryClient.invalidateQueries({ queryKey: ['trip', tripId] })
      toast.error(`Không gửi lại được lời mời: ${getErrorMessage(error)}`)
    },
  })
  return (
    <Button
      variant="ghost"
      size="sm"
      fullWidth={false}
      aria-label={`Gửi lại lời mời tới ${member.email}`}
      isLoading={mutation.isPending}
      onClick={() => mutation.mutate()}
    >
      Gửi lại
    </Button>
  )
}
