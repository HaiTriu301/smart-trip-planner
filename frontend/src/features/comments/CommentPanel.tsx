import { useEffect, useMemo, useRef, useState, type ReactNode } from 'react'
import { useIsMutating, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ChevronRight, MapPin, MessageCircle, Trash2, X } from 'lucide-react'
import { useLocation, useNavigate } from 'react-router-dom'
import { commentsKey, createCommentKey, deleteComment, listComments } from '../../api/comments'
import { getErrorMessage } from '../../api/errors'
import { Alert } from '../../components/Alert'
import { Avatar } from '../../components/Avatar'
import { Button } from '../../components/Button'
import { ConfirmDialog } from '../../components/ConfirmDialog'
import { Modal } from '../../components/Modal'
import { Skeleton } from '../../components/Skeleton'
import { useNow } from '../../hooks/useNow'
import {
  activitiesById,
  activitiesUnderDiscussion,
  commentsAbout,
  commentThreads,
  splitComments,
  type DiscussedDay,
} from '../../lib/commentTree'
import { formatDate, formatDateTimeIn, formatWeekday } from '../../lib/format'
import { relativeTime } from '../../lib/relativeTime'
import { DEFAULT_TIME_ZONE } from '../../lib/today'
import { useAuthStore } from '../../stores/authStore'
import { useCommentPanelStore, type CommentTab } from '../../stores/commentPanelStore'
import { useMapLinkStore } from '../../stores/mapLinkStore'
import { toast } from '../../stores/toastStore'
import type { Activity } from '../../types/activity'
import type { Comment } from '../../types/comment'
import type { TripRole } from '../../types/member'
import type { TripDayDetail, TripDetail } from '../../types/trip'
import { ACTIVITY_ROUTE } from '../itinerary/activityType'
import { CommentForm } from './CommentForm'
import { canDeleteComment, withoutDeleted } from './commentRules'

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
  const tab = useCommentPanelStore((state) => state.tab)
  const activityId = useCommentPanelStore((state) => state.activityId)
  const { openPanel, closePanel, showTab } = useCommentPanelStore.getState()
  // The root comment being answered: "Trả lời" under it in the list, the chip above the box in the footer
  const [replyTo, setReplyTo] = useState<Comment | null>(null)
  // The comment sent last: the list scrolls to it, wherever it lands (the end, or under its root for a reply)
  const [sentId, setSentId] = useState<number | null>(null)
  const { data: comments } = useQuery({ queryKey: commentsKey(tripId), queryFn: () => listComments(tripId) })
  // Esc and "×" wait for the server: closing would leave its answer (an error under the box) nowhere (BUG-UI-004)
  const isSending = useIsMutating({ mutationKey: createCommentKey(tripId) }) > 0
  const count = comments?.length ?? 0

  const navigate = useNavigate()
  const location = useLocation()

  // Leaving the trip page, or going to another trip, closes the panel: the stores outlive this page. The draft of
  // the comment box and a card asked for from a comment and never shown (its activity moved meanwhile) go too
  useEffect(
    () => () => {
      const panel = useCommentPanelStore.getState()
      panel.closePanel()
      panel.clearDraft()
      useMapLinkStore.getState().clearPendingReveal()
    },
    [tripId],
  )

  function close() {
    if (isSending) return
    closePanel()
    setReplyTo(null)
  }

  // "Xem trong lịch trình": close the panel, go to the day of the activity and show its card there (UI_GUIDE 8.1)
  function showActivity(activity: Activity) {
    const day = trip.days.find((d) => d.id === activity.dayId)
    if (!day || isSending) return
    close()
    useMapLinkStore.getState().revealWhenShown(activity.id)
    const path = `/trips/${tripId}/days/${day.dayIndex}`
    // Its own day already on screen: no new history entry, the card is just revealed
    if (location.pathname !== path) navigate(path)
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
          // A comment on an activity is written from that activity: the "Hoạt động" tab only reads
          activityId === null && tab === 'activities' ? (
            <p className="text-sm text-gray-600">Chọn một hoạt động để xem và viết bình luận.</p>
          ) : (
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
          )
        }
      >
        <CommentList
          tripId={tripId}
          days={trip.days}
          myRole={trip.myRole}
          tab={tab}
          activityId={activityId}
          sentId={sentId}
          onReply={setReplyTo}
          // A reply belongs to the comment it was started on: another tab drops it (the text typed stays)
          onShowTab={(next) => {
            setReplyTo(null)
            showTab(next)
          }}
          onOpenActivity={(id) => {
            setReplyTo(null)
            openPanel(id)
          }}
          onShowActivity={showActivity}
          // The comment being answered is gone: the box goes back to a new comment
          onDeleted={(commentId) => setReplyTo((current) => (current?.id === commentId ? null : current))}
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
 * Two tabs (Task 4.6 decision 15): "Chung", the comments on the whole trip, a conversation with the box below;
 * "Hoạt động", the activities that have comments, by day, each opening its own comments. One activity (from that
 * list or from its card) shows its comments under a chip "{activity} ×" that goes back to the "Hoạt động" tab and a
 * button to that activity in the itinerary, with the box below for a comment about it.
 * <p>
 * Its author and the owner of the trip can delete a comment, after a confirmation that says when its replies go
 * with it. The comment leaves the list at once, then the list is reloaded.
 */
