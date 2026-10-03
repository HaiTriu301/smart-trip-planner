import { useState } from 'react'
import { useWatch, type UseFormReturn } from 'react-hook-form'
import { FormField } from '../../components/FormField'
import { SelectField } from '../../components/SelectField'
import { TextAreaField } from '../../components/TextAreaField'
import { currencyOptions } from '../../lib/validation'
import type { Place } from '../../types/place'
import { ActivityPlaceField } from './ActivityPlaceField'
import { ACTIVITY_TYPES } from './activityType'
import { ActivityTypeField } from './ActivityTypeField'
import { NOTE_MAX_LENGTH, type ActivityValues } from './schemas'

interface ActivityFormFieldsProps {
  form: UseFormReturn<ActivityValues>
  /** The place the activity has when the dialog opens; null for a new activity or one without a place */
  initialPlace: Place | null
  /** A new activity takes the type its place suggests; an existing one keeps the type it was saved with */
  suggestType: boolean
}

/**
 * The inputs of the activity form, in the order the dialog shows them: the place first, as picking one can
 * fill the title and suggest the type. Saving, the overlap question and the
 * buttons stay in ActivityFormDialog; a fragment, so the fields keep the spacing of the form around them.
 */
export function ActivityFormFields({ form, initialPlace, suggestType }: ActivityFormFieldsProps) {
  const { register, control, setValue, getValues, setFocus, clearErrors, formState } = form
  const { errors, touchedFields } = formState
  const currency = useWatch({ control, name: 'currency' })
  // The form holds the id it will send; the name and address to show stay here
  const [place, setPlace] = useState<Place | null>(initialPlace)

  function changePlace(next: Place | null) {
    setPlace(next)
    setValue('placeId', next?.id ?? null, { shouldDirty: true })
    clearErrors('placeId')
    if (!next) return
    // An activity is often named after its place: offer the name, which stays editable; a title already typed
    // is never touched
    if (getValues('title').trim() === '') setValue('title', next.name, { shouldDirty: true, shouldValidate: true })
    // A market suggests "Mua sắm". Only while the user has not been to the type buttons: a type chosen by
    // hand is never overridden. Setting the value here does not count as a visit, so a second place can
    // still change a type that a first place suggested
    const suggested = ACTIVITY_TYPES.find((type) => type === next.category)
    if (suggestType && suggested && !touchedFields.type) setValue('type', suggested, { shouldDirty: true })
    setFocus('title')
  }

  return (
    <>
      <ActivityPlaceField place={place} error={errors.placeId?.message} onChange={changePlace} />
      <FormField label="Tên hoạt động" required error={errors.title?.message} {...register('title')} />
      <ActivityTypeField registration={register('type')} error={errors.type?.message} />
      <div className="grid gap-4 sm:grid-cols-2">
        <FormField label="Giờ bắt đầu" type="time" error={errors.startTime?.message} {...register('startTime')} />
        <FormField label="Giờ kết thúc" type="time" error={errors.endTime?.message} {...register('endTime')} />
      </div>
      <div className="grid gap-4 sm:grid-cols-[1fr_8rem]">
        <FormField
          label="Chi phí"
          inputMode="decimal"
          placeholder="Ví dụ: 350000"
          error={errors.costAmount?.message}
          {...register('costAmount')}
        />
        <SelectField
          label="Tiền tệ"
          options={currencyOptions(currency)}
          error={errors.currency?.message}
          {...register('currency')}
        />
      </div>
      <FormField
        label="Link đặt chỗ"
        type="url"
        placeholder="https://..."
        error={errors.bookingUrl?.message}
        {...register('bookingUrl')}
      />
      <TextAreaField
        label="Ghi chú"
        hint={`Tối đa ${NOTE_MAX_LENGTH} ký tự`}
        maxLength={NOTE_MAX_LENGTH}
        error={errors.note?.message}
        {...register('note')}
      />
    </>
  )
}
