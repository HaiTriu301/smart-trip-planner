import type { ReactNode } from 'react'
import { useWatch, type UseFormReturn } from 'react-hook-form'
import { FormField } from '../../components/FormField'
import { SelectField } from '../../components/SelectField'
import { TextAreaField } from '../../components/TextAreaField'
import { countDays } from '../../lib/format'
import { currencyOptions } from '../../lib/validation'
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
      <FormField label="Tên chuyến đi" autoFocus={autoFocus} error={errors.title?.message} {...register('title')} />
      <TextAreaField
        label={`Mô tả (không bắt buộc, tối đa ${DESCRIPTION_MAX_LENGTH} ký tự)`}
        maxLength={DESCRIPTION_MAX_LENGTH}
        error={errors.description?.message}
        {...register('description')}
      />
      <FormField
        label="Đường dẫn ảnh bìa (không bắt buộc)"
        type="url"
        placeholder="https://..."
        error={errors.coverImageUrl?.message}
        {...register('coverImageUrl')}
      />
    </div>
  )
}

export function TripDestinationFields({ form, autoFocus }: SectionProps) {
  const { register, formState: { errors } } = form
  return (
    <div className="space-y-2">
      <FormField
        label="Điểm đến (không bắt buộc)"
        autoFocus={autoFocus}
        placeholder="Ví dụ: Đà Lạt"
        error={errors.destinationName?.message}
        {...register('destinationName')}
      />
      <p className="text-sm text-gray-500">Chọn vị trí trên bản đồ sẽ có ở phiên bản sau.</p>
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
        <FormField label="Ngày bắt đầu" type="date" autoFocus={autoFocus} error={errors.startDate?.message} {...register('startDate')} />
        <FormField label="Ngày kết thúc" type="date" min={startDate || undefined} error={errors.endDate?.message} {...register('endDate')} />
      </div>
      <p className="text-sm text-gray-500">
        {dayCount ? `Chuyến đi dài ${dayCount} ngày. ` : ''}Tối đa {MAX_TRIP_DAYS} ngày.
      </p>
      {hint}
      <div className="grid gap-4 sm:grid-cols-[1fr_8rem]">
        <FormField
          label="Ngân sách (không bắt buộc)"
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
