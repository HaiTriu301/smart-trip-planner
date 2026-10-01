// Badge colours (UI_GUIDE 7.5): base colour at 12% as background, the full colour as text.

export type BadgeTone = 'neutral' | 'muted' | 'brand' | 'info' | 'success' | 'warning'

const TINT: Record<BadgeTone, string> = {
  neutral: 'bg-gray-100 text-gray-700',
  muted: 'bg-gray-100 text-gray-500',
  brand: 'bg-jade/12 text-jade-dark',
  info: 'bg-info/12 text-info',
  success: 'bg-success/12 text-success',
  // Amber text on a light background fails contrast, so the text stays dark
  warning: 'bg-warning/15 text-gray-800',
}

// Over a photo a 12% tint is unreadable: white background, coloured text and a thin border instead
const SOLID: Record<BadgeTone, string> = {
  neutral: 'border border-gray-300 bg-white text-gray-700',
  muted: 'border border-gray-300 bg-white text-gray-500',
  brand: 'border border-jade/30 bg-white text-jade-dark',
  info: 'border border-info/30 bg-white text-info',
  success: 'border border-success/30 bg-white text-success',
  warning: 'border border-warning/50 bg-white text-gray-800',
}

export function badgeToneClass(tone: BadgeTone, surface: 'tint' | 'solid' = 'tint'): string {
  return surface === 'solid' ? SOLID[tone] : TINT[tone]
}
