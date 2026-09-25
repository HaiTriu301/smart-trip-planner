import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import './index.css'
import App from './App.tsx'
import { refreshAccessToken } from './api/client'

const queryClient = new QueryClient()

// Restore the session once at startup (F5 / new tab): the access token only lives in memory, the refresh
// cookie survives. Runs outside React so StrictMode's double effects cannot fire a second rotation.
// Failure (no cookie, revoked, backend down) already leaves the store 'anonymous'.
refreshAccessToken().catch(() => undefined)

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <App />
    </QueryClientProvider>
  </StrictMode>,
)
