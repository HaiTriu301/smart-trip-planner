import { describe, expect, it } from 'vitest'
import { showsRoleBadge } from './tripRole'

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
