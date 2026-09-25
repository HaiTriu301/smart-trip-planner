import type { ReactNode } from 'react'

interface AlertProps {
  variant: 'error' | 'success' | 'info'
  children: ReactNode
}

const VARIANTS = {
  error: 'border-red-200 bg-red-50 text-red-700',
  success: 'border-green-200 bg-green-50 text-green-700',
  info: 'border-sky-200 bg-sky-50 text-sky-700',
}

export function Alert({ variant, children }: AlertProps) {
  return (
    <div
      role={variant === 'error' ? 'alert' : 'status'}
      className={`rounded-lg border px-4 py-3 text-sm ${VARIANTS[variant]}`}
    >
      {children}
    </div>
  )
}