function CommentList({
  tripId,
  days,
  myRole,
  tab,
  activityId,
  sentId,
  onReply,
  onShowTab,
  onOpenActivity,
  onShowActivity,
  onDeleted,
}: {
  tripId: number
  /** The days of the trip as the page has them: where the activity of a comment is looked up */
  days: TripDayDetail[]
  /** What the reader is to the trip: the owner may delete every comment */
  myRole: TripRole
  /** The tab shown while no single activity is open */
  tab: CommentTab
  /** Only the comments about this activity; null for a tab */
  activityId: number | null
  sentId: number | null
  onReply: (comment: Comment) => void
  onShowTab: (tab: CommentTab) => void
  /** A row of the "Hoạt động" tab: the comments of that activity */
  onOpenActivity: (activityId: number) => void
  /** "Xem trong lịch trình" at the top of one activity: its card, on its day */
  onShowActivity: (activity: Activity) => void
  onDeleted: (commentId: number) => void
}) {
  const queryClient = useQueryClient()
  const userId = useAuthStore((state) => state.user?.id ?? null)
  // The comment waiting for "Xoá bình luận", with how many replies go with it
  const [deleting, setDeleting] = useState<{ comment: Comment; replyCount: number } | null>(null)
  const deletion = useMutation({
    mutationFn: (comment: Comment) => deleteComment(tripId, comment.id),
    onSuccess: (_, comment) => {
      queryClient.setQueryData<Comment[]>(commentsKey(tripId), (list) => list && withoutDeleted(list, comment.id))
      void queryClient.invalidateQueries({ queryKey: commentsKey(tripId) })
      onDeleted(comment.id)
      setDeleting(null)
      toast.success('Đã xoá bình luận')
    },
    // Already deleted in another tab (404), or no longer allowed (403): the list as the server has it now
    onError: () => void queryClient.invalidateQueries({ queryKey: commentsKey(tripId) }),
  })

  function askToDelete(comment: Comment, replyCount: number) {
    deletion.reset()
    setDeleting({ comment, replyCount })
  }

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

  // The "Hoạt động" tab is a list of activities read from the top (day 1 first), not a conversation
  const conversation = !(activityId === null && tab === 'activities')
  useEffect(() => {
    if (!comments || (conversation && !atEnd.current)) return
    // The dialog is still closed while this runs on opening (its showModal() comes after the effects of its
    // content) and a closed dialog has no size to scroll: wait for the next frame
    const frame = requestAnimationFrame(() => {
      const body = end.current?.closest<HTMLElement>('[data-modal-body]')
      if (body) body.scrollTop = conversation ? body.scrollHeight : 0
    })
    return () => cancelAnimationFrame(frame)
    // tab, activityId: the list changes with them, without a new array from the server
  }, [comments, tab, activityId, conversation])

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

  const { trip: onTrip, activities: onActivities } = splitComments(comments)
  // What is on screen: one activity (opened from its card or from the "Hoạt động" tab), or one of the two tabs
  const view = activityId !== null ? 'activity' : tab
  const threads = commentThreads(activityId !== null ? commentsAbout(comments, activityId) : onTrip)
  const openActivity = activityId === null ? undefined : activities.get(activityId)
  return (
    <div className="space-y-4">
      {activityId !== null ? (
        <ActivityFilter
          activity={openActivity}
          onBack={() => onShowTab('activities')}
          onShowInItinerary={openActivity && (() => onShowActivity(openActivity))}
        />
      ) : (
        <CommentTabs tab={tab} tripCount={onTrip.length} activityCount={onActivities.length} onChange={onShowTab} />
      )}
      {/* A reload failed while comments were on screen: keep them (BUG-UI-005) */}
      {error && (
        <div className="space-y-3">
          <Alert variant="error">{`${getErrorMessage(error)}. Đang hiển thị bình luận đã tải trước đó.`}</Alert>
          {retry}
        </div>
      )}
      {view === 'activities' ? (
        <DiscussedActivities
          discussed={activitiesUnderDiscussion(onActivities, days)}
          timeZone={timeZone}
          onOpen={onOpenActivity}
        />
      ) : threads.length === 0 ? (
        <p className="text-sm text-gray-500">{EMPTY_TEXT[view]}</p>
      ) : (
        <ul className="space-y-4">
          {threads.map(({ comment, replies }) => (
            <CommentItem
              key={comment.id}
              comment={comment}
              timeZone={timeZone}
              onDelete={
                canDeleteComment(comment, userId, myRole) ? () => askToDelete(comment, replies.length) : undefined
              }
              action={
                <button
                  type="button"
                  onClick={() => onReply(comment)}
                  aria-label={`Trả lời bình luận của ${comment.author.fullName}`}
                  // 44px tall on touch screens (UI_GUIDE 12)
                  className="mt-1 inline-flex items-center rounded-control text-xs leading-4 font-semibold text-gray-500 hover:text-ink focus-visible:ring-[3px] focus-visible:ring-jade/25 focus-visible:outline-none pointer-coarse:min-h-11"
                >
                  Trả lời
                </button>
              }
            >
              {replies.length > 0 && (
                // Under the text of the root, not under its picture: 32px picture + 12px gap
                <ul aria-label={`Trả lời bình luận của ${comment.author.fullName}`} className="mt-3 ml-11 space-y-3">
                  {replies.map((reply) => (
                    <CommentItem
                      key={reply.id}
                      comment={reply}
                      timeZone={timeZone}
                      isReply
                      onDelete={canDeleteComment(reply, userId, myRole) ? () => askToDelete(reply, 0) : undefined}
                    />
                  ))}
                </ul>
              )}
            </CommentItem>
          ))}
        </ul>
      )}
      <div ref={end} />

      {/* Over the panel; Esc closes this one only (BUG-UI-011) */}
      <ConfirmDialog
        open={deleting !== null}
        title="Xoá bình luận?"
        confirmLabel="Xoá bình luận"
        variant="danger"
        isLoading={deletion.isPending}
        error={deletion.isError ? getErrorMessage(deletion.error) : undefined}
        onCancel={() => setDeleting(null)}
        onConfirm={() => deleting && deletion.mutate(deleting.comment)}
      >
        <p>Bình luận sẽ bị xoá và không khôi phục được.</p>
        {deleting && deleting.replyCount > 0 && <p>Các trả lời của bình luận này cũng bị xoá.</p>}
      </ConfirmDialog>
    </div>
  )
}

