/**
 * Whether the sign-in pages were reached from an invitation link: ProtectedRoute sends a visitor without a
 * session to /login with the page they asked for in the router state ({ from: location }), and the links between
 * /login and /register pass that state on. Router state is whatever was pushed with the history entry, so every
 * level is checked before use.
 */
export function isReturningToInvite(state: unknown): boolean {
  if (typeof state !== 'object' || state === null || !('from' in state)) return false
  const { from } = state
  return typeof from === 'object' && from !== null && 'pathname' in from && from.pathname === '/invite'
}
