import type { ReactNode } from 'react'
import { Alert } from './Alert'
import { Button } from './Button'
import { Modal } from './Modal'

interface ConfirmDialogProps {
  open: boolean
  title: string
  children: ReactNode
  confirmLabel: string
  variant?: 'primary' | 'danger'
  isLoading?: boolean
  /** Shown above the buttons, e.g. when the confirmed action itself failed */
  error?: string
  onConfirm: () => void
  onCancel: () => void
}

/** Yes/no question before an action that loses data or needs a second thought. */
export function ConfirmDialog({
  open,
  title,
  children,
  confirmLabel,
  variant = 'primary',
  isLoading = false,
  error,
  onConfirm,
  onCancel,
}: ConfirmDialogProps) {
  return (
    <Modal open={open} title={title} onClose={() => !isLoading && onCancel()}>
      <div className="space-y-2 text-gray-600">{children}</div>
      {error && <Alert variant="error">{error}</Alert>}
      <div className="flex justify-end gap-2">
        <Button variant="secondary" fullWidth={false} disabled={isLoading} onClick={onCancel}>
          Huỷ
        </Button>
        {/* The confirm button of a destructive dialog is the only red-filled button (UI_GUIDE 7.1) */}
        <Button
          variant={variant === 'danger' ? 'danger-solid' : 'primary'}
          fullWidth={false}
          isLoading={isLoading}
          onClick={onConfirm}
        >
          {confirmLabel}
        </Button>
      </div>
    </Modal>
  )
}
