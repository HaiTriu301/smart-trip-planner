/**
 * Up to two capital letters for an avatar without a picture: the first letters of the first and the last word
 * of the name ("Trần Nhật Triều" → "TT", "Mai Anh" → "MA"), one letter for a one-word name, and the first letter
 * of the email for an invited person who has no account (no name) yet.
 * Text is normalised first, so a letter typed with a separate accent mark ("e" + "̂") stays one letter ("Ê").
 */
export function initials(fullName: string | null, email: string): string {
  const words = (fullName ?? '').normalize('NFC').trim().split(/\s+/).filter(Boolean)
  if (words.length === 0) return firstLetter(email)
  if (words.length === 1) return firstLetter(words[0])
  return firstLetter(words[0]) + firstLetter(words[words.length - 1])
}

function firstLetter(text: string): string {
  const [first = '?'] = Array.from(text.normalize('NFC').trim())
  return first.toLocaleUpperCase('vi')
}
