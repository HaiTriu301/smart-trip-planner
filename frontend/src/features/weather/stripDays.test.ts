import { describe, expect, it } from 'vitest'
import type { TripWeatherDay } from '../../types/weather'
import { daysWithForecast, noForecastReason, pageCount, pageOf, stripLayout } from './stripDays'

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

describe('stripLayout', () => {
  it('shows every tile without buttons when they all fit', () => {
    // The 380px column: 356px inside the padding of the card, room for four tiles and three gaps
    expect(stripLayout(356, 4)).toEqual({ size: 4, paged: false })
    expect(stripLayout(356, 2)).toEqual({ size: 2, paged: false })
  })

  it('turns to pages when one tile too many is asked for, and a page holds what the row holds', () => {
    expect(stripLayout(356, 5)).toEqual({ size: 4, paged: true })
    expect(stripLayout(356, 16)).toEqual({ size: 4, paged: true })
  })

  it('fits three tiles in the narrower column', () => {
    // The 340px column: 316px inside the padding of the card
    expect(stripLayout(316, 3)).toEqual({ size: 3, paged: false })
    expect(stripLayout(316, 4)).toEqual({ size: 3, paged: true })
  })

  it('needs room for the gaps as well as the tiles', () => {
    // Four tiles of 82px take 328px, with three gaps 352px: one pixel less and only three fit
    expect(stripLayout(352, 4)).toEqual({ size: 4, paged: false })
    expect(stripLayout(351, 4)).toEqual({ size: 3, paged: true })
  })

  it('fits more tiles on a wide row', () => {
    expect(stripLayout(876, 16)).toEqual({ size: 9, paged: true })
  })

  it('always keeps one tile, however narrow the row is', () => {
    expect(stripLayout(120, 5)).toEqual({ size: 1, paged: true })
    expect(stripLayout(0, 5)).toEqual({ size: 1, paged: true })
  })
})

describe('pageCount and pageOf', () => {
  it.each([
    [7, 3, 3],
    [6, 3, 2],
    [16, 4, 4],
    [1, 3, 1],
    [0, 3, 1],
  ])('%i cells, %i a page → %i pages', (total, size, expected) => {
    expect(pageCount(total, size)).toBe(expected)
  })

  it.each([
    [0, 3, 0],
    [2, 3, 0],
    [3, 3, 1],
    [6, 3, 2],
    [15, 4, 3],
  ])('cell %i, %i a page → page %i', (position, size, expected) => {
    expect(pageOf(position, size)).toBe(expected)
  })
})
