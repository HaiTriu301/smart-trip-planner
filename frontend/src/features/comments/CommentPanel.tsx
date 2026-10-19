import { useEffect, useMemo, useRef, useState, type ReactNode } from 'react'
import { useIsMutating, useQuery } from '@tanstack/react-query'
import { MessageCircle, X } from 'lucide-react'
import { commentsKey, createCommentKey, listComments } from '../../api/comments'
import { getErrorMessage } from '../../api/errors'
import { Alert } from '../../components/Alert'
import { Avatar } from '../../components/Avatar'
import { Button } from '../../components/Button'
import { Modal } from '../../components/Modal'
import { Skeleton } from '../../components/Skeleton'
import { useNow } from '../../hooks/useNow'
import { activitiesById, commentsAbout, commentThreads } from '../../lib/commentTree'
import { formatDateTimeIn } from '../../lib/format'
import { relativeTime } from '../../lib/relativeTime'
import { DEFAULT_TIME_ZONE } from '../../lib/today'
import { useAuthStore } from '../../stores/authStore'
import { useCommentPanelStore } from '../../stores/commentPanelStore'
import type { Activity } from '../../types/activity'
import type { Comment } from '../../types/comment'
import type { TripDayDetail, TripDetail } from '../../types/trip'
import { ACTIVITY_ROUTE } from '../itinerary/activityType'
import { ActivityChip } from './ActivityChip'
import { CommentForm } from './CommentForm'

/**
 * "Bình luận" in the trip header (UI_GUIDE 8.1, Task 4.6): a secondary button with the number of comments of the
 * trip, replies included, that opens the comment panel. Every role sees it: anybody who can see the trip can read
 * and write comments (design.md 6.2). The list is asked for with the page, since the count needs it; a failed load
 * only leaves the count out.
 */
