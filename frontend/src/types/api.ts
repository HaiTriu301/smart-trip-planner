// Mirrors backend common/ApiResponse and common/ErrorResponse (design.md 10.1).
// Temporary hand-written types; replaced by generated types from OpenAPI in a later task.
export interface ApiResponse<T> {
  success: true
  data: T
  message: string
  timestamp: string
}

export interface FieldError {
  field: string
  message: string | null
}

export interface ErrorResponse {
  success: false
  errorCode: string
  message: string
  details: FieldError[]
  timestamp: string
  path: string
}