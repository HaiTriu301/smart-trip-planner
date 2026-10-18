import { describe, expect, it } from 'vitest'
import { initials } from './initials'

describe('initials', () => {
  it.each([
    ['Mai Anh', 'MA'],
    ['Trần Nhật Triều', 'TT'],
    ['đặng văn ân', 'ĐÂ'],
    ['Trieu', 'T'],
    ['  Lê   Hoàng  ', 'LH'],
  ])('takes the first and last word of "%s"', (name, expected) => {
    expect(initials(name, 'someone@example.com')).toBe(expected)
  })

  it('keeps a letter typed with a separate accent mark as one letter', () => {
    // "Ê" written as "E" followed by the combining circumflex, as some keyboards send it
    const decomposed = 'Ê'.normalize('NFD') + 'm Út'.normalize('NFD')
    expect(initials(decomposed, 'em@example.com')).toBe('ÊÚ')
  })

  it.each([null, '', '   '])('falls back to the email when the name is %j', (name) => {
    expect(initials(name, 'quangkhoi.dev@gmail.com')).toBe('Q')
  })
})
