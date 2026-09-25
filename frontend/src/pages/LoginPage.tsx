import { Link } from 'react-router-dom'
import { AuthLayout } from '../features/auth/AuthLayout'
import { LoginForm } from '../features/auth/LoginForm'

export function LoginPage() {
  return (
    <AuthLayout
      title="Đăng nhập"
      subtitle="Chào mừng bạn quay lại"
      footer={
        <>
          Chưa có tài khoản?{' '}
          <Link to="/register" className="font-medium text-sky-700 hover:underline">
            Đăng ký
          </Link>
        </>
      }
    >
      <LoginForm />
    </AuthLayout>
  )
}
