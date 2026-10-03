import { useId, useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import { MapPin, X } from 'lucide-react'
import { pickPlace } from '../../api/places'
import { getErrorMessage } from '../../api/errors'
import { FieldShell } from '../../components/FieldShell'
import { describedBy } from '../../components/fieldStyles'
import type { Coordinates, Place } from '../../types/place'
import { PlaceSearchField } from '../places/PlaceSearchField'

const HINT = 'Tìm theo tên hoặc địa chỉ, từ 2 ký tự.'

interface ActivityPlaceFieldProps {
  /** The place the form holds now; null: none, the search box is shown */
  place: Place | null
  /** Error of the form for this field, e.g. the server refused the place */
  error?: string
  /** Destination of the trip: suggestions around it come first; null when the trip has no position */
  near: Coordinates | null
  onChange: (place: Place | null) => void
}

/**
 * "Địa điểm" of the activity form (UI_GUIDE 7.12). Without a place: the search box. Picking a suggestion stores
 * it on the server right away (POST /places), because an activity refers to a place by its id. With a place:
 * a box with its name and address, and "×" to go back to the search box.
 */
export function ActivityPlaceField({ place, error, near, onChange }: ActivityPlaceFieldProps) {
  const id = useId()
  // After "×" the search box takes the cursor; when the dialog opens, Modal places it by itself
  const [removedHere, setRemovedHere] = useState(false)
  const pick = useMutation({ mutationFn: pickPlace, onSuccess: onChange })
  const shownError = error ?? (pick.isError ? getErrorMessage(pick.error) : undefined)
  const hint = pick.isPending ? 'Đang lưu địa điểm…' : HINT

  return (
    <FieldShell id={id} label="Địa điểm" hint={place ? undefined : hint} error={shownError}>
      {place ? (
        <div id={id} className="flex items-start gap-2 rounded-control border border-tide bg-gray-50 py-2.5 pr-1.5 pl-3">
          <MapPin aria-hidden className="mt-0.5 size-4 shrink-0 text-jade" />
          <div className="min-w-0 flex-1">
            <p className="text-[15px] leading-5 font-medium wrap-anywhere text-ink">{place.name}</p>
            {place.address && <p className="text-[13px] leading-5 wrap-anywhere text-gray-600">{place.address}</p>}
          </div>
          <button
            type="button"
            aria-label={`Bỏ địa điểm ${place.name}`}
            // Not where the dialog puts the cursor when it opens: Enter there would remove the place
            data-no-initial-focus
            onClick={() => {
              setRemovedHere(true)
              onChange(null)
            }}
            className="flex size-7 shrink-0 items-center justify-center rounded-control text-gray-500 hover:bg-gray-200 hover:text-gray-900 focus-visible:ring-[3px] focus-visible:ring-jade/25 focus-visible:outline-none pointer-coarse:size-11"
          >
            <X aria-hidden className="size-4" />
          </button>
        </div>
      ) : (
        <PlaceSearchField
          id={id}
          placeholder="Ví dụ: Chùa Linh Ứng"
          invalid={Boolean(shownError)}
          describedBy={describedBy(id, shownError, hint)}
          disabled={pick.isPending}
          focusOnMount={removedHere}
          near={near}
          onPick={(result) => pick.mutate(result)}
        />
      )}
    </FieldShell>
  )
}
