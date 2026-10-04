import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { updateTripStatus } from '../../api/trips'
import { getErrorMessage } from '../../api/errors'
import { ConfirmDialog } from '../../components/ConfirmDialog'
import { useToday } from '../../hooks/useToday'
import { formatDate } from '../../lib/format'
import { shouldAskToComplete } from '../../lib/tripDates'
import { toast } from '../../stores/toastStore'
import type { TripResponse } from '../../types/trip'

/**
 * Asks whether a trip that is over is completed (design.md rule 14.22, UI_GUIDE 8.1). The status of a trip
 * never changes by itself: a trip past its last day and still Nháp, Đã lên kế hoạch or Đang diễn ra gets this
 * question when it is opened. Yes sets the status to COMPLETED; "Để sau", Esc and "×" leave it as it is.
 * Completing locks nothing: the itinerary stays editable.
 */
export function CompleteTripPrompt({ trip }: { trip: TripResponse }) {
  const queryClient = useQueryClient()
  const today = useToday()
  const [postponed, setPostponed] = useState(false)

  const mutation = useMutation({
    mutationFn: () => updateTripStatus(trip.id, 'COMPLETED'),
    onSuccess: async () => {
      // Once the trip comes back as completed, the question has no reason to stay and the dialog closes
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['trip', trip.id] }),
        queryClient.invalidateQueries({ queryKey: ['trips'] }),
      ])
      toast.success('Đã hoàn thành chuyến đi')
    },
  })

  return (
    <ConfirmDialog
      open={shouldAskToComplete(trip, today) && !postponed}
      title="Hoàn thành chuyến đi?"
      confirmLabel="Hoàn thành chuyến đi"
      cancelLabel="Để sau"
      isLoading={mutation.isPending}
      error={mutation.isError ? getErrorMessage(mutation.error) : undefined}
      onCancel={() => setPostponed(true)}
      onConfirm={() => mutation.mutate()}
    >
      <p>
        Chuyến đi <strong className="wrap-anywhere text-ink">{trip.title}</strong> đã kết thúc ngày{' '}
        <span className="tabular">{formatDate(trip.endDate)}</span>. Lịch trình vẫn sửa được sau khi hoàn thành.
      </p>
    </ConfirmDialog>
  )
}
