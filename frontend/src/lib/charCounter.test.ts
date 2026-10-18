import { describe, expect, it } from 'vitest'
import { charCounter } from './charCounter'

describe('charCounter', () => {
  it('shows nothing while less than 80% of the limit is used', () => {
    expect(charCounter(0, 255)).toBeNull()
    expect(charCounter(203, 255)).toBeNull()
  })

  it('shows the count from 80% of the limit, rounded up', () => {
    // 80% of 255 is 204
    expect(charCounter(204, 255)).toEqual({ text: '204 / 255', full: false })
    expect(charCounter(800, 1000)).toEqual({ text: '800 / 1000', full: false })
  })

  it('marks the counter full once the limit is reached', () => {
    expect(charCounter(255, 255)).toEqual({ text: '255 / 255', full: true })
  })

  it('shows nothing for a field without a limit', () => {
    expect(charCounter(5000, undefined)).toBeNull()
    expect(charCounter(10, 0)).toBeNull()
  })
})
