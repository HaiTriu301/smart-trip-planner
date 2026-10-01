// Shared look of text inputs, selects and textareas (UI_GUIDE 7.2): 1px tide border, 6px radius, jade border +
// 3px soft ring on focus, danger border when invalid. The ring stays visible for keyboard users.
// Width and horizontal padding are NOT included: each caller adds its own (w-full px-3, or pl-9 next to an icon),
// because two classes setting the same property on one element have no guaranteed winner (BUG-UI-001).
export function controlClass(invalid: boolean): string {
  const state = invalid
    ? 'border-danger focus:ring-danger/15'
    : 'border-tide hover:border-gray-300 focus:border-jade focus:ring-jade/15'
  return `block rounded-control border bg-white text-[15px] text-ink outline-none transition-colors placeholder:text-gray-400 focus:ring-[3px] disabled:bg-gray-50 disabled:text-gray-500 ${state}`
}

/** ids for aria-describedby: the error replaces the hint while the field is invalid. */
export function describedBy(id: string, error?: string, hint?: string): string | undefined {
  if (error) return `${id}-error`
  if (hint) return `${id}-hint`
  return undefined
}
