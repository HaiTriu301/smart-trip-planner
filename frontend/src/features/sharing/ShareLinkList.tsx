import { useId } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Copy } from 'lucide-react'
import { listShareLinks, shareLinksKey } from '../../api/shareLinks'
import { getErrorMessage } from '../../api/errors'
import { Alert } from '../../components/Alert'
import { Badge } from '../../components/Badge'
import { Button } from '../../components/Button'
import { Skeleton } from '../../components/Skeleton'
import { formatDate } from '../../lib/format'
import { shareUrl } from '../../lib/shareUrl'
import { DEFAULT_TIME_ZONE, todayIn } from '../../lib/today'
import { useAuthStore } from '../../stores/authStore'
import { toast } from '../../stores/toastStore'
import type { ShareLink } from '../../types/shareLink'

/**
 * "Liên kết chia sẻ" of the sharing dialog, for the owner (UI_GUIDE 15.3): every link not revoked, newest first,
 * as a read-only address with "Sao chép", how many times it was opened and until when it works. The dialog mounts
 * this only while it is open, so the links are asked for when the owner looks, not with every trip page.
 */
export function ShareLinkList({ tripId }: { tripId: number }) {
  const headingId = useId()
  const { data: links, error, isPending, isFetching, refetch } = useQuery({
    queryKey: shareLinksKey(tripId),
    queryFn: () => listShareLinks(tripId),
  })

  return (
    <section aria-labelledby={headingId} className="space-y-3">
      <div>
        <h3 id={headingId} className="text-[15px] leading-6 font-semibold text-ink">
          Liên kết chia sẻ
        </h3>
        <p className="text-sm text-gray-600">Ai có liên kết đều xem được chuyến đi, không cần tài khoản.</p>
      </div>
      {isPending ? (
        <Skeleton className="h-24 w-full" />
      ) : !links ? (
        <div className="space-y-3">
          <Alert variant="error">{getErrorMessage(error)}</Alert>
          <Button variant="secondary" fullWidth={false} isLoading={isFetching} onClick={() => refetch()}>
            Thử lại
          </Button>
        </div>
      ) : links.length === 0 ? (
        <p className="text-sm text-gray-500">Chưa có liên kết nào.</p>
      ) : (
        <ul className="space-y-3">
          {links.map((link) => (
            <ShareLinkRow key={link.id} link={link} />
          ))}
        </ul>
      )}
    </section>
  )
}

function ShareLinkRow({ link }: { link: ShareLink }) {
  const timeZone = useAuthStore((state) => state.user?.timezone) ?? DEFAULT_TIME_ZONE
  const url = shareUrl(window.location.origin, link.token)
  // The calendar day the link stops on, in the account's time zone like every date of the app
  const until = link.expiresAt ? formatDate(todayIn(timeZone, new Date(link.expiresAt))) : null
  const expiry = until === null ? 'Không hết hạn' : link.expired ? `Hết hạn ngày ${until}` : `Dùng được đến hết ngày ${until}`

  async function copy() {
    try {
      await navigator.clipboard.writeText(url)
      toast.success('Đã sao chép liên kết')
    } catch {
      // No clipboard outside HTTPS / localhost, or the browser refused: the field stays selectable by hand
      toast.error('Không sao chép được. Hãy chọn liên kết và sao chép thủ công.')
    }
  }

  return (
    <li className="space-y-2 rounded-card border border-tide p-3">
      <div className="flex gap-2">
        <input
          readOnly
          value={url}
          aria-label="Địa chỉ liên kết chia sẻ"
          onFocus={(event) => event.target.select()}
          className={`h-9 min-w-0 flex-1 rounded-control border border-tide bg-gray-50 px-3 font-mono text-[13px] outline-none focus:border-jade focus:ring-[3px] focus:ring-jade/15 ${
            link.expired ? 'text-gray-400' : 'text-ink'
          }`}
        />
        <Button variant="secondary" size="sm" fullWidth={false} disabled={link.expired} onClick={copy}>
          <Copy aria-hidden className="size-3.5" />
          Sao chép
        </Button>
      </div>
      <p className="tabular flex flex-wrap items-center gap-x-2 gap-y-1 text-[13px] text-gray-600">
        {link.expired && <Badge tone="muted">Hết hạn</Badge>}
        <span>
          {link.viewCount.toLocaleString('vi-VN')} lượt xem · {expiry}
        </span>
      </p>
    </li>
  )
}
