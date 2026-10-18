/**
 * The address a share link opens: {origin}/share/{token} (design.md 10.2: the server sends only the token, the
 * client puts the address together). The origin is the one the app runs on, so the same code gives the
 * localhost address in development and the real domain once deployed.
 */
export function shareUrl(origin: string, token: string): string {
  return `${origin.replace(/\/+$/, '')}/share/${encodeURIComponent(token)}`
}

// What the server issues: 32 characters, lowercase letters and digits (design.md 5.2 share_links.token)
const SHARE_TOKEN = /^[a-z0-9]{32}$/

/**
 * Whether the end of a /share/... address can be a share link at all. Anything else (cut short when copied,
 * changed by hand) gets the "no longer valid" page at once, without asking the server.
 */
export function isShareToken(token: string | undefined): token is string {
  return token !== undefined && SHARE_TOKEN.test(token)
}
