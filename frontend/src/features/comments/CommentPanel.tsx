import { useEffect, useRef, useState, type ReactNode } from 'react'
import { useIsMutating, useQuery } from '@tanstack/react-query'
import { MessageCircle } from 'lucide-react'
import { commentsKey, createCommentKey, listComments } from '../../api/comments'
import { getErrorMessage } from '../../api/errors'
import { Alert } from '../../components/Alert'
import { Avatar } from '../../components/Avatar'
import { Button } from '../../components/Button'
import { Modal } from '../../components/Modal'
import { Skeleton } from '../../components/Skeleton'
import { useNow } from '../../hooks/useNow'
import { commentThreads } from '../../lib/commentTree'
import { formatDateTimeIn } from '../../lib/format'
import { relativeTime } from '../../lib/relativeTime'
import { DEFAULT_TIME_ZONE } from '../../lib/today'
import { useAuthStore } from '../../stores/authStore'
import type { Comment } from '../../types/comment'
import { CommentForm } from './CommentForm'

/**
 * "Bình luận" in the trip header (UI_GUIDE 8.1, Task 4.6): a secondary button with the number of comments of the
 * trip, replies included, that opens the comment panel. Every role sees it: anybody who can see the trip can read
 * and write comments (design.md 6.2). The list is asked for with the page, since the count needs it; a failed load
 * only leaves the count out.
 */
export function CommentButton({ tripId }: { tripId: number }) {
  const [open, setOpen] = useState(false)
  // The root comment being answered: "Trả lời" under it in the list, the chip above the box in the footer
  const [replyTo, setReplyTo] = useState<Comment | null>(null)
  // The comment sent last: the list scrolls to it, wherever it lands (the end, or under its root for a reply)
  const [sentId, setSentId] = useState<number | null>(null)
  const { data: comments } = useQuery({ queryKey: commentsKey(tripId), queryFn: () => listComments(tripId) })
  // Esc and "×" wait for the server: closing would leave its answer (an error under the box) nowhere (BUG-UI-004)
  const isSending = useIsMutating({ mutationKey: createCommentKey(tripId) }) > 0
  const count = comments?.length ?? 0

  function close() {
    if (isSending) return
    setOpen(false)
    setReplyTo(null)
  }

  return (
    <>
      <Button variant="secondary" fullWidth={false} onClick={() => setOpen(true)}>
        <MessageCircle aria-hidden className="size-4" />
        Bình luận
        {count > 0 && <span className="tabular">· {count}</span>}
      </Button>
      {/* A panel over the page: the sharing dialog cannot be opened at the same time */}
      <Modal
        open={open}
        onClose={close}
        title="Bình luận"
        layout="panel"
        footer={
          <CommentForm
            tripId={tripId}
            replyTo={replyTo}
            onCancelReply={() => setReplyTo(null)}
            onSent={(comment) => {
              setReplyTo(null)
              setSentId(comment.id)
            }}
          />
        }
      >
        <CommentList tripId={tripId} sentId={sentId} onReply={setReplyTo} />
      </Modal>
    </>
  )
}

/** How close to the end (px) still counts as "reading the latest comments" */
const NEAR_END = 48

/**
 * The comments of the trip, oldest at the top like a conversation. Mounted each time the panel opens, which asks
 * for the list again: comments written by others since the page loaded show up (no live updates before Phase 5).
 * The panel opens scrolled to the end, and stays at the end when new comments arrive while the reader is there;
 * someone reading further up is not pulled down. A comment just sent is scrolled to wherever it lands: the end for a
 * comment, under its root for a reply.
 */
