import { useEffect, useId, useRef, type ReactNode } from 'react'
import { X } from 'lucide-react'

interface ModalProps {
  open: boolean
  title: string
  onClose: () => void
  /** md: 480px (simple forms, confirmations) · lg: 640px (activity form), UI_GUIDE 7.6 */
  size?: 'md' | 'lg'
  children: ReactNode
}

const WIDTHS = { md: 'sm:max-w-[480px]', lg: 'sm:max-w-[640px]' }

const FOCUSABLE =
  'input:not([type="hidden"]):not([disabled]), select:not([disabled]), textarea:not([disabled]), button:not([disabled]), a[href]'

/**
 * Native <dialog> opened with showModal(): the browser provides the backdrop, focus trap and Esc key.
 * The focus starts on the first control of the content, not on the "×" button (UI_GUIDE 7.6).
 * Children mount only while open, so a form inside starts fresh every time.
 * On phones the dialog sits at the bottom edge like a sheet, at most 90% of the screen height.
 */
export function Modal({ open, title, onClose, size = 'md', children }: ModalProps) {
  const ref = useRef<HTMLDialogElement>(null)
  const titleId = useId()

  useEffect(() => {
    const dialog = ref.current
    if (!dialog) return
    if (open && !dialog.open) {
      dialog.showModal()
      // showModal() focuses the first focusable element, which is the "×" button: typing would go nowhere and
      // Enter would close the dialog. Start on the first control of the content instead: the first field of a
      // form, "Huỷ" in a confirmation. A control marked data-no-initial-focus is skipped: a button that removes
      // something must not be where Enter lands. The autoFocus prop cannot do this inside a dialog: React focuses at
      // mount, while the dialog is still closed, and focusing a hidden element has no effect.
      Array.from(dialog.querySelectorAll<HTMLElement>(FOCUSABLE))
        .find(
          (element) => !element.hasAttribute('data-modal-close') && !element.hasAttribute('data-no-initial-focus'),
        )
        ?.focus()
    }
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
      className={`mx-auto mb-0 mt-auto max-h-[90vh] w-full max-w-none overflow-y-auto rounded-t-panel border border-tide bg-white p-6 text-ink shadow-lg backdrop:bg-ink/45 sm:m-auto sm:w-[calc(100%-2rem)] sm:rounded-panel ${WIDTHS[size]}`}
    >
      {open && (
        <div className="space-y-4">
          <div className="flex items-start justify-between gap-4">
            <h2 id={titleId} className="text-lg leading-[26px] font-semibold text-ink">
              {title}
            </h2>
            <button
              type="button"
              aria-label="Đóng"
              data-modal-close
              onClick={onClose}
              className="-mr-2 -mt-1 rounded-control p-2 text-gray-500 hover:bg-gray-100 hover:text-gray-900 focus-visible:ring-[3px] focus-visible:ring-jade/25 focus-visible:outline-none"
            >
              <X aria-hidden className="size-4" />
            </button>
          </div>
          {children}
        </div>
      )}
    </dialog>
  )
}
