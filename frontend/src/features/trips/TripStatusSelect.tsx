import { useMutation, useQueryClient } from '@tanstack/react-query'
import { updateTripStatus } from '../../api/trips'
import { getErrorMessage } from '../../api/errors'
import type { TripResponse, TripStatus } from '../../types/trip'
import { isTripStatus, TRIP_STATUSES, TRIP_STATUS_LABELS, TRIP_STATUS_STYLES } from './tripStatus'

/** Status badge that is also a select: picking a value saves it right away through PATCH /trips/{id}/status. */
export function TripStatusSelect({ trip }: { trip: TripResponse }) {
  const queryClient = useQueryClient()
  const mutation = useMutation({
    mutationFn: (status: TripStatus) => updateTripStatus(trip.id, status),
    onSuccess: () =>
      Promise.all([
        queryClient.invalidateQueries({ queryKey: ['trip', trip.id] }),
        queryClient.invalidateQueries({ queryKey: ['trips'] }),
      ]),
  })

  // While saving, show the chosen value instead of jumping back to the stored one
  const shown = mutation.isPending && mutation.variables ? mutation.variables : trip.status

  return (
    <div className="flex items-center gap-2">
      <select
        aria-label="Trạng thái chuyến đi"
        value={shown}
        disabled={mutation.isPending}
        onChange={(e) => {
          if (isTripStatus(e.target.value)) mutation.mutate(e.target.value)
        }}
        className={`cursor-pointer rounded-full border-0 py-0.5 pl-2 pr-7 text-xs font-medium outline-none focus-visible:ring-2 focus-visible:ring-sky-400 disabled:cursor-wait ${TRIP_STATUS_STYLES[shown]}`}
      >
        {TRIP_STATUSES.map((value) => (
          <option key={value} value={value}>
            {TRIP_STATUS_LABELS[value]}
          </option>
        ))}
      </select>
      {mutation.isError && (
        <span role="alert" className="text-xs text-red-600">
          {getErrorMessage(mutation.error)}
        </span>
      )}
    </div>
  )
}
