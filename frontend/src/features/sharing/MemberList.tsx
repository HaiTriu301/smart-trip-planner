import { useId, useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { changeMemberRole, inviteMember, inviteMemberKey, removeMember } from '../../api/members'
import { getErrorMessage } from '../../api/errors'
import { Avatar } from '../../components/Avatar'
import { Badge } from '../../components/Badge'
import { Button } from '../../components/Button'
import { ConfirmDialog } from '../../components/ConfirmDialog'
import { formatDayMonthIn } from '../../lib/format'
import { DEFAULT_TIME_ZONE } from '../../lib/today'
import { useAuthStore } from '../../stores/authStore'
import { toast } from '../../stores/toastStore'
import type { Member, MemberRole } from '../../types/member'
import { MEMBER_ROLE_OPTIONS } from './schemas'
import { TRIP_ROLE_LABELS, TRIP_ROLE_TONES } from './tripRole'

interface MemberListProps {
  tripId: number
  members: Member[]
  /** The owner looking: every other row gets a role select and a remove button, pending invitations "Gửi lại" */
  canManage: boolean
}

/**
 * Everyone on the trip, in the order of GET /trips/{id} (owner first, then members by invitation date): picture
 * or initials, name and email, role, and "Đang chờ" while an invitation is not accepted (UI_GUIDE 15.3).
 * Someone invited without an account has no name yet: the email is the only line.
 */
export function MemberList({ tripId, members, canManage }: MemberListProps) {
  const headingId = useId()
  const queryClient = useQueryClient()
  // The row waiting for confirmation of "Gỡ" / "Huỷ lời mời"
  const [removing, setRemoving] = useState<Member | null>(null)
  const removingPending = removing?.status === 'PENDING'

  const removal = useMutation({
    mutationFn: (member: Member) => removeMember(tripId, member.memberId!),
    onSuccess: async (_none, member) => {
      await queryClient.invalidateQueries({ queryKey: ['trip', tripId] })
      setRemoving(null)
      toast.success(
        member.status === 'PENDING' ? `Đã huỷ lời mời gửi tới ${member.email}` : `Đã gỡ ${member.fullName ?? member.email}`,
      )
    },
  })

  return (
    <section aria-labelledby={headingId} className="space-y-2">
      <h3 id={headingId} className="tabular text-[15px] leading-6 font-semibold text-ink">
        Thành viên ({members.length})
      </h3>
      <ul className="divide-y divide-tide rounded-card border border-tide">
        {members.map((member) => (
          // The owner has no member row, hence no memberId; there is only one owner
          <MemberRow
            key={member.memberId ?? 'owner'}
            tripId={tripId}
            member={member}
            canManage={canManage}
            onRemove={() => {
              removal.reset()
              setRemoving(member)
            }}
          />
        ))}
      </ul>

      {/* Opens over the sharing dialog (a second native dialog on top): Esc closes this one only */}
      <ConfirmDialog
        open={removing !== null}
        title={removingPending ? 'Huỷ lời mời?' : 'Gỡ thành viên?'}
        confirmLabel={removingPending ? 'Huỷ lời mời' : 'Gỡ thành viên'}
        cancelLabel={removingPending ? 'Giữ lời mời' : 'Huỷ'}
        variant="danger"
        isLoading={removal.isPending}
        error={removal.isError ? getErrorMessage(removal.error) : undefined}
        onCancel={() => setRemoving(null)}
        onConfirm={() => removing && removal.mutate(removing)}
      >
        {removingPending ? (
          <p>
            Link trong mail mời gửi tới <strong className="wrap-anywhere text-ink">{removing?.email}</strong> sẽ
            không dùng được nữa. Bạn có thể mời lại sau.
          </p>
        ) : (
          <p>
            <strong className="wrap-anywhere text-ink">{removing?.fullName ?? removing?.email}</strong> sẽ không
            còn xem được chuyến đi này. Bạn có thể mời lại sau.
          </p>
        )}
      </ConfirmDialog>
    </section>
  )
}

interface MemberRowProps {
  tripId: number
  member: Member
  canManage: boolean
  onRemove: () => void
}

function MemberRow({ tripId, member, canManage, onRemove }: MemberRowProps) {
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
        {/* The owner's row has no member id: a fixed badge, the owner cannot be demoted (design rule 14.7) */}
        {canManage && member.memberId !== null ? (
          <RoleSelect tripId={tripId} memberId={member.memberId} member={member} />
        ) : (
          <Badge tone={TRIP_ROLE_TONES[member.role]}>{TRIP_ROLE_LABELS[member.role]}</Badge>
        )}
        {pending && canManage && <ResendButton tripId={tripId} member={member} />}
        {canManage && member.memberId !== null && (
          <Button
            variant="secondary"
            size="sm"
            fullWidth={false}
            aria-label={pending ? `Huỷ lời mời gửi tới ${member.email}` : `Gỡ ${member.fullName ?? member.email}`}
            onClick={onRemove}
          >
            {pending ? 'Huỷ lời mời' : 'Gỡ'}
          </Button>
        )}
      </div>
    </li>
  )
}

const isMemberRole = (value: string): value is MemberRole => value === 'EDITOR' || value === 'VIEWER'

interface RoleSelectProps {
  tripId: number
  memberId: number
  member: Member
}

/**
 * The role of one member, changed by the owner: saved as soon as it is picked, no save button (Task 4.5
 * decision 5). Works on a pending invitation too: the person gets the new role when they accept. While saving it
 * shows the chosen value; a refusal puts the stored one back and says why in a toast.
 */
function RoleSelect({ tripId, memberId, member }: RoleSelectProps) {
  const queryClient = useQueryClient()
  const mutation = useMutation({
    mutationFn: (role: MemberRole) => changeMemberRole(tripId, memberId, { role }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['trip', tripId] })
      toast.success('Đã đổi vai trò')
    },
    onError: async (error) => {
      // Removed or accepted elsewhere in the meantime: the reloaded list shows how things are now
      await queryClient.invalidateQueries({ queryKey: ['trip', tripId] })
      toast.error(`Không đổi được vai trò: ${getErrorMessage(error)}`)
    },
  })
  const shown = mutation.isPending && mutation.variables ? mutation.variables : member.role
  return (
    <select
      aria-label={`Vai trò của ${member.fullName ?? member.email}`}
      value={shown}
      disabled={mutation.isPending}
      onChange={(event) => {
        if (isMemberRole(event.target.value)) mutation.mutate(event.target.value)
      }}
      className="h-8 cursor-pointer rounded-control border border-tide bg-white pr-7 pl-2.5 text-[13px] text-ink outline-none transition-colors hover:border-gray-300 focus-visible:border-jade focus-visible:ring-[3px] focus-visible:ring-jade/15 disabled:cursor-wait disabled:bg-gray-50 pointer-coarse:h-11"
    >
      {MEMBER_ROLE_OPTIONS.map((option) => (
        <option key={option.value} value={option.value}>
          {option.label}
        </option>
      ))}
    </select>
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
