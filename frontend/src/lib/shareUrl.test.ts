import { describe, expect, it } from 'vitest'
import { shareUrl } from './shareUrl'

const TOKEN = 'k3j9x0abcdefghijklmnopqrstuvwxyz'

describe('shareUrl', () => {
  it.each([
    ['http://localhost:5173', `http://localhost:5173/share/${TOKEN}`],
    ['https://smarttrip.example', `https://smarttrip.example/share/${TOKEN}`],
  ])('puts the token after /share/ on %s', (origin, expected) => {
    expect(shareUrl(origin, TOKEN)).toBe(expected)
  })

  it('does not double the slash when the origin ends with one', () => {
    expect(shareUrl('https://smarttrip.example/', TOKEN)).toBe(`https://smarttrip.example/share/${TOKEN}`)
  })

  it('keeps the address valid whatever the token holds', () => {
    // The server only sends [a-z0-9]; anything else must not break out of the path
    expect(shareUrl('http://localhost:5173', 'a/b?c')).toBe('http://localhost:5173/share/a%2Fb%3Fc')
  })
})
