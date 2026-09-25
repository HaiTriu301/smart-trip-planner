import type { ComponentProps } from 'react'

type ButtonProps = ComponentProps<'button'> & {
  variant?: 'primary' | 'secondary'
  isLoading?: boolean
}

const VARIANTS = {
  primary: 'bg-sky-600 text-white hover:bg-sky-700 disabled:bg-sky-300',
  secondary:
    'border border-slate-300 bg-white text-slate-700 hover:bg-slate-50 disabled:text-slate-400',
}

export function Button({
  variant = 'primary',
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
      className={`inline-flex w-full items-center justify-center rounded-lg px-4 py-2 font-medium transition-colors disabled:cursor-not-allowed ${VARIANTS[variant]} ${className ?? ''}`}
      {...props}
    >
      {isLoading ? 'Đang xử lý...' : children}
    </button>
  )
}
