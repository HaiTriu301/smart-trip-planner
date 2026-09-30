import * as DropdownMenu from '@radix-ui/react-dropdown-menu'
import { EllipsisVertical, Pencil, Trash2 } from 'lucide-react'

interface ActivityMenuProps {
  title: string
  onEdit: () => void
  onDelete: () => void
  /** Classes of the trigger's wrapper, e.g. to show it only on hover */
  className?: string
}

const ITEM =
  'flex cursor-pointer items-center gap-2 px-3 py-2 text-sm outline-none select-none data-[highlighted]:bg-gray-50'

/**
 * "⋮" menu of an activity card (UI_GUIDE 7.3). Radix handles keyboard navigation, focus and screen readers;
 * the look is ours (tokens), no ready-made theme.
 */
export function ActivityMenu({ title, onEdit, onDelete, className }: ActivityMenuProps) {
  return (
    <DropdownMenu.Root>
      <div className={className}>
        <DropdownMenu.Trigger asChild>
          <button
            type="button"
            aria-label={`Tác vụ cho ${title}`}
            className="rounded-control p-1.5 text-gray-500 hover:bg-gray-100 hover:text-gray-900 focus-visible:ring-[3px] focus-visible:ring-jade/25 focus-visible:outline-none data-[state=open]:bg-gray-100"
          >
            <EllipsisVertical aria-hidden className="size-4" />
          </button>
        </DropdownMenu.Trigger>
      </div>
      <DropdownMenu.Portal>
        <DropdownMenu.Content
          align="end"
          sideOffset={4}
          className="z-40 min-w-40 rounded-control border border-tide bg-white py-1 text-ink shadow-md"
        >
          <DropdownMenu.Item onSelect={onEdit} className={ITEM}>
            <Pencil aria-hidden className="size-4 text-gray-500" />
            Sửa
          </DropdownMenu.Item>
          <DropdownMenu.Item onSelect={onDelete} className={`${ITEM} text-danger`}>
            <Trash2 aria-hidden className="size-4" />
            Xoá
          </DropdownMenu.Item>
        </DropdownMenu.Content>
      </DropdownMenu.Portal>
    </DropdownMenu.Root>
  )
}
