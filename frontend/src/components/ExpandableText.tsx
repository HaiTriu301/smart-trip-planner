import { useEffect, useRef, useState } from 'react'

interface ExpandableTextProps {
  text: string
  /** Text styles (size, colour); layout and clamping are handled here */
  className?: string
}

/**
 * Multi-line user text shown on at most two lines. "Đọc thêm" appears only when the text really is longer,
 * and toggles to "Thu gọn". Line breaks typed by the user are kept; long words without spaces wrap.
 */
export function ExpandableText({ text, className }: ExpandableTextProps) {
  const ref = useRef<HTMLParagraphElement>(null)
  const [expanded, setExpanded] = useState(false)
  const [overflowing, setOverflowing] = useState(false)

  useEffect(() => {
    const element = ref.current
    // While expanded nothing is clipped, so the last measurement (overflowing) is kept for "Thu gọn"
    if (!element || expanded) return
    // A ResizeObserver reports once right after observe(), then on every width change (window resize)
    const observer = new ResizeObserver(() => {
      setOverflowing(element.scrollHeight > element.clientHeight + 1)
    })
    observer.observe(element)
    return () => observer.disconnect()
  }, [text, expanded])

  return (
    <div>
      <p
        ref={ref}
        className={`whitespace-pre-line wrap-anywhere ${expanded ? '' : 'line-clamp-2'} ${className ?? ''}`}
      >
        {text}
      </p>
      {overflowing && (
        <button
          type="button"
          aria-expanded={expanded}
          onClick={() => setExpanded(!expanded)}
          className="text-sm font-medium text-sky-700 hover:underline"
        >
          {expanded ? 'Thu gọn' : 'Đọc thêm'}
        </button>
      )}
    </div>
  )
}
