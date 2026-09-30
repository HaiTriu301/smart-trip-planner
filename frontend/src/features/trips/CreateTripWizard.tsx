import { useState, type FormEvent } from 'react'
import { useForm, useWatch } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { createTrip } from '../../api/trips'
import { applyFieldErrors, getApiError, getErrorMessage } from '../../api/errors'
import { Alert } from '../../components/Alert'
import { Button } from '../../components/Button'
import { FormField } from '../../components/FormField'
import { SelectField } from '../../components/SelectField'
import { TextAreaField } from '../../components/TextAreaField'
import { countDays } from '../../lib/format'
import { CURRENCIES, MAX_TRIP_DAYS, toCreateTripRequest, tripSchema, type TripValues } from './schemas'

// design.md 15: info → destination (map picker added in Task 3.4) → dates
const STEPS = [
  { title: 'Thông tin', fields: ['title', 'description', 'coverImageUrl'] },
  { title: 'Điểm đến', fields: ['destinationName'] },
  { title: 'Ngày đi', fields: ['startDate', 'endDate', 'budgetAmount', 'currency'] },
] as const satisfies readonly { title: string; fields: readonly (keyof TripValues)[] }[]

const ALL_FIELDS = STEPS.flatMap((step) => step.fields)
const LAST_STEP = STEPS.length - 1
const CURRENCY_OPTIONS = CURRENCIES.map((code) => ({ value: code, label: code }))

/** One form across three steps; "Tiếp" validates only the fields of the current step. */
export function CreateTripWizard() {
  const [step, setStep] = useState(0)
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const {
    register,
    handleSubmit,
    trigger,
    setError,
    control,
    formState: { errors },
  } = useForm<TripValues>({
    resolver: zodResolver(tripSchema),
    defaultValues: {
      title: '',
      description: '',
      coverImageUrl: '',
      destinationName: '',
      startDate: '',
      endDate: '',
      budgetAmount: '',
      currency: 'VND',
    },
  })

  const mutation = useMutation({
    mutationFn: createTrip,
    onSuccess: async (trip) => {
      await queryClient.invalidateQueries({ queryKey: ['trips'] })
      navigate(`/trips/${trip.id}`)
    },
    onError: (error) => {
      if (!applyFieldErrors(error, setError, ALL_FIELDS)) return
      // A server-side field error may belong to an earlier step: go back to the first one
      const fields = getApiError(error)?.details.map((d) => d.field) ?? []
      const target = STEPS.findIndex((s) => s.fields.some((f) => fields.includes(f)))
      if (target >= 0) setStep(target)
    },
  })

  async function goNext() {
    if (await trigger(STEPS[step].fields)) setStep(step + 1)
  }

  // Enter in any input submits the form: advance on intermediate steps, create on the last one
  const onSubmit = handleSubmit((values) => mutation.mutate(toCreateTripRequest(values)))
  function handleFormSubmit(event: FormEvent<HTMLFormElement>) {
    if (step < LAST_STEP) {
      event.preventDefault()
      void goNext()
      return
    }
    void onSubmit(event)
  }

  const [startDate, endDate] = useWatch({ control, name: ['startDate', 'endDate'] })
  const dayCount = startDate && endDate && endDate >= startDate ? countDays(startDate, endDate) : null

  return (
    <form noValidate className="space-y-6" onSubmit={handleFormSubmit}>
      <ol className="flex gap-2 text-sm" aria-label="Các bước">
        {STEPS.map((s, index) => (
          <li
            key={s.title}
            aria-current={index === step ? 'step' : undefined}
            className={`flex-1 rounded-lg border px-3 py-2 text-center ${
              index === step
                ? 'border-sky-500 bg-sky-50 font-medium text-sky-800'
                : index < step
                  ? 'border-slate-200 bg-white text-slate-600'
                  : 'border-slate-200 bg-slate-50 text-slate-400'
            }`}
          >
            {index + 1}. {s.title}
          </li>
        ))}
      </ol>

      {mutation.isError && <Alert variant="error">{getErrorMessage(mutation.error)}</Alert>}

      {step === 0 && (
        <div className="space-y-4">
          <FormField label="Tên chuyến đi" autoFocus error={errors.title?.message} {...register('title')} />
          <TextAreaField label="Mô tả (không bắt buộc)" error={errors.description?.message} {...register('description')} />
          <FormField
            label="Đường dẫn ảnh bìa (không bắt buộc)"
            type="url"
            placeholder="https://..."
            error={errors.coverImageUrl?.message}
            {...register('coverImageUrl')}
          />
        </div>
      )}

      {step === 1 && (
        <div className="space-y-4">
          <FormField
            label="Điểm đến (không bắt buộc)"
            autoFocus
            placeholder="Ví dụ: Đà Lạt"
            error={errors.destinationName?.message}
            {...register('destinationName')}
          />
          <p className="text-sm text-slate-500">Chọn vị trí trên bản đồ sẽ có ở phiên bản sau.</p>
        </div>
      )}

      {step === 2 && (
        <div className="space-y-4">
          <div className="grid gap-4 sm:grid-cols-2">
            <FormField label="Ngày bắt đầu" type="date" autoFocus error={errors.startDate?.message} {...register('startDate')} />
            <FormField label="Ngày kết thúc" type="date" min={startDate || undefined} error={errors.endDate?.message} {...register('endDate')} />
          </div>
          <p className="text-sm text-slate-500">
            {dayCount ? `Chuyến đi dài ${dayCount} ngày. ` : ''}Tối đa {MAX_TRIP_DAYS} ngày.
          </p>
          <div className="grid gap-4 sm:grid-cols-[1fr_8rem]">
            <FormField
              label="Ngân sách (không bắt buộc)"
              inputMode="decimal"
              placeholder="Ví dụ: 5000000"
              error={errors.budgetAmount?.message}
              {...register('budgetAmount')}
            />
            <SelectField label="Tiền tệ" options={CURRENCY_OPTIONS} error={errors.currency?.message} {...register('currency')} />
          </div>
        </div>
      )}

      <div className="flex justify-between gap-3">
        {step > 0 ? (
          <Button variant="secondary" className="w-auto" onClick={() => setStep(step - 1)}>
            Quay lại
          </Button>
        ) : (
          <Button variant="secondary" className="w-auto" onClick={() => navigate('/trips')}>
            Huỷ
          </Button>
        )}
        <Button type="submit" className="w-auto" isLoading={mutation.isPending}>
          {step < LAST_STEP ? 'Tiếp' : 'Tạo chuyến đi'}
        </Button>
      </div>
    </form>
  )
}
