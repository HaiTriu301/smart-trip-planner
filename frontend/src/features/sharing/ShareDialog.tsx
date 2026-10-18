import { useState } from 'react'
import { useIsMutating } from '@tanstack/react-query'
import { Share2, Users } from 'lucide-react'
import { inviteMemberKey } from '../../api/members'
import { Button } from '../../components/Button'
import { Modal } from '../../components/Modal'
import type { TripDetail } from '../../types/trip'
import { InviteMemberForm } from './InviteMemberForm'
import { MemberList } from './MemberList'
import { ShareLinkList } from './ShareLinkList'

/**
 * The button of the trip header that opens the sharing dialog (UI_GUIDE 8.1, 15.3; Task 4.5 decision 3). The
 * owner shares the trip: "Chia sẻ". Anybody else only sees who is on it: "Thành viên". A secondary button
 * either way: the page's primary action stays "Thêm hoạt động".
 */
export function ShareButton({ trip }: { trip: TripDetail }) {
  const [open, setOpen] = useState(false)
  const isOwner = trip.myRole === 'OWNER'
  return (
    <>
      <Button variant="secondary" fullWidth={false} onClick={() => setOpen(true)}>
        {isOwner ? <Share2 aria-hidden className="size-4" /> : <Users aria-hidden className="size-4" />}
        {isOwner ? 'Chia sẻ' : 'Thành viên'}
      </Button>
      <ShareDialog trip={trip} open={open} onClose={() => setOpen(false)} />
    </>
  )
}

interface ShareDialogProps {
  trip: TripDetail
  open: boolean
  onClose: () => void
}

/**
 * Sharing a trip, a 640px dialog (UI_GUIDE 15.3). The members come with the trip itself (GET /trips/{id}), so the
 * dialog opens without a request of its own and shows what the page shows. Only the owner gets the invite form
 * (the cursor starts in its email field) and the share links, under a rule. Esc and "×" wait while an invitation is being sent: closing would leave
 * the answer (an error under the email) nowhere to show (BUG-UI-004).
 */
function ShareDialog({ trip, open, onClose }: ShareDialogProps) {
  const isOwner = trip.myRole === 'OWNER'
  const isInviting = useIsMutating({ mutationKey: inviteMemberKey(trip.id) }) > 0
  // Only the owner's own row: nobody else sees this trip yet (UI_GUIDE 7.9, no button: the form is right above)
  const alone = trip.members.length === 1
  return (
    <Modal
      open={open}
      onClose={() => !isInviting && onClose()}
      size="lg"
      title={isOwner ? 'Chia sẻ chuyến đi' : 'Thành viên chuyến đi'}
    >
      <div className="space-y-6">
        {isOwner && (
          <div className="space-y-3">
            <InviteMemberForm tripId={trip.id} />
            {alone && (
              <p className="text-sm text-gray-600">Chỉ mình bạn thấy chuyến đi này. Mời bạn bè để cùng chỉnh sửa.</p>
            )}
          </div>
        )}
        <MemberList tripId={trip.id} members={trip.members} canManage={isOwner} />
        {isOwner && (
          <div className="border-t border-tide pt-6">
            <ShareLinkList tripId={trip.id} />
          </div>
        )}
      </div>
    </Modal>
  )
}