interface CommentItemProps {
  comment: Comment
  timeZone: string
  /** A reply: smaller picture, and it never has replies of its own (one level, design.md rule 27) */
  isReply?: boolean
  /** Present when the reader may delete it (author or owner): the trash button at the end of the name row */
  onDelete?: () => void
  /** Under the text, inside the comment: "Trả lời" of a root comment */
  action?: ReactNode
  /** Under the comment, outside its hover area: the replies of a root comment */
  children?: ReactNode
}

/**
 * Picture, name and date on one line, then the text: plain, line breaks kept, long words broken. The pointer on a
 * comment tints it light gray (Stitch mockup), so the trash button that shows up is plainly its own; a reply is
 * tinted alone, not with its root.
 */
function CommentItem({ comment, timeZone, isReply = false, onDelete, action, children }: CommentItemProps) {
  const name = comment.author.fullName
  return (
    <li data-comment-id={comment.id}>
      {/* The negative margins let the tint reach past the text without moving anything */}
      <div
        className={`group/comment -mx-2 -my-1.5 flex rounded-control px-2 py-1.5 transition-colors hover:bg-gray-50 ${
          isReply ? 'gap-2' : 'gap-3'
        }`}
      >
        <Avatar name={name} url={comment.author.avatarUrl} size={isReply ? 'sm' : 'md'} />
        <div className="min-w-0 flex-1">
          <div className="flex items-start gap-2">
            <p className="flex min-w-0 flex-1 flex-wrap items-baseline gap-x-2">
              <span className="text-sm leading-5 font-semibold wrap-anywhere text-ink">{name}</span>
              <CommentTime instant={comment.createdAt} timeZone={timeZone} />
            </p>
            {onDelete && (
              <button
                type="button"
                onClick={onDelete}
                aria-label={`Xoá bình luận của ${name}`}
                // Shown with the pointer or the keyboard on the comment, always on touch screens (no hover there)
                className="-my-1 flex size-7 shrink-0 items-center justify-center rounded-control text-gray-500 opacity-0 transition-opacity group-focus-within/comment:opacity-100 group-hover/comment:opacity-100 hover:bg-gray-100 hover:text-danger focus-visible:ring-[3px] focus-visible:ring-jade/25 focus-visible:outline-none pointer-coarse:size-11 pointer-coarse:opacity-100"
              >
                <Trash2 aria-hidden className="size-4" />
              </button>
            )}
          </div>
          <p className="mt-0.5 text-sm leading-5 whitespace-pre-wrap wrap-anywhere text-gray-800">{comment.content}</p>
          {action}
        </div>
      </div>
      {children}
    </li>
  )
}

