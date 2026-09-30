import { useState, type FormEvent } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { createTrip } from '../../api/trips'
import { applyFieldErrors, getApiError, getErrorMessage } from '../../api/errors'
import { Alert } from '../../components/Alert'
import { Button } from '../../components/Button'
import { toCreateTripRequest, tripSchema, type TripValues } from './schemas'
import { TripDateFields, TripDestinationFields, TripInfoFields } from './TripFormFields'

// design.md 15: info → destination (map picker added in Task 3.4) → dates
const STEPS = [
  { title: 'Thông tin', fields: ['title', 'description', 'coverImageUrl'] },
  { title: 'Điểm đến', fields: ['destinationName'] },
  { title: 'Ngày đi', fields: ['startDate', 'endDate', 'budgetAmount', 'currency'] },
] as const satisfies readonly { title: string; fields: readonly (keyof TripValues)[] }[]

const ALL_FIELDS = STEPS.flatMap((step) => step.fields)
const LAST_STEP = STEPS.length - 1

/** One form across three steps; "Tiếp" validates only the fields of the current step. */
export function CreateTripWizard() {
  const [step, setStep] = useState(0)
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const form = useForm<TripValues>({
    mode: 'onTouched',
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

  const { handleSubmit, trigger, setError } = form

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

  return (
    <form noValidate className="space-y-6" onSubmit={handleFormSubmit}>
      <ol className="flex gap-2 text-sm" aria-label="Các bước">
        {STEPS.map((s, index) => (
          <li
            key={s.title}
            aria-current={index === step ? 'step' : undefined}
            className={`flex-1 rounded-control border px-3 py-2 text-center ${
              index === step
                ? 'border-jade bg-jade-light font-medium text-jade-dark'
                : index < step
                  ? 'border-gray-200 bg-white text-gray-600'
                  : 'border-gray-200 bg-gray-50 text-gray-400'
            }`}
          >
            {index + 1}. {s.title}
          </li>
        ))}
      </ol>

      {mutation.isError && <Alert variant="error">{getErrorMessage(mutation.error)}</Alert>}

      {step === 0 && <TripInfoFields form={form} autoFocus />}
      {step === 1 && <TripDestinationFields form={form} autoFocus />}
      {step === 2 && <TripDateFields form={form} autoFocus />}

      <div className="flex justify-between gap-3">
        {step > 0 ? (
          <Button variant="secondary" fullWidth={false} onClick={() => setStep(step - 1)}>
            Quay lại
          </Button>
        ) : (
          <Button variant="secondary" fullWidth={false} onClick={() => navigate('/trips')}>
            Huỷ
          </Button>
        )}
        <Button type="submit" fullWidth={false} isLoading={mutation.isPending}>
          {step < LAST_STEP ? 'Tiếp' : 'Tạo chuyến đi'}
        </Button>
      </div>
    </form>
  )
}