export function CommentButton({ trip }: { trip: TripDetail }) {
  const tripId = trip.id
  // Open from here or from the button of an activity card, through the store
  const open = useCommentPanelStore((state) => state.open)
  const activityId = useCommentPanelStore((state) => state.activityId)
  const { openPanel, closePanel } = useCommentPanelStore.getState()
  // The root comment being answered: "Trả lời" under it in the list, the chip above the box in the footer
  const [replyTo, setReplyTo] = useState<Comment | null>(null)
  // The comment sent last: the list scrolls to it, wherever it lands (the end, or under its root for a reply)
  const [sentId, setSentId] = useState<number | null>(null)
  const { data: comments } = useQuery({ queryKey: commentsKey(tripId), queryFn: () => listComments(tripId) })
  // Esc and "×" wait for the server: closing would leave its answer (an error under the box) nowhere (BUG-UI-004)
  const isSending = useIsMutating({ mutationKey: createCommentKey(tripId) }) > 0
  const count = comments?.length ?? 0

  // Leaving the trip page, or going to another trip, closes the panel: the store outlives this page
  useEffect(() => () => useCommentPanelStore.getState().closePanel(), [tripId])

  function close() {
    if (isSending) return
    closePanel()
    setReplyTo(null)
  }

  return (
    <>
      <Button variant="secondary" fullWidth={false} onClick={() => openPanel()}>
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
            activityId={activityId}
            replyTo={replyTo}
            onCancelReply={() => setReplyTo(null)}
            onSent={(comment) => {
              setReplyTo(null)
              setSentId(comment.id)
            }}
          />
        }
      >
        <CommentList
          tripId={tripId}
          days={trip.days}
          activityId={activityId}
          sentId={sentId}
          onReply={setReplyTo}
        />
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
 * <p>
 * Opened from an activity card, it shows only the comments about that activity, under a chip "{activity} ×" that
 * goes back to all of them; the comments then carry no chip of their own (they are all about that activity).
 */
function CommentList({
  tripId,
  days,
  activityId,
  sentId,
  onReply,
}: {
  tripId: number
  /** The days of the trip as the page has them: where the activity of a comment is looked up */
  days: TripDayDetail[]
  /** Only the comments about this activity; null for all of them */
  activityId: number | null
  sentId: number | null
  onReply: (comment: Comment) => void
}) {
  const timeZone = useAuthStore((state) => state.user?.timezone) ?? DEFAULT_TIME_ZONE
  const activities = useMemo(() => activitiesById(days), [days])
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
    // activityId: the list changes when the filter does, without a new array from the server
  }, [comments, activityId])

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

  const filtered = activityId !== null
  const threads = commentThreads(filtered ? commentsAbout(comments, activityId) : comments)
  return (
    <div className="space-y-4">
      {filtered && <ActivityFilter activity={activities.get(activityId)} />}
      {/* A reload failed while comments were on screen: keep them (BUG-UI-005) */}
      {error && (
        <div className="space-y-3">
          <Alert variant="error">{`${getErrorMessage(error)}. Đang hiển thị bình luận đã tải trước đó.`}</Alert>
          {retry}
        </div>
      )}
      {threads.length === 0 ? (
        <p className="text-sm text-gray-500">
          {filtered
            ? 'Chưa có bình luận nào về hoạt động này.'
            : 'Chưa có bình luận nào. Hỏi hoặc góp ý cho cả nhóm ở ô bên dưới.'}
        </p>
      ) : (
        <ul className="space-y-4">
          {threads.map(({ comment, replies }) => (
            <CommentItem
              key={comment.id}
              comment={comment}
              timeZone={timeZone}
              // On the root only: a reply is about the same activity, saying it again adds nothing. None while
              // filtered: the chip at the top already says it
              activity={filtered || comment.activityId === null ? undefined : activities.get(comment.activityId)}
            >
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
  /** The activity the comment is about, shown as a chip between the name and the text */
  activity?: Activity
  /** The replies of a root comment, drawn in its text column so they line up with its text */
  children?: ReactNode
}

/** Picture, name and date on one line, then the text: plain, line breaks kept, long words broken. */
function CommentItem({ comment, timeZone, isReply = false, activity, children }: CommentItemProps) {
  return (
    <li data-comment-id={comment.id} className={`flex ${isReply ? 'gap-2' : 'gap-3'}`}>
      <Avatar name={comment.author.fullName} url={comment.author.avatarUrl} size={isReply ? 'sm' : 'md'} />
      <div className="min-w-0 flex-1">
        <p className="flex flex-wrap items-baseline gap-x-2">
          <span className="text-sm leading-5 font-semibold wrap-anywhere text-ink">{comment.author.fullName}</span>
          <CommentTime instant={comment.createdAt} timeZone={timeZone} />
        </p>
        {activity && (
          <div className="mt-1 flex">
            <ActivityChip activity={activity} />
          </div>
        )}
        <p className="mt-0.5 text-sm leading-5 whitespace-pre-wrap wrap-anywhere text-gray-800">{comment.content}</p>
        {children}
      </div>
    </li>
  )
}

/**
 * "{activity} ×" at the top of a panel opened from an activity card, staying in view while the list scrolls (Stitch
 * mockup). "×" shows every comment of the trip again. An activity the trip no longer has (deleted meanwhile) still
 * gets the chip, without a name.
 */
function ActivityFilter({ activity }: { activity: Activity | undefined }) {
  const showAllComments = useCommentPanelStore((state) => state.showAllComments)
  const label = activity?.title ?? 'Hoạt động không còn trong lịch trình'
  return (
    // Pinned to the top of the scrolling body, on white so the comments pass under it
    // -mt-4 / -mx-5 cancel the padding of the body, so it sits flush under the panel header
    <div className="sticky top-0 z-10 -mx-5 -mt-4 border-b border-tide bg-white px-5 py-3">
      <span className="inline-flex min-h-7 max-w-full min-w-0 items-center gap-1.5 rounded-full border border-tide bg-white pr-1 pl-2.5 text-[13px] leading-5 text-ink">
        {activity && (
          <span aria-hidden className={`size-2 shrink-0 rounded-full ${ACTIVITY_ROUTE[activity.type].marker}`} />
        )}
        <span className="sr-only">Đang xem bình luận về hoạt động: </span>
        <span className="truncate" title={label}>
          {label}
        </span>
        <button
          type="button"
          onClick={showAllComments}
          aria-label="Xem mọi bình luận của chuyến đi"
          className="flex size-6 shrink-0 items-center justify-center rounded-full text-gray-500 hover:bg-gray-100 hover:text-gray-900 focus-visible:ring-[3px] focus-visible:ring-jade/25 focus-visible:outline-none pointer-coarse:size-9"
        >
          <X aria-hidden className="size-3.5" />
        </button>
      </span>
    </div>
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
