import { useEffect, useRef, useState, type ChangeEvent, type KeyboardEvent } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Send, X } from 'lucide-react'
import { commentsKey, createComment, createCommentKey } from '../../api/comments'
import { applyFieldErrors, getApiError, getErrorMessage } from '../../api/errors'
import { Alert } from '../../components/Alert'
import { Button } from '../../components/Button'
import { TextAreaField } from '../../components/TextAreaField'
import { useMediaQuery } from '../../hooks/useMediaQuery'
import { draftFor, useCommentPanelStore } from '../../stores/commentPanelStore'
import type { Comment } from '../../types/comment'
import { COMMENT_MAX_LENGTH, commentSchema, type CommentValues } from './schemas'

const FORM_FIELDS = ['content'] as const

/** Below this width the send button takes the whole row, easier to reach with a thumb (Stitch mockup, Task 4.6) */
const PHONE = '(max-width: 639px)'

interface CommentFormProps {
  tripId: number
  /** The activity the panel is showing (opened from its card): a new comment is about it. null for the trip */
  activityId: number | null
  /** The root comment being answered ("Trả lời" in the list), or null for a new comment */
  replyTo: Comment | null
  /** "×" on the reply chip: back to a new comment, the text typed stays */
  onCancelReply: () => void
  /** The comment is stored and in the list: the panel scrolls to it */
  onSent: (comment: Comment) => void
}

/** What is sent: the box, plus the root answered or the activity the comment is about */
type SendValues = CommentValues & { parentId?: number; activityId?: number }

/**
 * The one comment box of the panel, fixed at its bottom (UI_GUIDE 8.1): "Viết bình luận…" and the panel's only
 * primary button "Gửi bình luận". Ctrl+Enter (⌘+Enter) sends, Enter goes to a new line. Checked when sending only,
 * like the invite form (BUG-UI-012): somebody who only came to read must not be told the box is empty. A sent
 * comment joins the list at once; the box starts again empty. A refusal of the server shows under the box (its
 * rule on content) or above it (anything else, such as a root deleted meanwhile), and the text typed stays.
 * <p>
 * Answering does not open a second box (one primary button per screen): "Trả lời {tên} ×" shows above this one and
 * the cursor moves into it.
 */
export function CommentForm({ tripId, activityId, replyTo, onCancelReply, onSent }: CommentFormProps) {
  const queryClient = useQueryClient()
  const onPhone = useMediaQuery(PHONE)
  const form = useRef<HTMLFormElement>(null)
  // Bumped after each comment sent: the box is mounted again, empty, because TextAreaField reads its value (for its
  // height and counter) only when it is mounted
  const [round, setRound] = useState(0)
  const {
    register,
    handleSubmit,
    setError,
    setFocus,
    reset,
    formState: { errors },
  } = useForm<CommentValues>({
    mode: 'onSubmit',
    reValidateMode: 'onChange',
    resolver: zodResolver(commentSchema),
    // The text left when the panel was closed (Task 4.6 Mốc 12): read once, when the panel opens
    defaultValues: { content: draftFor(tripId) },
  })

  // Every keystroke is kept, so closing the panel (a click outside, Esc) loses nothing
  const contentField = register('content', {
    onChange: (event: ChangeEvent<HTMLTextAreaElement>) =>
      useCommentPanelStore.getState().saveDraft(tripId, event.target.value),
  })

  const mutation = useMutation({
    mutationKey: createCommentKey(tripId),
    mutationFn: ({ content, parentId, activityId }: SendValues) =>
      createComment(tripId, { content, parentId, activityId }),
    onSuccess: (created) => {
      // Shown at once from the answer; the reload that follows also brings what others wrote meanwhile
      queryClient.setQueryData<Comment[]>(commentsKey(tripId), (comments) => comments && [...comments, created])
      void queryClient.invalidateQueries({ queryKey: commentsKey(tripId) })
      // Empty, not back to the draft the box started with; sent, so no draft any more
      reset({ content: '' })
      useCommentPanelStore.getState().clearDraft()
      setRound((value) => value + 1)
      onSent(created)
    },
    onError: (error) => applyFieldErrors(error, setError, FORM_FIELDS),
  })

  // The new, empty box keeps the cursor, ready for the next comment
  useEffect(() => {
    if (round > 0) setFocus('content')
  }, [round, setFocus])

  // "Trả lời" pressed in the list: straight to the box
  useEffect(() => {
    if (replyTo) setFocus('content')
  }, [replyTo, setFocus])

  const onContentField = (getApiError(mutation.error)?.details ?? []).some((detail) => detail.field === 'content')
  const bannerError = mutation.isError && !onContentField ? getErrorMessage(mutation.error) : undefined

  function sendOnCtrlEnter(event: KeyboardEvent<HTMLTextAreaElement>) {
    // isComposing: Enter that ends a word typed with an input method (Vietnamese Telex...) is not a send
    if (event.key === 'Enter' && (event.ctrlKey || event.metaKey) && !event.nativeEvent.isComposing) {
      event.preventDefault()
      form.current?.requestSubmit()
    }
  }

  return (
    <form
      ref={form}
      noValidate
      className="space-y-3"
      onSubmit={handleSubmit((values) => {
        // Ctrl+Enter pressed again while the first one is on its way
        if (mutation.isPending) return
        mutation.reset()
        // A reply sits on the activity of its root, the server takes it from there: only a new comment names one
        mutation.mutate(
          replyTo ? { ...values, parentId: replyTo.id } : { ...values, activityId: activityId ?? undefined },
        )
      })}
    >
      {bannerError && <Alert variant="error">{bannerError}</Alert>}
      {replyTo && (
        <ReplyChip
          name={replyTo.author.fullName}
          onRemove={() => {
            onCancelReply()
            setFocus('content')
          }}
        />
      )}
      <TextAreaField
        key={round}
        label="Viết bình luận"
        hideLabel
        // Fixed at 3 lines: a box growing at the bottom of the panel would squeeze the list above it
        autoGrow={false}
        placeholder="Viết bình luận…"
        maxLength={COMMENT_MAX_LENGTH}
        error={errors.content?.message}
        onKeyDown={sendOnCtrlEnter}
        {...contentField}
      />
      <div className="flex justify-end">
        <Button type="submit" fullWidth={onPhone} isLoading={mutation.isPending}>
          <Send aria-hidden className="size-4" />
          Gửi bình luận
        </Button>
      </div>
    </form>
  )
}

/** "Trả lời Mai Anh ×" above the box, light teal like a chosen item (Stitch mockup); a long name is cut with "…". */
function ReplyChip({ name, onRemove }: { name: string; onRemove: () => void }) {
  return (
    <div className="flex">
      <span className="inline-flex min-h-6 max-w-full min-w-0 items-center gap-1 rounded-full bg-jade-light pr-0.5 pl-2.5 text-xs leading-4 text-jade-dark">
        <span className="truncate">
          Trả lời <span className="font-semibold">{name}</span>
        </span>
        <button
          type="button"
          onClick={onRemove}
          aria-label={`Bỏ trả lời ${name}`}
          className="flex size-5 shrink-0 items-center justify-center rounded-full hover:bg-jade/15 focus-visible:ring-[3px] focus-visible:ring-jade/25 focus-visible:outline-none pointer-coarse:size-8"
        >
          <X aria-hidden className="size-3" />
        </button>
      </span>
    </div>
  )
}
