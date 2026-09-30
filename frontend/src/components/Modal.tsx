import { useEffect, useId, useRef, type ReactNode } from 'react'

interface ModalProps {
  open: boolean
  title: string
  onClose: () => void
  children: ReactNode
}

/**
 * Native <dialog> opened with showModal(): the browser provides the backdrop, focus trap and Esc key.
 * Children mount only while open, so a form inside starts fresh every time.
 */
export function Modal({ open, title, onClose, children }: ModalProps) {
  const ref = useRef<HTMLDialogElement>(null)
  const titleId = useId()

  useEffect(() => {
    const dialog = ref.current
    if (!dialog) return
    if (open && !dialog.open) dialog.showModal()
    if (!open && dialog.open) dialog.close()
  }, [open])

  return (
    <dialog
      ref={ref}
      aria-labelledby={titleId}
      // Esc: let the parent decide (it may be busy saving) instead of the browser closing on its own
      onCancel={(event) => {
        event.preventDefault()
        onClose()
      }}
      className="m-auto max-h-[90vh] w-[calc(100%-2rem)] max-w-lg overflow-y-auto rounded-xl bg-white p-6 shadow-xl backdrop:bg-slate-900/40"
    >
      {open && (
        <div className="space-y-4">
          <h2 id={titleId} className="text-lg font-semibold text-slate-800">
            {title}
          </h2>
          {children}
        </div>
      )}
    </dialog>
  )
}
