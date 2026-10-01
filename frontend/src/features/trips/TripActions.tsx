import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { Pencil, Trash2 } from 'lucide-react'
import { deleteTrip } from '../../api/trips'
import { getErrorMessage } from '../../api/errors'
import { Button } from '../../components/Button'
import { ConfirmDialog } from '../../components/ConfirmDialog'
import { useAuthStore } from '../../stores/authStore'
import { toast } from '../../stores/toastStore'
import type { TripResponse } from '../../types/trip'
import { EditTripDialog } from './EditTripDialog'

/** "Sửa" / "Xoá" buttons of the trip detail header. Delete is owner-only (design.md 6.2). */
export function TripActions({ trip }: { trip: TripResponse }) {
  const [dialog, setDialog] = useState<'edit' | 'delete' | null>(null)
  const isOwner = useAuthStore((s) => s.user?.id === trip.ownerId)
  const navigate = useNavigate()
  const queryClient = useQueryClient()

  const deletion = useMutation({
    mutationFn: () => deleteTrip(trip.id),
    onSuccess: () => {
      // Leave first: removing the query while this page still observes it would refetch a deleted trip
      navigate('/trips', { replace: true })
      queryClient.removeQueries({ queryKey: ['trip', trip.id] })
      void queryClient.invalidateQueries({ queryKey: ['trips'] })
      toast.success('Đã xoá chuyến đi')
    },
  })

  return (
    <div className="flex gap-2">
      <Button variant="secondary" fullWidth={false} onClick={() => setDialog('edit')}>
        <Pencil aria-hidden className="size-4" />
        Sửa
      </Button>
      {isOwner && (
        <Button variant="danger" fullWidth={false} onClick={() => setDialog('delete')}>
          <Trash2 aria-hidden className="size-4" />
          Xoá
        </Button>
      )}

      <EditTripDialog trip={trip} open={dialog === 'edit'} onClose={() => setDialog(null)} />

      <ConfirmDialog
        open={dialog === 'delete'}
        title="Xoá chuyến đi?"
        confirmLabel="Xoá chuyến đi"
        variant="danger"
        isLoading={deletion.isPending}
        error={deletion.isError ? getErrorMessage(deletion.error) : undefined}
        onCancel={() => {
          deletion.reset()
          setDialog(null)
        }}
        onConfirm={() => deletion.mutate()}
      >
        <p>
          Chuyến đi <strong className="text-ink">{trip.title}</strong> cùng mọi ngày và hoạt động của nó sẽ bị
          xoá khỏi danh sách của bạn.
        </p>
      </ConfirmDialog>
    </div>
  )
}
