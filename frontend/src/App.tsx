import { createBrowserRouter, Navigate, RouterProvider } from 'react-router-dom'
import { GuestRoute } from './components/GuestRoute'
import { ProtectedRoute } from './components/ProtectedRoute'
import { ForgotPasswordPage } from './pages/ForgotPasswordPage'
import { LoginPage } from './pages/LoginPage'
import { RegisterPage } from './pages/RegisterPage'
import { ResetPasswordPage } from './pages/ResetPasswordPage'
import { TripsPage } from './pages/TripsPage'
import { VerifyEmailPage } from './pages/VerifyEmailPage'

// Routes follow design.md 15; the landing page at "/" comes later, until then it forwards to /trips
const router = createBrowserRouter([
  { path: '/', element: <Navigate to="/trips" replace /> },
  {
    element: <GuestRoute />,
    children: [
      { path: '/login', element: <LoginPage /> },
      { path: '/register', element: <RegisterPage /> },
      { path: '/forgot-password', element: <ForgotPasswordPage /> },
    ],
  },
  // Opened from mail links: must work whether or not the browser already has a session
  { path: '/verify-email', element: <VerifyEmailPage /> },
  { path: '/reset-password', element: <ResetPasswordPage /> },
  {
    element: <ProtectedRoute />,
    children: [{ path: '/trips', element: <TripsPage /> }],
  },
  { path: '*', element: <Navigate to="/" replace /> },
])

function App() {
  return <RouterProvider router={router} />
}

export default App
