import { useEffect, useRef, useState, type KeyboardEvent } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Send } from 'lucide-react'
import { commentsKey, createComment, createCommentKey } from '../../api/comments'
import { applyFieldErrors, getApiError, getErrorMessage } from '../../api/errors'
import { Alert } from '../../components/Alert'
import { Button } from '../../components/Button'
import { TextAreaField } from '../../components/TextAreaField'
import { useMediaQuery } from '../../hooks/useMediaQuery'
import type { Comment } from '../../types/comment'
import { COMMENT_MAX_LENGTH, commentSchema, type CommentValues } from './schemas'

const FORM_FIELDS = ['content'] as const

/** Below this width the send button takes the whole row, easier to reach with a thumb (Stitch mockup, Task 4.6) */
const PHONE = '(max-width: 639px)'

interface CommentFormProps {
  tripId: number
  /** The comment is stored and in the list: the panel scrolls down to it */
  onSent: () => void
}

/**
 * The one comment box of the panel, fixed at its bottom (UI_GUIDE 8.1): "Viết bình luận…" and the panel's only
 * primary button "Gửi bình luận". Ctrl+Enter (⌘+Enter) sends, Enter goes to a new line. Checked when sending only,
 * like the invite form (BUG-UI-012): somebody who only came to read must not be told the box is empty. A sent
 * comment joins the list at once; the box starts again empty. A refusal of the server shows under the box (its
 * rule on content) or above it (anything else), and the text typed stays.
 */
export function CommentForm({ tripId, onSent }: CommentFormProps) {
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
    defaultValues: { content: '' },
  })

  const mutation = useMutation({
    mutationKey: createCommentKey(tripId),
    mutationFn: (values: CommentValues) => createComment(tripId, { content: values.content }),
    onSuccess: (created) => {
      // Shown at once from the answer; the reload that follows also brings what others wrote meanwhile
      queryClient.setQueryData<Comment[]>(commentsKey(tripId), (comments) => comments && [...comments, created])
      void queryClient.invalidateQueries({ queryKey: commentsKey(tripId) })
      reset()
      setRound((value) => value + 1)
      onSent()
    },
    onError: (error) => applyFieldErrors(error, setError, FORM_FIELDS),
  })

  // The new, empty box keeps the cursor, ready for the next comment
  useEffect(() => {
    if (round > 0) setFocus('content')
  }, [round, setFocus])

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
        mutation.mutate(values)
      })}
    >
      {bannerError && <Alert variant="error">{bannerError}</Alert>}
      <TextAreaField
        key={round}
        label="Viết bình luận"
        hideLabel
        placeholder="Viết bình luận…"
        maxLength={COMMENT_MAX_LENGTH}
        error={errors.content?.message}
        onKeyDown={sendOnCtrlEnter}
        {...register('content')}
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
