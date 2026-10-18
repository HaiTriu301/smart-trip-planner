import { Link, useLocation } from 'react-router-dom'
import { Alert } from '../components/Alert'
import { AuthLayout } from '../features/auth/AuthLayout'
import { LoginForm } from '../features/auth/LoginForm'
import { isReturningToInvite } from '../features/auth/returnTo'

/**
 * The router state carries the page ProtectedRoute sent the visitor from: it goes along to /register, so an
 * invited person who first needs an account still knows where they came from (Task 4.5).
 */
export function LoginPage() {
  const { state } = useLocation()
  return (
    <AuthLayout
      title="Đăng nhập"
      subtitle="Chào mừng bạn quay lại"
      footer={
        <>
          Chưa có tài khoản?{' '}
          <Link to="/register" state={state} className="font-medium text-jade hover:underline">
            Đăng ký
          </Link>
        </>
      }
    >
      <div className="space-y-6">
        {isReturningToInvite(state) && (
          <Alert variant="info">
            Đăng nhập bằng đúng email đã nhận lời mời để tham gia chuyến đi. Chưa có tài khoản thì đăng ký bằng
            email đó.
          </Alert>
        )}
        <LoginForm />
      </div>
    </AuthLayout>
  )
}
