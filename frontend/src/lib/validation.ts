import { z } from 'zod'

// Field rules shared by several forms; messages match backend messages.properties.

const URL_PATTERN = /^https?:\/\/\S+$/
const NUMBER_PATTERN = /^-?\d+(\.\d+)?$/
// DECIMAL(15,2): up to 13 integer digits and 2 decimals
const MONEY_PATTERN = /^\d{1,13}(\.\d{1,2})?$/
const CURRENCY_PATTERN = /^[A-Z]{3}$/

/** Offered in currency selects; the backend accepts any 3-letter code. */
export const CURRENCIES = ['VND', 'USD', 'EUR', 'JPY', 'KRW', 'THB', 'SGD']

/** Shown when an optional value that PATCH cannot clear yet was emptied (design.md 10.2). */
export const CANNOT_CLEAR_MESSAGE = 'Chưa hỗ trợ xoá thông tin này, hãy nhập giá trị mới'

/** Optional amount typed as text; label is the Vietnamese field name, e.g. "Ngân sách". */
export function moneyText(label: string) {
  return z
    .string()
    .trim()
    .superRefine((v, ctx) => {
      if (v === '') return
      if (!NUMBER_PATTERN.test(v)) {
        ctx.addIssue({ code: 'custom', message: `${label} phải là một số, ví dụ 5000000` })
      } else if (v.startsWith('-')) {
        ctx.addIssue({ code: 'custom', message: `${label} không được âm` })
      } else if (!MONEY_PATTERN.test(v)) {
        ctx.addIssue({ code: 'custom', message: `${label} có tối đa 13 chữ số phần nguyên và 2 chữ số thập phân` })
      }
    })
}

/** Optional http(s) link, at most 512 characters. */
export function urlText(label: string) {
  return z
    .string()
    .trim()
    .max(512, `${label} không được vượt quá 512 ký tự`)
    .refine((v) => v === '' || URL_PATTERN.test(v), `${label} phải bắt đầu bằng http:// hoặc https://`)
}

export const currencyCode = z
  .string()
  .regex(CURRENCY_PATTERN, 'Mã tiền tệ phải gồm 3 chữ cái in hoa, ví dụ VND hoặc USD')

/** Currency select options that always include the current value, even when it is not in CURRENCIES. */
export function currencyOptions(current: string) {
  const codes = CURRENCIES.includes(current) || current === '' ? CURRENCIES : [...CURRENCIES, current]
  return codes.map((code) => ({ value: code, label: code }))
}
