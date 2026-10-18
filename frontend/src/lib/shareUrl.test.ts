import { describe, expect, it } from 'vitest'
import { isShareToken, shareUrl } from './shareUrl'

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

describe('isShareToken', () => {
  it('accepts the 32 lowercase letters and digits the server issues', () => {
    expect(isShareToken(TOKEN)).toBe(true)
  })

  it.each([
    ['no token', undefined],
    ['a token cut short', TOKEN.slice(0, 31)],
    ['a token one character too long', `${TOKEN}a`],
    ['capital letters', TOKEN.toUpperCase()],
    ['other characters', `${TOKEN.slice(0, 31)}-`],
  ])('refuses %s', (_case, token) => {
    expect(isShareToken(token)).toBe(false)
  })
})
