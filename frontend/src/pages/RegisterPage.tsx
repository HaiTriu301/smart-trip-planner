import { useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { Alert } from '../components/Alert'
import { AuthLayout } from '../features/auth/AuthLayout'
import { RegisterForm } from '../features/auth/RegisterForm'
import { isReturningToInvite } from '../features/auth/returnTo'
import { ResendVerificationForm } from '../features/auth/ResendVerificationForm'

export function RegisterPage() {
  // New accounts cannot log in until the email is verified (design.md 6.1), so no auto-login here
  const [registeredEmail, setRegisteredEmail] = useState<string | null>(null)
  // Came from an invitation link (through /login): the account cannot accept it until the email is verified, and
  // the mail of that verification opens in another tab, so the way back is the invitation mail itself (Task 4.5)
  const { state } = useLocation()
  const invited = isReturningToInvite(state)

  const footer = (
    <>
      Đã có tài khoản?{' '}
      <Link to="/login" state={state} className="font-medium text-jade hover:underline">
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
          {invited && (
            <Alert variant="info">
              Xác thực xong, mở lại link trong mail mời để tham gia chuyến đi.
            </Alert>
          )}
          <p className="text-sm text-gray-600">Không nhận được mail?</p>
          <ResendVerificationForm defaultEmail={registeredEmail} />
        </div>
      </AuthLayout>
    )
  }

  return (
    <AuthLayout title="Tạo tài khoản" subtitle="Lên kế hoạch chuyến đi đầu tiên của bạn" footer={footer}>
      <div className="space-y-6">
        {invited && (
          <Alert variant="info">
            Đăng ký bằng đúng email đã nhận lời mời. Xác thực email xong, mở lại link trong mail mời để tham gia
            chuyến đi.
          </Alert>
        )}
        <RegisterForm onRegistered={setRegisteredEmail} />
      </div>
    </AuthLayout>
  )
}
