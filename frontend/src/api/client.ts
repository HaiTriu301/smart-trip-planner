import axios from 'axios'

// Falls back to a relative path so requests always go through the dev proxy / nginx
const baseURL = import.meta.env.VITE_API_URL || '/api/v1'

export const apiClient = axios.create({
  baseURL,
  timeout: 10_000,
  // Refresh token travels in an httpOnly cookie (design.md 6.1)
  withCredentials: true,
  headers: { 'Content-Type': 'application/json' },
})