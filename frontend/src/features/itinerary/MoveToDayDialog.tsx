import { useState } from 'react'
import { Button } from '../../components/Button'
import { Modal } from '../../components/Modal'
import { SelectField } from '../../components/SelectField'
import { formatDate, formatWeekday } from '../../lib/format'
import type { Activity } from '../../types/activity'
import type { TripDayDetail } from '../../types/trip'

interface MoveToDayDialogProps {
  /** The activity to move; null keeps the dialog closed */
  activity: Activity | null
  /** Every day of the trip; the activity's own day is left out of the choices */
  days: TripDayDetail[]
  onMove: (activityId: number, dayId: number) => void
  onClose: () => void
}

/**
 * Menu "⋮" → "Chuyển sang ngày…": works on every device, unlike dropping on the day list. The activity goes to the
 * end of the chosen day through the reorder save path (overlap question, rollback on error, toast with a link).
 */
export function MoveToDayDialog({ activity, days, onMove, onClose }: MoveToDayDialogProps) {
  return (
    <Modal open={activity !== null} title="Chuyển sang ngày khác" onClose={onClose}>
      {activity && <MoveToDayForm activity={activity} days={days} onMove={onMove} onClose={onClose} />}
    </Modal>
  )
}

function MoveToDayForm({ activity, days, onMove, onClose }: MoveToDayDialogProps & { activity: Activity }) {
  const choices = days.filter((d) => d.id !== activity.dayId)
  const [dayId, setDayId] = useState(String(choices[0]?.id ?? ''))

  return (
    <form
      noValidate
      className="space-y-4"
      onSubmit={(event) => {
        event.preventDefault()
        onMove(activity.id, Number(dayId))
        onClose()
      }}
    >
      <p className="text-gray-600">
        Chuyển <strong className="text-ink">{activity.title}</strong> xuống cuối ngày được chọn.
      </p>
      <SelectField
        label="Ngày"
        autoFocus
        value={dayId}
        onChange={(e) => setDayId(e.target.value)}
        options={choices.map((d) => ({
          value: String(d.id),
          label: `Ngày ${d.dayIndex} · ${formatWeekday(d.date)}, ${formatDate(d.date)}${d.title ? ` · ${d.title}` : ''}`,
        }))}
      />
      <div className="flex justify-end gap-2">
        <Button variant="secondary" fullWidth={false} onClick={onClose}>
          Huỷ
        </Button>
        <Button type="submit" fullWidth={false}>
          Chuyển sang ngày
        </Button>
      </div>
    </form>
  )
}
