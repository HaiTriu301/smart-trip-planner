import type { ComponentProps } from 'react'

type ButtonProps = ComponentProps<'button'> & {
  variant?: 'primary' | 'secondary' | 'danger' | 'ghost' | 'ghost-danger'
  size?: 'md' | 'sm'
  /** Stretch to the container width (forms); false sizes the button to its label */
  fullWidth?: boolean
  isLoading?: boolean
}

const VARIANTS = {
  primary: 'bg-sky-600 text-white hover:bg-sky-700 disabled:bg-sky-300',
  secondary:
    'border border-slate-300 bg-white text-slate-700 hover:bg-slate-50 disabled:text-slate-400',
  danger: 'bg-red-600 text-white hover:bg-red-700 disabled:bg-red-300',
  // Low-key action repeated on many rows (edit a day, an activity), usually with size="sm"
  ghost: 'text-slate-500 hover:bg-slate-200/70 hover:text-slate-800 disabled:text-slate-300',
  // Same low-key look, turning red on hover: a delete action next to a ghost edit button
  'ghost-danger': 'text-slate-500 hover:bg-red-50 hover:text-red-700 disabled:text-slate-300',
}

// Width and padding live in props, not className: two classes setting the same property on one element
// have no guaranteed winner (a fullWidth={false} lost to w-full, BUG-UI-001)
const SIZES = {
  md: 'px-4 py-2',
  sm: 'px-2 py-1 text-sm',
}

export function Button({
  variant = 'primary',
  size = 'md',
  fullWidth = true,
  isLoading = false,
  disabled,
  className,
  children,
  type = 'button',
  ...props
}: ButtonProps) {
  return (
    <button
      type={type}
      disabled={disabled || isLoading}
      className={`inline-flex ${fullWidth ? 'w-full' : ''} items-center justify-center rounded-lg font-medium transition-colors disabled:cursor-not-allowed ${SIZES[size]} ${VARIANTS[variant]} ${className ?? ''}`}
      {...props}
    >
      {isLoading ? 'Đang xử lý...' : children}
    </button>
  )
}
