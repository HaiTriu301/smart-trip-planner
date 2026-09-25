import { useQuery } from '@tanstack/react-query'
import { Link, useSearchParams } from 'react-router-dom'
import { verifyEmail } from '../api/auth'
import { getErrorMessage } from '../api/errors'
import { Alert } from '../components/Alert'
import { AuthLayout } from '../features/auth/AuthLayout'
import { ResendVerificationForm } from '../features/auth/ResendVerificationForm'

const loginLink = (
  <Link to="/login" className="font-medium text-sky-700 hover:underline">
    Đến trang đăng nhập
  </Link>
)

/** Landing page of the mailed link: /verify-email?token=... */
export function VerifyEmailPage() {
  const token = useSearchParams()[0].get('token')

  // useQuery (not a mutation in an effect) so the one-shot token is sent exactly once: the cache dedupes
  // StrictMode's double mount, and a second POST would fail with INVALID_TOKEN after the first succeeded.
  const { isPending, isError, error } = useQuery({
    queryKey: ['auth', 'verify-email', token],
    queryFn: () => verifyEmail(token!).then(() => true),
    enabled: !!token,
    retry: false,
    staleTime: Infinity,
    gcTime: Infinity,
    refetchOnWindowFocus: false,
  })

  if (!token) {
    return (
      <AuthLayout title="Xác thực email" footer={loginLink}>
        <div className="space-y-4">
          <Alert variant="error">Link xác thực không hợp lệ. Hãy mở đúng link trong mail.</Alert>
          <ResendVerificationForm />
        </div>
      </AuthLayout>
    )
  }

  if (isPending) {
    return (
      <AuthLayout title="Xác thực email">
        <p className="text-slate-600">Đang xác thực email của bạn...</p>
      </AuthLayout>
    )
  }

  if (isError) {
    return (
      <AuthLayout title="Xác thực email" footer={loginLink}>
        <div className="space-y-4">
          <Alert variant="error">
            {getErrorMessage(error)}. Link có thể đã hết hạn hoặc đã được dùng; gửi lại mail bên dưới.
          </Alert>
          <ResendVerificationForm />
        </div>
      </AuthLayout>
    )
  }

  return (
    <AuthLayout title="Xác thực email">
      <div className="space-y-4">
        <Alert variant="success">Email đã được xác thực. Bạn có thể đăng nhập ngay.</Alert>
        <p className="text-center">{loginLink}</p>
      </div>
    </AuthLayout>
  )
}
