import { useMutation, useQueryClient } from '@tanstack/react-query'
import { updateTripStatus } from '../../api/trips'
import { getErrorMessage } from '../../api/errors'
import { toast } from '../../stores/toastStore'
import type { TripResponse, TripStatus } from '../../types/trip'
import { badgeToneClass } from '../../components/badgeStyles'
import { isTripStatus, TRIP_STATUSES, TRIP_STATUS_LABELS, TRIP_STATUS_TONES } from './tripStatus'

/** Status badge that is also a select: picking a value saves it right away through PATCH /trips/{id}/status. */
export function TripStatusSelect({ trip }: { trip: TripResponse }) {
  const queryClient = useQueryClient()
  const mutation = useMutation({
    mutationFn: (status: TripStatus) => updateTripStatus(trip.id, status),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['trip', trip.id] }),
        queryClient.invalidateQueries({ queryKey: ['trips'] }),
      ])
      toast.success('Đã đổi trạng thái')
    },
    onError: (error) => toast.error(`Không đổi được trạng thái: ${getErrorMessage(error)}`),
  })

  // While saving, show the chosen value instead of jumping back to the stored one
  const shown = mutation.isPending && mutation.variables ? mutation.variables : trip.status

  return (
    <select
      aria-label="Trạng thái chuyến đi"
      value={shown}
      disabled={mutation.isPending}
      onChange={(e) => {
        if (isTripStatus(e.target.value)) mutation.mutate(e.target.value)
      }}
      className={`h-7 cursor-pointer rounded-control border-0 pr-7 pl-2.5 text-xs font-medium outline-none focus-visible:ring-[3px] focus-visible:ring-jade/25 disabled:cursor-wait ${badgeToneClass(TRIP_STATUS_TONES[shown])}`}
    >
      {TRIP_STATUSES.map((value) => (
        <option key={value} value={value}>
          {TRIP_STATUS_LABELS[value]}
        </option>
      ))}
    </select>
  )
}
