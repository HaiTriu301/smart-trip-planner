import { useState } from 'react'
import { Share2, Users } from 'lucide-react'
import { Button } from '../../components/Button'
import { Modal } from '../../components/Modal'
import type { TripDetail } from '../../types/trip'
import { MemberList } from './MemberList'

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
 * dialog opens without a request of its own and shows what the page shows.
 */
function ShareDialog({ trip, open, onClose }: ShareDialogProps) {
  const isOwner = trip.myRole === 'OWNER'
  return (
    <Modal open={open} onClose={onClose} size="lg" title={isOwner ? 'Chia sẻ chuyến đi' : 'Thành viên chuyến đi'}>
      <MemberList members={trip.members} />
    </Modal>
  )
}
