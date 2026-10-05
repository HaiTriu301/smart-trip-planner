import { describe, expect, it } from 'vitest'
import { cardForecastTarget } from './cardForecast'

const TODAY = '2026-10-13'

describe('cardForecastTarget', () => {
  it('shows today for a trip in progress', () => {
    expect(cardForecastTarget({ startDate: '2026-10-12', endDate: '2026-10-15' }, TODAY)).toEqual({
      date: TODAY,
      kind: 'today',
    })
  })

  it.each([
    ['starts today', '2026-10-13', '2026-10-16'],
    ['ends today', '2026-10-10', '2026-10-13'],
    ['lasts one day, today', '2026-10-13', '2026-10-13'],
  ])('counts a trip that %s as in progress', (_name, startDate, endDate) => {
    expect(cardForecastTarget({ startDate, endDate }, TODAY)).toEqual({ date: TODAY, kind: 'today' })
  })

  it('shows the first day for a trip that starts tomorrow', () => {
    expect(cardForecastTarget({ startDate: '2026-10-14', endDate: '2026-10-18' }, TODAY)).toEqual({
      date: '2026-10-14',
      kind: 'departure',
    })
  })

  it('still shows the first day when it is the last day of the forecast window', () => {
    // 13 October + 15 days: the 16th day counting today
    expect(cardForecastTarget({ startDate: '2026-10-28', endDate: '2026-11-02' }, TODAY)).toEqual({
      date: '2026-10-28',
      kind: 'departure',
    })
  })

  it('shows nothing for a trip that starts one day after the forecast window', () => {
    expect(cardForecastTarget({ startDate: '2026-10-29', endDate: '2026-11-02' }, TODAY)).toBeNull()
  })

  it('shows nothing for a trip that ended yesterday', () => {
    expect(cardForecastTarget({ startDate: '2026-10-09', endDate: '2026-10-12' }, TODAY)).toBeNull()
  })

  it('counts the window across the end of a year', () => {
    expect(cardForecastTarget({ startDate: '2027-01-04', endDate: '2027-01-06' }, '2026-12-20')).toEqual({
      date: '2027-01-04',
      kind: 'departure',
    })
    expect(cardForecastTarget({ startDate: '2027-01-05', endDate: '2027-01-06' }, '2026-12-20')).toBeNull()
  })
})
