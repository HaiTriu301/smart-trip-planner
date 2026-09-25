import { useState } from 'react'
import { Link } from 'react-router-dom'
import { Alert } from '../components/Alert'
import { AuthLayout } from '../features/auth/AuthLayout'
import { RegisterForm } from '../features/auth/RegisterForm'
import { ResendVerificationForm } from '../features/auth/ResendVerificationForm'

export function RegisterPage() {
  // New accounts cannot log in until the email is verified (design.md 6.1), so no auto-login here
  const [registeredEmail, setRegisteredEmail] = useState<string | null>(null)

  const footer = (
    <>
      Đã có tài khoản?{' '}
      <Link to="/login" className="font-medium text-sky-700 hover:underline">
        Đăng nhập
      </Link>
    </>
  )

  if (registeredEmail) {
    return (
      <AuthLayout title="Kiểm tra hộp thư" footer={footer}>
        <div className="space-y-4">
          <Alert variant="success">
            Đã gửi mail xác thực tới <strong>{registeredEmail}</strong>. Mở link trong mail để kích hoạt
            tài khoản rồi đăng nhập.
          </Alert>
          <p className="text-sm text-slate-600">Không nhận được mail?</p>
          <ResendVerificationForm defaultEmail={registeredEmail} />
        </div>
      </AuthLayout>
    )
  }

  return (
    <AuthLayout title="Tạo tài khoản" subtitle="Lên kế hoạch chuyến đi đầu tiên của bạn" footer={footer}>
      <RegisterForm onRegistered={setRegisteredEmail} />
    </AuthLayout>
  )
}
