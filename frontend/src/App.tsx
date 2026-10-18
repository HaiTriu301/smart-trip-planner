import { createBrowserRouter, Navigate, RouterProvider } from 'react-router-dom'
import { GuestRoute } from './components/GuestRoute'
import { ProtectedRoute } from './components/ProtectedRoute'
import { AppLayout } from './layouts/AppLayout'
import { CreateTripPage } from './pages/CreateTripPage'
import { ForgotPasswordPage } from './pages/ForgotPasswordPage'
import { InvitePage } from './pages/InvitePage'
import { LoginPage } from './pages/LoginPage'
import { PublicTripPage } from './pages/PublicTripPage'
import { RegisterPage } from './pages/RegisterPage'
import { ResetPasswordPage } from './pages/ResetPasswordPage'
import { TripDetailPage } from './pages/TripDetailPage'
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
  // A share link: for anybody, signed in or not, without the app bar (design.md 15, UI_GUIDE 8.5)
  { path: '/share/:token', element: <PublicTripPage /> },
  {
    element: <ProtectedRoute />,
    children: [
      // Opened from the invitation mail: needs an account (accepting binds the invitation to it, design.md 15),
      // but stands alone like the sign-in pages, without the app bar
      { path: '/invite', element: <InvitePage /> },
      {
        element: <AppLayout />,
        children: [
          { path: '/trips', element: <TripsPage /> },
          { path: '/trips/new', element: <CreateTripPage /> },
          { path: '/trips/:id', element: <TripDetailPage /> },
          { path: '/trips/:id/days/:dayIndex', element: <TripDetailPage /> },
        ],
      },
    ],
  },
  { path: '*', element: <Navigate to="/" replace /> },
])

function App() {
  return <RouterProvider router={router} />
}

export default App
