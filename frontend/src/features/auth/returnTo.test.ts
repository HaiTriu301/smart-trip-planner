import { describe, expect, it } from 'vitest'
import { isReturningToInvite } from './returnTo'

describe('isReturningToInvite', () => {
  it('recognises the state ProtectedRoute leaves when an invitation link needed a sign-in', () => {
    expect(isReturningToInvite({ from: { pathname: '/invite', search: '?trip=12&token=abc', hash: '' } })).toBe(true)
  })

  it('is false for any other page the visitor was sent back from', () => {
    expect(isReturningToInvite({ from: { pathname: '/trips/12/days/1', search: '', hash: '' } })).toBe(false)
  })

  it.each([
    ['no state (the sign-in page opened directly)', null],
    ['an empty state', {}],
    ['a state without a location', { from: '/invite' }],
    ['a location without a path', { from: {} }],
    ['a state of another kind', 'invite'],
  ])('is false with %s', (_case, state) => {
    expect(isReturningToInvite(state)).toBe(false)
  })
})
