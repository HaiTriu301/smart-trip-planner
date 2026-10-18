import * as DropdownMenu from '@radix-ui/react-dropdown-menu'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Check, ChevronDown, LoaderCircle } from 'lucide-react'
import { updateTripStatus } from '../../api/trips'
import { getErrorMessage } from '../../api/errors'
import { toast } from '../../stores/toastStore'
import type { TripResponse, TripStatus } from '../../types/trip'
import { badgeDotClass, badgeToneClass } from '../../components/badgeStyles'
import { MENU_CONTENT, MENU_ITEM } from '../../components/menuStyles'
import { isTripStatus, TRIP_STATUSES, TRIP_STATUS_LABELS, TRIP_STATUS_TONES } from './tripStatus'

/**
 * The status badge of the trip page, with a chevron: it opens a menu of the five statuses, each with a dot in its
 * badge colour and a check on the current one (UI_GUIDE 7.16, Task 4.9 decision 2). Picking one saves it right
 * away through PATCH /trips/{id}/status, as the old select did; picking the current one sends nothing.
 */
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
    <DropdownMenu.Root>
      <DropdownMenu.Trigger asChild>
        <button
          type="button"
          aria-label={`Trạng thái chuyến đi: ${TRIP_STATUS_LABELS[shown]}`}
          disabled={mutation.isPending}
          className={`group inline-flex h-7 cursor-pointer items-center gap-1 rounded-control pr-1.5 pl-2.5 text-xs font-medium outline-none focus-visible:ring-[3px] focus-visible:ring-jade/25 disabled:cursor-wait ${badgeToneClass(TRIP_STATUS_TONES[shown])}`}
        >
          {TRIP_STATUS_LABELS[shown]}
          {mutation.isPending ? (
            <LoaderCircle aria-hidden className="size-3.5 animate-spin" />
          ) : (
            <ChevronDown
              aria-hidden
              className="size-3.5 transition-transform duration-150 group-data-[state=open]:rotate-180 motion-reduce:transition-none"
            />
          )}
        </button>
      </DropdownMenu.Trigger>
      <DropdownMenu.Portal>
        <DropdownMenu.Content align="start" sideOffset={4} className={MENU_CONTENT}>
          <DropdownMenu.RadioGroup
            value={shown}
            onValueChange={(value) => {
              if (isTripStatus(value) && value !== shown) mutation.mutate(value)
            }}
          >
            {TRIP_STATUSES.map((status) => (
              <DropdownMenu.RadioItem key={status} value={status} className={MENU_ITEM}>
                <span aria-hidden className={`size-2 shrink-0 rounded-full ${badgeDotClass(TRIP_STATUS_TONES[status])}`} />
                <span className="flex-1">{TRIP_STATUS_LABELS[status]}</span>
                {/* Keeps its width when empty, so the labels do not shift when the check moves */}
                <span className="flex size-4 shrink-0 items-center justify-center">
                  <DropdownMenu.ItemIndicator>
                    <Check aria-hidden className="size-4 text-jade" />
                  </DropdownMenu.ItemIndicator>
                </span>
              </DropdownMenu.RadioItem>
            ))}
          </DropdownMenu.RadioGroup>
        </DropdownMenu.Content>
      </DropdownMenu.Portal>
    </DropdownMenu.Root>
  )
}
