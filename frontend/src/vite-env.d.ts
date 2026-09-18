/// <reference types="vite/client" />

// Typed access to import.meta.env.* (only VITE_-prefixed variables are exposed to the browser)
interface ImportMetaEnv {
  readonly VITE_API_URL?: string
}