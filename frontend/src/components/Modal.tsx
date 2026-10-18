import { useEffect, useId, useState, type ReactNode } from 'react'
import { X } from 'lucide-react'
import { OverlayContainerContext } from './overlayContainer'

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
 * Content taller than that scrolls on its own under the title and "×", which stay in place (Task 4.5): the way out
 * never scrolls away.
 * Lists and calendars that pop out of a field inside are drawn into this <dialog> (OverlayContainerContext), not at
 * the end of <body> where the dialog would cover them (Task 4.9).
 */
export function Modal({ open, title, onClose, size = 'md', children }: ModalProps) {
  // State rather than a ref: the children need the element itself, through the context, to draw into it
  const [dialog, setDialog] = useState<HTMLDialogElement | null>(null)
  const titleId = useId()

  useEffect(() => {
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
  }, [open, dialog])

  return (
    <dialog
      ref={setDialog}
      aria-labelledby={titleId}
      // Esc: let the parent decide (it may be busy saving) instead of the browser closing on its own. The browser
      // sends Esc to the top dialog only, but React passes the event on to every component above, so a dialog
      // opened inside another one (a confirmation over a form) would also close the one below; it stops here
      // (BUG-UI-011)
      onCancel={(event) => {
        event.preventDefault()
        event.stopPropagation()
        onClose()
      }}
      // A column only while open ("open:"): a display set on the closed dialog would override the browser rule
      // that hides it
      className={`mx-auto mb-0 mt-auto max-h-[90vh] w-full max-w-none overflow-hidden rounded-t-panel border border-tide bg-white p-0 text-ink shadow-lg backdrop:bg-ink/45 open:flex open:flex-col sm:m-auto sm:w-[calc(100%-2rem)] sm:rounded-panel ${WIDTHS[size]}`}
    >
      {open && (
        <OverlayContainerContext.Provider value={dialog}>
          <div className="flex shrink-0 items-start justify-between gap-4 px-6 pt-6 pb-4">
            <h2 id={titleId} className="text-lg leading-[26px] font-semibold text-ink">
              {title}
            </h2>
            <button
              type="button"
              aria-label="Đóng"
              data-modal-close
              onClick={onClose}
              className="-mr-2 -mt-1 flex size-8 shrink-0 items-center justify-center rounded-control text-gray-500 hover:bg-gray-100 hover:text-gray-900 focus-visible:ring-[3px] focus-visible:ring-jade/25 focus-visible:outline-none pointer-coarse:size-11"
            >
              <X aria-hidden className="size-4" />
            </button>
          </div>
          {/* The only part that scrolls. min-h-0 lets it shrink below its content inside the column */}
          <div className="min-h-0 space-y-4 overflow-y-auto px-6 pb-6">{children}</div>
        </OverlayContainerContext.Provider>
      )}
    </dialog>
  )
}
