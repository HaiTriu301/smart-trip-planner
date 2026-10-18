// Look of the menus that drop out of a button (Radix DropdownMenu): the "⋮" menu of an activity, the trip status,
// the sort order of the trip list (UI_GUIDE 7.16). One place, so they all open, highlight and close alike.

/**
 * The floating box: white, 1px tide border, 6px radius, soft shadow; it grows from the side of its button
 * (the transform origin Radix computes) and fades out when closing.
 */
export const MENU_CONTENT =
  'z-40 min-w-40 origin-(--radix-dropdown-menu-content-transform-origin) rounded-control border border-tide bg-white py-1 text-ink shadow-md data-[state=closed]:animate-pop-out data-[state=open]:animate-pop-in motion-reduce:animate-none'

/**
 * One row: 36px with a mouse, 44px for a finger (UI_GUIDE 12); gray-100 under the pointer or the arrow keys, a
 * neutral tint that also suits the red "Xoá" row (the lists of a select use light teal instead).
 */
export const MENU_ITEM =
  'flex cursor-pointer items-center gap-2 px-3 py-2 text-sm outline-none select-none data-[highlighted]:bg-gray-100 pointer-coarse:py-3'