function CommentList({
  tripId,
  sentId,
  onReply,
}: {
  tripId: number
  sentId: number | null
  onReply: (comment: Comment) => void
}) {
  const timeZone = useAuthStore((state) => state.user?.timezone) ?? DEFAULT_TIME_ZONE
  const { data: comments, error, isPending, isFetching, refetch } = useQuery({
    queryKey: commentsKey(tripId),
    queryFn: () => listComments(tripId),
    refetchOnMount: 'always',
  })
  const end = useRef<HTMLDivElement>(null)
  // Starts true: the panel opens at the end
  const atEnd = useRef(true)
  // A comment sent before this opening of the panel is not scrolled to again
  const sentBefore = useRef(sentId)

  useEffect(() => {
    const body = end.current?.closest<HTMLElement>('[data-modal-body]')
    if (!body) return
    const onScroll = () => {
      atEnd.current = body.scrollHeight - body.scrollTop - body.clientHeight < NEAR_END
    }
    body.addEventListener('scroll', onScroll)
    return () => body.removeEventListener('scroll', onScroll)
  }, [])

  useEffect(() => {
    if (!comments || !atEnd.current) return
    // The dialog is still closed while this runs on opening (its showModal() comes after the effects of its
    // content) and a closed dialog has no size to scroll: wait for the next frame
    const frame = requestAnimationFrame(() => {
      const body = end.current?.closest<HTMLElement>('[data-modal-body]')
      if (body) body.scrollTop = body.scrollHeight
    })
    return () => cancelAnimationFrame(frame)
  }, [comments])

  useEffect(() => {
    if (sentId === null || sentId === sentBefore.current) return
    sentBefore.current = sentId
    // Next frame: the new comment is drawn by then. "nearest" moves the list only as far as needed to show it
    const frame = requestAnimationFrame(() => {
      end.current
        ?.closest('[data-modal-body]')
        ?.querySelector(`[data-comment-id="${sentId}"]`)
        ?.scrollIntoView({ block: 'nearest' })
    })
    return () => cancelAnimationFrame(frame)
  }, [sentId, comments])

  const retry = (
    <Button variant="secondary" fullWidth={false} isLoading={isFetching} onClick={() => refetch()}>
      Thử lại
    </Button>
  )

  if (isPending) return <CommentListSkeleton />
  if (!comments) {
    return (
      <div className="space-y-3">
        <Alert variant="error">{getErrorMessage(error)}</Alert>
        {retry}
      </div>
    )
  }

  const threads = commentThreads(comments)
  return (
    <div className="space-y-4">
      {/* A reload failed while comments were on screen: keep them (BUG-UI-005) */}
      {error && (
        <div className="space-y-3">
          <Alert variant="error">{`${getErrorMessage(error)}. Đang hiển thị bình luận đã tải trước đó.`}</Alert>
          {retry}
        </div>
      )}
      {threads.length === 0 ? (
        <p className="text-sm text-gray-500">Chưa có bình luận nào. Hỏi hoặc góp ý cho cả nhóm ở ô bên dưới.</p>
      ) : (
        <ul className="space-y-4">
          {threads.map(({ comment, replies }) => (
            <CommentItem key={comment.id} comment={comment} timeZone={timeZone}>
              <button
                type="button"
                onClick={() => onReply(comment)}
                aria-label={`Trả lời bình luận của ${comment.author.fullName}`}
                // 44px tall on touch screens (UI_GUIDE 12)
                className="mt-1 inline-flex items-center rounded-control text-xs leading-4 font-semibold text-gray-500 hover:text-ink focus-visible:ring-[3px] focus-visible:ring-jade/25 focus-visible:outline-none pointer-coarse:min-h-11"
              >
                Trả lời
              </button>
              {replies.length > 0 && (
                <ul aria-label={`Trả lời bình luận của ${comment.author.fullName}`} className="mt-3 space-y-3">
                  {replies.map((reply) => (
                    <CommentItem key={reply.id} comment={reply} timeZone={timeZone} isReply />
                  ))}
                </ul>
              )}
            </CommentItem>
          ))}
        </ul>
      )}
      <div ref={end} />
    </div>
  )
}

interface CommentItemProps {
  comment: Comment
  timeZone: string
  /** A reply: smaller picture, and it never has replies of its own (one level, design.md rule 27) */
  isReply?: boolean
  /** The replies of a root comment, drawn in its text column so they line up with its text */
  children?: ReactNode
}

/** Picture, name and date on one line, then the text: plain, line breaks kept, long words broken. */
function CommentItem({ comment, timeZone, isReply = false, children }: CommentItemProps) {
  return (
    <li data-comment-id={comment.id} className={`flex ${isReply ? 'gap-2' : 'gap-3'}`}>
      <Avatar name={comment.author.fullName} url={comment.author.avatarUrl} size={isReply ? 'sm' : 'md'} />
      <div className="min-w-0 flex-1">
        <p className="flex flex-wrap items-baseline gap-x-2">
          <span className="text-sm leading-5 font-semibold wrap-anywhere text-ink">{comment.author.fullName}</span>
          <CommentTime instant={comment.createdAt} timeZone={timeZone} />
        </p>
        <p className="mt-0.5 text-sm leading-5 whitespace-pre-wrap wrap-anywhere text-gray-800">{comment.content}</p>
        {children}
      </div>
    </li>
  )
}

/**
 * "5 phút trước", moving on by itself while the panel stays open; the full date and time on hover. The only part
 * of a comment that reads the clock, so the list does not redraw every minute.
 */
function CommentTime({ instant, timeZone }: { instant: string; timeZone: string }) {
  const now = useNow()
  return (
    <time
      dateTime={instant}
      title={formatDateTimeIn(instant, timeZone)}
      className="tabular text-xs leading-4 text-gray-500"
    >
      {relativeTime(instant, now, timeZone)}
    </time>
  )
}

/** Three comments in outline while the list loads (UI_GUIDE 8.1). */
function CommentListSkeleton() {
  return (
    <div className="space-y-4" role="status" aria-label="Đang tải bình luận">
      {Array.from({ length: 3 }, (_, i) => (
        <div key={i} className="flex gap-3">
          {/* Round by clipping: a radius class next to the Skeleton's own would have no sure winner (BUG-UI-001) */}
          <div className="size-8 shrink-0 overflow-hidden rounded-full">
            <Skeleton className="size-full" />
          </div>
          <div className="flex-1 space-y-2">
            <Skeleton className="h-4 w-1/2" />
            <Skeleton className="h-4 w-full" />
          </div>
        </div>
      ))}
    </div>
  )
}
