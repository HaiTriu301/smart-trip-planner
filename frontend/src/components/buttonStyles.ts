// Button look shared by <Button> and <LinkButton> (a link that must look like a button, e.g. "Tạo chuyến đi").

export type ButtonVariant =
  | 'primary'
  | 'secondary'
  | 'ghost'
  | 'ghost-danger'
  | 'ghost-inverse'
  | 'danger'
  | 'danger-solid'
export type ButtonSize = 'sm' | 'md' | 'lg'

// UI_GUIDE 7.1. Only one primary action per screen.
const VARIANTS: Record<ButtonVariant, string> = {
  primary: 'bg-jade text-white hover:bg-jade-dark',
  secondary: 'border border-tide bg-white text-gray-800 hover:bg-gray-50',
  // Low-key action repeated on many rows (edit a day, an activity)
  ghost: 'text-gray-600 hover:bg-gray-100 hover:text-gray-900',
  // Same low-key look, turning red on hover: a delete action next to a ghost edit button
  'ghost-danger': 'text-gray-600 hover:bg-danger/8 hover:text-danger',
  // Ghost for the dark ink bars (top navigation)
  'ghost-inverse': 'text-gray-300 hover:bg-white/10 hover:text-white',
  // Delete outside a confirmation: outlined; only the confirm button of a dialog is filled red
  danger: 'border border-danger bg-white text-danger hover:bg-danger/8',
  'danger-solid': 'bg-danger text-white hover:bg-danger/90',
}

// Width and padding live in props, not className: two classes setting the same property on one element
// have no guaranteed winner (className="w-auto" lost to the built-in w-full, BUG-UI-001).
// Heights: 36px by default, 44px on phones for the 44×44 touch target (UI_GUIDE 7.1, 12).
const SIZES: Record<ButtonSize, string> = {
  sm: 'h-8 gap-1 px-2 text-[13px]',
  md: 'h-11 gap-2 px-4 text-[15px] sm:h-9',
  lg: 'h-11 gap-2 px-4 text-[15px]',
}

export function buttonClass(variant: ButtonVariant, size: ButtonSize, fullWidth: boolean): string {
  return `inline-flex ${fullWidth ? 'w-full' : ''} items-center justify-center rounded-control font-medium transition-colors focus-visible:ring-[3px] focus-visible:ring-jade/25 focus-visible:outline-none disabled:cursor-not-allowed disabled:opacity-50 ${SIZES[size]} ${VARIANTS[variant]}`
}
