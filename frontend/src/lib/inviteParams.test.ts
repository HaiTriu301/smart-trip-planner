import { describe, expect, it } from 'vitest'
import { parseInviteParams } from './inviteParams'

// 43 characters of URL-safe Base64, the shape the server sends
const TOKEN = 'q3Zr9-AbCdEfGhIjKlMnOpQrStUvWxYz_0123456789'

const parse = (query: string) => parseInviteParams(new URLSearchParams(query))

describe('parseInviteParams', () => {
  it('reads the trip and the token of a mailed link', () => {
    expect(parse(`trip=12&token=${TOKEN}`)).toEqual({ tripId: 12, token: TOKEN })
  })

  it.each([
    ['no trip', `token=${TOKEN}`],
    ['no token', 'trip=12'],
    ['an empty token', 'trip=12&token='],
    ['a trip that is not a number', `trip=abc&token=${TOKEN}`],
    ['trip 0', `trip=0&token=${TOKEN}`],
    ['a negative trip', `trip=-3&token=${TOKEN}`],
    ['a decimal trip', `trip=1.5&token=${TOKEN}`],
    ['a trip too large to stay exact', `trip=12345678901234567&token=${TOKEN}`],
    ['a token cut short', 'trip=12&token=q3Zr9-AbCd'],
    // A "+" in a query string reads as a space: a link copied through a tool that encoded it differently
    ['a token with a space', `trip=12&token=${TOKEN.slice(0, 20)}+${TOKEN.slice(20)}`],
    ['a token with characters Base64 for URLs never uses', `trip=12&token=${TOKEN.slice(0, 40)}/=.`],
  ])('refuses a link with %s', (_case, query) => {
    expect(parse(query)).toBeNull()
  })
})
