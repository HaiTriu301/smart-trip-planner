// Shared look of text inputs, selects and textareas (UI_GUIDE 7.2): 1px tide border, 6px radius, jade border +
// 3px soft ring on focus, danger border when invalid. The ring stays visible for keyboard users.
// Width and horizontal padding are NOT included: each caller adds its own (w-full px-3, or pl-9 next to an icon),
// because two classes setting the same property on one element have no guaranteed winner (BUG-UI-001).
export function controlClass(invalid: boolean): string {
  return `block ${controlLookClass(invalid)}`
}

/**
 * controlClass without the display: for a field drawn as a flex row (the button of Select), which would otherwise
 * carry both "block" and "flex" (BUG-UI-001). "sm" is the 13px text of the compact select in a member row, given
 * here rather than added over the 15px one.
 */
export function controlLookClass(invalid: boolean, textSize: 'md' | 'sm' = 'md'): string {
  const state = invalid
    ? 'border-danger focus:ring-danger/15'
    : 'border-tide hover:border-gray-300 focus:border-jade focus:ring-jade/15'
  const text = textSize === 'sm' ? 'text-[13px]' : 'text-[15px]'
  return `rounded-control border bg-white ${text} text-ink outline-none transition-colors placeholder:text-gray-400 focus:ring-[3px] disabled:bg-gray-50 disabled:text-gray-500 ${state}`
}

/** ids for aria-describedby: the error replaces the hint while the field is invalid. */
export function describedBy(id: string, error?: string, hint?: string): string | undefined {
  if (error) return `${id}-error`
  if (hint) return `${id}-hint`
  return undefined
}
