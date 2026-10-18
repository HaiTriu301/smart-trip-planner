/**
 * The address a share link opens: {origin}/share/{token} (design.md 10.2: the server sends only the token, the
 * client puts the address together). The origin is the one the app runs on, so the same code gives the
 * localhost address in development and the real domain once deployed.
 */
export function shareUrl(origin: string, token: string): string {
  return `${origin.replace(/\/+$/, '')}/share/${encodeURIComponent(token)}`
}
