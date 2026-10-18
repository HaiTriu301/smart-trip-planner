import { describe, expect, it } from 'vitest'
import { canEditTrip, showsRoleBadge } from './tripRole'

describe('canEditTrip', () => {
  it.each(['OWNER', 'EDITOR'] as const)('lets the %s change the trip', (role) => {
    expect(canEditTrip(role)).toBe(true)
  })

  it('keeps a viewer to reading', () => {
    expect(canEditTrip('VIEWER')).toBe(false)
  })
})

describe('showsRoleBadge', () => {
  it('hides the owner badge on a trip nobody else is on', () => {
    expect(showsRoleBadge('OWNER', 1)).toBe(false)
  })

  it('shows the owner badge once somebody else is on the trip, pending or not', () => {
    expect(showsRoleBadge('OWNER', 2)).toBe(true)
  })

  it.each(['EDITOR', 'VIEWER'] as const)('always shows the badge of a %s', (role) => {
    // A member is never alone on a trip: the owner is listed too
    expect(showsRoleBadge(role, 2)).toBe(true)
  })
})
