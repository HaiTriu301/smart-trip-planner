import type { ReactNode } from 'react'
import { CircleAlert, CircleCheck, Info } from 'lucide-react'

interface AlertProps {
  variant: 'error' | 'success' | 'info'
  children: ReactNode
}

// Semantic colours (UI_GUIDE 3.3): tinted background, full-strength text, and an icon so colour is never
// the only signal (UI_GUIDE 12)
const VARIANTS = {
  error: { box: 'border-danger/30 bg-danger/8 text-danger', Icon: CircleAlert },
  success: { box: 'border-success/30 bg-success/8 text-success', Icon: CircleCheck },
  info: { box: 'border-info/30 bg-info/8 text-info', Icon: Info },
}

export function Alert({ variant, children }: AlertProps) {
  const { box, Icon } = VARIANTS[variant]
  return (
    <div
      role={variant === 'error' ? 'alert' : 'status'}
      className={`flex gap-2 rounded-card border px-4 py-3 text-sm ${box}`}
    >
      <Icon aria-hidden className="mt-0.5 size-4 shrink-0" />
      <div className="min-w-0">{children}</div>
    </div>
  )
}
