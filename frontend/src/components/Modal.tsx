import { useEffect, useId, useState, type ReactNode } from 'react'
import { X } from 'lucide-react'
import { OverlayContainerContext } from './overlayContainer'

interface ModalProps {
  open: boolean
  title: string
  onClose: () => void
  /** md: 480px (simple forms, confirmations) · lg: 640px (activity form), UI_GUIDE 7.6. Ignored by a panel */
  size?: 'md' | 'lg'
  /**
   * dialog: centred box (a sheet at the bottom on phones). panel: full height along the right edge, 360px wide,
   * the whole screen below 1024px, and the focus starts on the scrolling content instead of its first control
   * (UI_GUIDE 7.6, Task 4.6)
   */
  layout?: 'dialog' | 'panel'
  /** Panel only: stays at the bottom under a rule while the content scrolls, e.g. the comment box (Task 4.6) */
  footer?: ReactNode
  children: ReactNode
}

const WIDTHS = { md: 'sm:max-w-[480px]', lg: 'sm:max-w-[640px]' }

const DIALOG_FRAME = 'mx-auto mb-0 mt-auto max-h-[90vh] w-full max-w-none rounded-t-panel border border-tide sm:m-auto sm:w-[calc(100%-2rem)] sm:rounded-panel'

// m-0 + ml-auto against the browser's fixed inset of 0: pinned to the right edge, full height
const PANEL_FRAME = 'm-0 ml-auto h-dvh max-h-none w-full max-w-none border-0 lg:w-[360px] lg:rounded-l-panel lg:border-l lg:border-tide'

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
 * The panel layout (Task 4.6) is the same dialog along the right edge, for reading more than for filling a form.
 */
export function Modal({ open, title, onClose, size = 'md', layout = 'dialog', footer, children }: ModalProps) {
  // State rather than a ref: the children need the element itself, through the context, to draw into it
  const [dialog, setDialog] = useState<HTMLDialogElement | null>(null)
  const titleId = useId()
  const isPanel = layout === 'panel'

  useEffect(() => {
    if (!dialog) return
    if (open && !dialog.open) {
      dialog.showModal()
      if (isPanel) {
        // A panel is opened to read: starting on its input would bring up the keyboard of a phone, and its first
        // control is just the first button of a list. The scrolling content takes the focus (arrow keys scroll it)
        dialog.querySelector<HTMLElement>('[data-modal-body]')?.focus()
        return
      }
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
  }, [open, dialog, isPanel])

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
      className={`overflow-hidden bg-white p-0 text-ink shadow-lg backdrop:bg-ink/45 open:flex open:flex-col ${
        isPanel ? PANEL_FRAME : `${DIALOG_FRAME} ${WIDTHS[size]}`
      }`}
    >
      {open && (
        <OverlayContainerContext.Provider value={dialog}>
          <div
            className={`flex shrink-0 items-start justify-between gap-4 ${
              isPanel ? 'border-b border-tide px-5 py-4' : 'px-6 pt-6 pb-4'
            }`}
          >
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
          {isPanel ? (
            <>
              <div data-modal-body tabIndex={-1} className="min-h-0 flex-1 overflow-y-auto px-5 py-4 focus:outline-none">
                {children}
              </div>
              {footer && <div className="shrink-0 border-t border-tide px-5 py-4">{footer}</div>}
            </>
          ) : (
            <div className="min-h-0 space-y-4 overflow-y-auto px-6 pb-6">{children}</div>
          )}
        </OverlayContainerContext.Provider>
      )}
    </dialog>
  )
}
