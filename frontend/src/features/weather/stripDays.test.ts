import { describe, expect, it } from 'vitest'
import type { TripWeatherDay } from '../../types/weather'
import { daysWithForecast, noForecastReason } from './stripDays'

const FORECAST = { condition: 'CLOUDY', tempMin: 24.6, tempMax: 31.2, precipitationProbability: 20 } as const

const day = (dayId: number, date: string, hasForecast: boolean): TripWeatherDay => ({
  dayId,
  date,
  forecast: hasForecast ? FORECAST : null,
})

describe('daysWithForecast', () => {
  it('leaves out the days that are over and keeps the others in order', () => {
    // Today is 13 October: the server sends no forecast for the 12th
    const days = [day(1, '2026-10-12', false), day(2, '2026-10-13', true), day(3, '2026-10-14', true)]

    expect(daysWithForecast(days).map((d) => d.dayId)).toEqual([2, 3])
  })

  it('leaves out the days further ahead than the forecast reaches', () => {
    const days = [day(1, '2026-10-27', true), day(2, '2026-10-28', true), day(3, '2026-10-29', false)]

    expect(daysWithForecast(days).map((d) => d.dayId)).toEqual([1, 2])
  })

  it('keeps every day of a trip that lies inside the forecast window', () => {
    const days = [day(1, '2026-10-13', true), day(2, '2026-10-14', true)]

    expect(daysWithForecast(days)).toEqual(days)
  })

  it('gives nothing when no day has a forecast', () => {
    expect(daysWithForecast([day(1, '2026-10-01', false), day(2, '2026-10-02', false)])).toEqual([])
  })
})

describe('noForecastReason', () => {
  const today = '2026-10-13'

  it('says the trip is over when its last day was yesterday', () => {
    expect(noForecastReason([{ date: '2026-10-11' }, { date: '2026-10-12' }], today)).toBe('over')
  })

  it('says the trip is still ahead when it starts after the forecast window', () => {
    expect(noForecastReason([{ date: '2026-11-20' }, { date: '2026-11-21' }], today)).toBe('ahead')
  })

  it('does not call a trip over while one of its days is today or later', () => {
    expect(noForecastReason([{ date: '2026-10-12' }, { date: '2026-10-13' }], today)).toBe('ahead')
  })
})
