import { useEffect, useId, useRef, useState, type KeyboardEvent } from 'react'
import { useMutation } from '@tanstack/react-query'
import { addManualPlace } from '../../api/places'
import { getApiError, getErrorMessage } from '../../api/errors'
import { Button } from '../../components/Button'
import { FormField } from '../../components/FormField'
import { PointPicker } from '../../components/map/PointPicker'
import type { Coordinates, Place } from '../../types/place'

// Same limit as the backend (design.md 10.2 "Quy ước Place API")
const NAME_MAX_LENGTH = 200

interface ManualPlaceFormProps {
  /** Destination of the trip: where the map opens; null when the trip has no position */
  around: Coordinates | null
  onAdded: (place: Place) => void
  onCancel: () => void
}

/**
 * "Tự thêm địa điểm" (UI_GUIDE 7.12): a name and a point clicked on the map, for a place the search does not
 * know (somebody's home, a spot on a beach). The place is stored for its creator only (design rule 14.19).
 * <p>
 * It sits inside the activity form, and a form cannot contain another one: this is a group of controls with
 * its own button, and Enter in the name box is caught here so it does not save the activity.
 */
export function ManualPlaceForm({ around, onAdded, onCancel }: ManualPlaceFormProps) {
  const positionHintId = useId()
  const nameRef = useRef<HTMLInputElement>(null)
  const [name, setName] = useState('')
  const [point, setPoint] = useState<Coordinates | null>(null)
  // Errors appear once "Thêm địa điểm" was tried, not while the user is still filling the two parts in
  const [tried, setTried] = useState(false)
  const add = useMutation({ mutationFn: addManualPlace, onSuccess: onAdded })

  useEffect(() => {
    nameRef.current?.focus()
  }, [])

  const trimmedName = name.trim()
  const nameError = tried && trimmedName === '' ? 'Tên địa điểm không được để trống' : undefined
  const pointError = tried && !point ? 'Bấm lên bản đồ để đặt vị trí của địa điểm' : undefined
  // A refusal of the server names the field it is about; the first one is enough for a two-field form
  const serverError = add.isError
    ? (getApiError(add.error)?.details[0]?.message ?? getErrorMessage(add.error))
    : undefined

  function submit() {
    setTried(true)
    if (trimmedName === '' || !point) return
    add.mutate({ name: trimmedName, lat: point.lat, lng: point.lng })
  }

  function handleNameKeyDown(event: KeyboardEvent<HTMLInputElement>) {
    if (event.key !== 'Enter') return
    event.preventDefault()
    submit()
  }

  return (
    <div role="group" aria-label="Tự thêm địa điểm" className="space-y-3 rounded-control border border-tide bg-gray-50 p-3">
      <FormField
        ref={nameRef}
        label="Tên địa điểm"
        required
        maxLength={NAME_MAX_LENGTH}
        placeholder="Ví dụ: Nhà bà ngoại"
        value={name}
        error={nameError}
        onChange={(event) => setName(event.target.value)}
        onKeyDown={handleNameKeyDown}
      />
      <div className="space-y-1.5">
        <p className="text-[13px] leading-[18px] font-medium text-gray-700">
          Vị trí
          <span aria-hidden className="ml-0.5 text-danger">
            *
          </span>
        </p>
        <PointPicker
          value={point}
          around={around}
          onPick={setPoint}
          heightClass="h-60"
          label="Bản đồ chọn vị trí của địa điểm"
        />
        <p id={positionHintId} className={`text-xs leading-4 ${pointError ? 'text-danger' : 'text-gray-500'}`}>
          {pointError ??
            (point ? (
              <>
                Đã đặt vị trí: <span className="tabular">{point.lat.toFixed(4)}, {point.lng.toFixed(4)}</span>. Bấm chỗ
                khác để đổi.
              </>
            ) : (
              'Phóng to rồi bấm lên bản đồ để đặt vị trí.'
            ))}
        </p>
      </div>
      {serverError && (
        <p role="alert" className="text-xs leading-4 text-danger">
          {serverError}
        </p>
      )}
      <div className="flex justify-end gap-2">
        <Button variant="ghost" fullWidth={false} disabled={add.isPending} onClick={onCancel}>
          Quay lại tìm kiếm
        </Button>
        <Button variant="secondary" fullWidth={false} isLoading={add.isPending} onClick={submit}>
          Thêm địa điểm
        </Button>
      </div>
    </div>
  )
}