// The top of the panel body, pinned while the list scrolls, on white so the comments pass under it. -mt-4 / -mx-5
// cancel the padding of the body, so it sits flush under the panel header
// The pinned offset is -top-4, not top-0: a sticky box sticks inside the padding of its scroll container, so top-0
// would push it 16px back down, leaving a gap above it and covering the first 16px of the list (BUG-UI-013)
const PANEL_TOP = 'sticky -top-4 z-10 -mx-5 -mt-4 border-b border-tide bg-white px-5 py-3'

const EMPTY_TEXT: Record<'trip' | 'activities' | 'activity', string> = {
  trip: 'Chưa có bình luận chung nào. Hỏi hoặc góp ý cho cả nhóm ở ô bên dưới.',
  activities: 'Chưa có bình luận nào về các hoạt động.',
  activity: 'Chưa có bình luận nào về hoạt động này.',
}

const TABS: readonly { tab: CommentTab; label: string }[] = [
  { tab: 'trip', label: 'Chung' },
  { tab: 'activities', label: 'Hoạt động' },
]

interface CommentTabsProps {
  tab: CommentTab
  tripCount: number
  activityCount: number
  onChange: (tab: CommentTab) => void
}

/**
 * "Chung · 4 | Hoạt động · 7" at the top of the panel (Task 4.6 decision 15): the look of SegmentedControl (UI_GUIDE
 * 7.15, gray track, the chosen one raised in white), built as two toggle buttons like the list / map switch of the
 * trip page, since it switches what is shown and belongs to no form. Counts include replies; none when 0.
 */
function CommentTabs({ tab, tripCount, activityCount, onChange }: CommentTabsProps) {
  const counts: Record<CommentTab, number> = { trip: tripCount, activities: activityCount }
  return (
    <div className={PANEL_TOP}>
      <div role="group" aria-label="Loại bình luận" className="grid grid-cols-2 gap-1 rounded-control bg-gray-200 p-1">
        {TABS.map((option) => {
          const chosen = option.tab === tab
          return (
            <button
              key={option.tab}
              type="button"
              aria-pressed={chosen}
              onClick={() => onChange(option.tab)}
              className={`inline-flex h-8 items-center justify-center gap-1 rounded-[4px] text-sm transition-[color,background-color,box-shadow] focus-visible:ring-[3px] focus-visible:ring-jade/25 focus-visible:outline-none pointer-coarse:h-11 ${
                chosen ? 'bg-white font-medium text-ink shadow-sm' : 'text-gray-600 hover:text-ink'
              }`}
            >
              {option.label}
              {counts[option.tab] > 0 && <span className="tabular">· {counts[option.tab]}</span>}
            </button>
          )
        })}
      </div>
    </div>
  )
}

interface DiscussedActivitiesProps {
  discussed: DiscussedDay[]
  timeZone: string
  onOpen: (activityId: number) => void
}

/**
 * The "Hoạt động" tab (Task 4.6 decision 15): the activities people are talking about, grouped by day in the order
 * of the itinerary. One compact row each: the dot of its type and its title, how many comments, and the newest one
 * cut to a line with its time. A row opens the comments of that activity, where they can be answered.
 */
