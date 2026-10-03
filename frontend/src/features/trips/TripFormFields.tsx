import { useId, useState, type ReactNode } from 'react'
import { MapPin } from 'lucide-react'
import { useWatch, type UseFormReturn } from 'react-hook-form'
import { FieldShell } from '../../components/FieldShell'
import { describedBy } from '../../components/fieldStyles'
import { FormField } from '../../components/FormField'
import { SelectField } from '../../components/SelectField'
import { TextAreaField } from '../../components/TextAreaField'
import { countDays } from '../../lib/format'
import { currencyOptions } from '../../lib/validation'
import type { PlaceResult } from '../../types/place'
import { PlaceSearchField } from '../places/PlaceSearchField'
import { DESCRIPTION_MAX_LENGTH, MAX_TRIP_DAYS, type TripValues } from './schemas'

// Field groups shared by the create wizard (one group per step) and the edit dialog (all groups at once)

interface SectionProps {
  form: UseFormReturn<TripValues>
  autoFocus?: boolean
}

export function TripInfoFields({ form, autoFocus }: SectionProps) {
  const { register, formState: { errors } } = form
  return (
    <div className="space-y-4">
      <FormField label="Tên chuyến đi" required autoFocus={autoFocus} error={errors.title?.message} {...register('title')} />
      <TextAreaField
        label="Mô tả"
        hint={`Tối đa ${DESCRIPTION_MAX_LENGTH} ký tự`}
        maxLength={DESCRIPTION_MAX_LENGTH}
        error={errors.description?.message}
        {...register('description')}
      />
      <FormField
        label="Đường dẫn ảnh bìa"
        type="url"
        placeholder="https://..."
        error={errors.coverImageUrl?.message}
        {...register('coverImageUrl')}
      />
    </div>
  )
}

const POSITION_HINT = 'Tìm một địa điểm ở nơi bạn đến. Vị trí này dùng cho bản đồ và dự báo thời tiết.'

/** "16.0544, 108.2022": four decimals are about 11 m, plenty to recognise a place. */
function formatPosition(lat: number, lng: number): string {
  return `${lat.toFixed(4)}, ${lng.toFixed(4)}`
}

/**
 * The destination has two parts (design.md 15): a name the user types, and a position picked from the place
 * search. The search result is only read for its coordinates; nothing is stored as a place. The name stays
 * free text because a source of places knows landmarks, not "Đà Nẵng" as a whole.
 */
export function TripDestinationFields({ form, autoFocus }: SectionProps) {
  const { register, control, setValue, getValues, clearErrors, formState } = form
  const { errors, defaultValues } = formState
  const positionId = useId()
  const [lat, lng] = useWatch({ control, name: ['destinationLat', 'destinationLng'] })
  // Known only for a position picked in this form; a saved position has coordinates but no name
  const [pickedName, setPickedName] = useState<string | null>(null)
  // PATCH cannot clear a field yet (design.md 10.2): a position that is already saved can be moved, not removed
  const hasSavedPosition = defaultValues?.destinationLat != null
  const positionError = errors.destinationLat?.message ?? errors.destinationLng?.message

  function setPosition(result: PlaceResult) {
    setValue('destinationLat', result.lat, { shouldDirty: true })
    setValue('destinationLng', result.lng, { shouldDirty: true })
    clearErrors(['destinationLat', 'destinationLng'])
    setPickedName(result.name)
    // Offer the name of the place as the name of the destination; a name already typed is never touched
    if (getValues('destinationName').trim() === '') {
      setValue('destinationName', result.name, { shouldDirty: true, shouldValidate: true })
    }
  }

  function clearPosition() {
    setValue('destinationLat', null, { shouldDirty: true })
    setValue('destinationLng', null, { shouldDirty: true })
    setPickedName(null)
  }

  return (
    <div className="space-y-4">
      <FormField
        label="Tên điểm đến"
        autoFocus={autoFocus}
        placeholder="Ví dụ: Đà Lạt"
        error={errors.destinationName?.message}
        {...register('destinationName')}
      />
      <FieldShell id={positionId} label="Vị trí trên bản đồ" hint={POSITION_HINT} error={positionError}>
        <PlaceSearchField
          id={positionId}
          placeholder="Tìm một địa điểm ở nơi bạn đến"
          invalid={Boolean(positionError)}
          describedBy={describedBy(positionId, positionError, POSITION_HINT)}
          onPick={setPosition}
        />
        {lat != null && lng != null && (
          <p className="flex flex-wrap items-center gap-x-2 gap-y-1 text-[13px] leading-5 text-gray-700">
            <MapPin aria-hidden className="size-3.5 shrink-0 text-jade" />
            {/* The name is gone after leaving and coming back to this step of the wizard: say what is known */}
            <span className="min-w-0 wrap-anywhere">
              {pickedName ?? (hasSavedPosition ? 'Vị trí đã lưu' : 'Đã chọn vị trí')}
            </span>
            <span className="tabular text-gray-500">{formatPosition(lat, lng)}</span>
            {!hasSavedPosition && (
              <button
                type="button"
                onClick={clearPosition}
                className="rounded-control font-medium text-jade hover:underline focus-visible:ring-[3px] focus-visible:ring-jade/25 focus-visible:outline-none"
              >
                Bỏ vị trí
              </button>
            )}
          </p>
        )}
      </FieldShell>
    </div>
  )
}

interface DateSectionProps extends SectionProps {
  /** Extra guidance under the dates, e.g. what an edit will do to existing days */
  hint?: ReactNode
}

export function TripDateFields({ form, autoFocus, hint }: DateSectionProps) {
  const { register, control, formState: { errors } } = form
  const [startDate, endDate, currency] = useWatch({ control, name: ['startDate', 'endDate', 'currency'] })
  const dayCount = startDate && endDate && endDate >= startDate ? countDays(startDate, endDate) : null

  return (
    <div className="space-y-4">
      <div className="grid gap-4 sm:grid-cols-2">
        <FormField label="Ngày bắt đầu" required type="date" autoFocus={autoFocus} error={errors.startDate?.message} {...register('startDate')} />
        <FormField label="Ngày kết thúc" required type="date" min={startDate || undefined} error={errors.endDate?.message} {...register('endDate')} />
      </div>
      <p className="text-sm text-gray-500">
        {dayCount ? `Chuyến đi dài ${dayCount} ngày. ` : ''}Tối đa {MAX_TRIP_DAYS} ngày.
      </p>
      {hint}
      <div className="grid gap-4 sm:grid-cols-[1fr_8rem]">
        <FormField
          label="Ngân sách"
          inputMode="decimal"
          placeholder="Ví dụ: 5000000"
          error={errors.budgetAmount?.message}
          {...register('budgetAmount')}
        />
        <SelectField
          label="Tiền tệ"
          options={currencyOptions(currency)}
          error={errors.currency?.message}
          {...register('currency')}
        />
      </div>
    </div>
  )
}
