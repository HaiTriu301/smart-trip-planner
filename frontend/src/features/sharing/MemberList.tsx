import { useId } from 'react'
import { Avatar } from '../../components/Avatar'
import { Badge } from '../../components/Badge'
import type { Member } from '../../types/member'
import { TRIP_ROLE_LABELS, TRIP_ROLE_TONES } from './tripRole'

/**
 * Everyone on the trip, in the order of GET /trips/{id} (owner first, then members by invitation date): picture
 * or initials, name and email, role, and "Đang chờ" while an invitation is not accepted (UI_GUIDE 15.3).
 * Someone invited without an account has no name yet: the email is the only line.
 */
export function MemberList({ members }: { members: Member[] }) {
  const headingId = useId()
  return (
    <section aria-labelledby={headingId} className="space-y-2">
      <h3 id={headingId} className="tabular text-[15px] leading-6 font-semibold text-ink">
        Thành viên ({members.length})
      </h3>
      <ul className="divide-y divide-tide rounded-card border border-tide">
        {members.map((member) => (
          // The owner has no member row, hence no memberId; there is only one owner
          <MemberRow key={member.memberId ?? 'owner'} member={member} />
        ))}
      </ul>
    </section>
  )
}

function MemberRow({ member }: { member: Member }) {
  const pending = member.status === 'PENDING'
  return (
    <li className="flex items-center gap-3 px-4 py-3">
      <Avatar name={member.fullName} email={member.email} url={member.avatarUrl} pending={pending} />
      <div className="min-w-0 flex-1">
        {member.fullName ? (
          <>
            <p className="truncate text-[15px] leading-6 font-medium text-ink">{member.fullName}</p>
            <p className="truncate text-[13px] leading-5 text-gray-600">{member.email}</p>
          </>
        ) : (
          <p className="truncate text-[15px] leading-6 text-ink">{member.email}</p>
        )}
      </div>
      <div className="flex shrink-0 flex-wrap items-center justify-end gap-2">
        {pending && <Badge tone="warning">Đang chờ</Badge>}
        <Badge tone={TRIP_ROLE_TONES[member.role]}>{TRIP_ROLE_LABELS[member.role]}</Badge>
      </div>
    </li>
  )
}