function DiscussedActivities({ discussed, timeZone, onOpen }: DiscussedActivitiesProps) {
  if (discussed.length === 0) return <p className="text-sm text-gray-500">{EMPTY_TEXT.activities}</p>
  return (
    <div className="space-y-4">
      {discussed.map(({ day, activities }) => (
        <section key={day.id} aria-labelledby={`discussed-day-${day.id}`}>
          <h3 id={`discussed-day-${day.id}`} className="tabular text-xs leading-4 font-semibold text-gray-500">
            Ngày {day.dayIndex} · {formatWeekday(day.date)}, {formatDate(day.date).slice(0, 5)}
          </h3>
          <ul className="mt-1.5 space-y-1">
            {activities.map(({ activity, count, latest }) => (
              <li key={activity.id}>
                <button
                  type="button"
                  onClick={() => onOpen(activity.id)}
                  aria-label={`${activity.title}, ${count} bình luận. Mới nhất của ${latest.author.fullName}`}
                  // The negative margins let the hover tint reach past the text without moving it
                  className="-mx-2 block w-[calc(100%+1rem)] rounded-control px-2 py-1.5 text-left transition-colors hover:bg-gray-50 focus-visible:ring-[3px] focus-visible:ring-jade/25 focus-visible:outline-none"
                >
                  <span className="flex items-center gap-2">
                    <span aria-hidden className={`size-2 shrink-0 rounded-full ${ACTIVITY_ROUTE[activity.type].marker}`} />
                    <span className="min-w-0 flex-1 truncate text-sm leading-5 font-semibold text-ink">
                      {activity.title}
                    </span>
                    <span className="tabular shrink-0 text-xs leading-4 text-gray-500">{count}</span>
                    <ChevronRight aria-hidden className="size-4 shrink-0 text-gray-400" />
                  </span>
                  {/* Under the title, past the dot: who wrote last, what, and when, on one line */}
                  <span className="mt-0.5 flex items-baseline gap-1 pl-4 text-[13px] leading-5 text-gray-500">
                    <span className="min-w-0 truncate">
                      {latest.author.fullName}: {latest.content.replace(/\s+/g, ' ')}
                    </span>
                    <span aria-hidden className="shrink-0">
                      ·
                    </span>
                    <span className="shrink-0">
                      <CommentTime instant={latest.createdAt} timeZone={timeZone} />
                    </span>
                  </span>
                </button>
              </li>
            ))}
          </ul>
        </section>
      ))}
    </div>
  )
}

interface ActivityFilterProps {
  /** undefined: an activity the trip no longer has (deleted meanwhile) */
  activity: Activity | undefined
  /** "×": back to the "Hoạt động" tab */
  onBack: () => void
  /** "Xem trong lịch trình"; absent when the activity is gone */
  onShowInItinerary?: () => void
}

/**
 * The top of a panel opened on one activity (from its card, or from the "Hoạt động" tab): the chip "{activity} ×"
 * (Stitch mockup), whose "×" goes back to the "Hoạt động" tab, and "Xem trong lịch trình", which closes the panel and
 * shows the card on its day (what the chip of each comment did before the tabs, Task 4.6 decision 16). An activity
 * the trip no longer has still gets the chip, without a name.
 */
function ActivityFilter({ activity, onBack, onShowInItinerary }: ActivityFilterProps) {
  const label = activity?.title ?? 'Hoạt động không còn trong lịch trình'
  return (
    <div className={`${PANEL_TOP} flex items-center gap-2`}>
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
          onClick={onBack}
          aria-label="Quay về danh sách bình luận về các hoạt động"
          className="flex size-6 shrink-0 items-center justify-center rounded-full text-gray-500 hover:bg-gray-100 hover:text-gray-900 focus-visible:ring-[3px] focus-visible:ring-jade/25 focus-visible:outline-none pointer-coarse:size-9"
        >
          <X aria-hidden className="size-3.5" />
        </button>
      </span>
      {onShowInItinerary && (
        <button
          type="button"
          onClick={onShowInItinerary}
          className="ml-auto inline-flex shrink-0 items-center gap-1 rounded-control px-1.5 py-1 text-xs leading-4 font-semibold whitespace-nowrap text-jade-dark hover:bg-jade-light focus-visible:ring-[3px] focus-visible:ring-jade/25 focus-visible:outline-none pointer-coarse:min-h-11"
        >
          <MapPin aria-hidden className="size-3.5" />
          Xem trong lịch trình
        </button>
      )}
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
