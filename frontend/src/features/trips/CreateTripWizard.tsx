import { useState, type FormEvent } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { Check } from 'lucide-react'
import { createTrip } from '../../api/trips'
import { applyFieldErrors, getApiError, getErrorMessage } from '../../api/errors'
import { Alert } from '../../components/Alert'
import { Button } from '../../components/Button'
import { toCreateTripRequest, tripSchema, type TripValues } from './schemas'
import { TripDateFields, TripDestinationFields, TripInfoFields } from './TripFormFields'

// design.md 15: info → destination (name typed, position picked from place search) → dates
const STEPS = [
  { title: 'Thông tin', fields: ['title', 'description', 'coverImageUrl'] },
  { title: 'Điểm đến', fields: ['destinationName', 'destinationLat', 'destinationLng'] },
  { title: 'Ngày đi', fields: ['startDate', 'endDate', 'budgetAmount', 'currency'] },
] as const satisfies readonly { title: string; fields: readonly (keyof TripValues)[] }[]

const ALL_FIELDS = STEPS.flatMap((step) => step.fields)
const LAST_STEP = STEPS.length - 1

/**
 * The steps drawn as a short transit line (UI_GUIDE 1): a numbered station per step, the track turns jade up to
 * the current one, and passed stations get a check.
 */
function StepRail({ current }: { current: number }) {
  return (
    <ol aria-label="Các bước" className="flex items-start">
      {STEPS.map((s, index) => {
        const done = index < current
        const active = index === current
        return (
          <li
            key={s.title}
            aria-current={active ? 'step' : undefined}
            className="relative flex flex-1 flex-col items-center gap-1.5 text-center"
          >
            {index > 0 && (
              <span
                aria-hidden
                className={`absolute top-3.5 right-1/2 left-[-50%] h-0.5 ${index <= current ? 'bg-jade' : 'bg-tide'}`}
              />
            )}
            <span
              className={`tabular relative flex size-7 items-center justify-center rounded-full border-2 text-[13px] font-semibold ${
                done
                  ? 'border-jade bg-jade text-white'
                  : active
                    ? 'border-jade bg-white text-jade-dark'
                    : 'border-gray-300 bg-white text-gray-500'
              }`}
            >
              {done ? <Check aria-hidden className="size-4" strokeWidth={3} /> : index + 1}
            </span>
            <span className={`text-[13px] ${active ? 'font-semibold text-ink' : 'text-gray-600'}`}>
              {s.title}
              {done && <span className="sr-only"> (đã xong)</span>}
            </span>
          </li>
        )
      })}
    </ol>
  )
}

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
      destinationLat: null,
      destinationLng: null,
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
      <StepRail current={step} />

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
