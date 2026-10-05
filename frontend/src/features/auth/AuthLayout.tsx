import type { ReactNode } from 'react'
import { Logo } from '../../components/Logo'

interface AuthLayoutProps {
  title: string
  subtitle?: string
  children: ReactNode
  /** Line under the card, e.g. "Chưa có tài khoản? Đăng ký" */
  footer?: ReactNode
}

/** Sign-in pages (Stitch mockup): logo with tagline, a 400px card with a 1px border, then the switch link below. */
export function AuthLayout({ title, subtitle, children, footer }: AuthLayoutProps) {
  return (
    <main className="flex min-h-screen items-center justify-center px-4 py-12">
      <div className="w-full max-w-[400px] space-y-6">
        <div className="flex justify-center">
          <Logo tagline="Kế hoạch hành trình theo dòng thời gian" />
        </div>
        <div className="rounded-card border border-tide bg-white p-6 sm:p-8">
          <h1 className="text-[28px] leading-9 font-bold tracking-[-0.02em] text-ink">{title}</h1>
          {subtitle && <p className="mt-1 text-gray-600">{subtitle}</p>}
          <div className="mt-6">{children}</div>
        </div>
        {footer && <div className="text-center text-gray-600">{footer}</div>}
      </div>
    </main>
  )
}
