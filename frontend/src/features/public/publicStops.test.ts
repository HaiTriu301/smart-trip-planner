import { describe, expect, it } from 'vitest'
import type { PublicActivity, PublicDay } from '../../types/publicTrip'
import { publicStops } from './publicStops'

function activity(title: string, place: { lat: number; lng: number } | null): PublicActivity {
  return {
    title,
    type: 'SIGHTSEEING',
    startTime: null,
    endTime: null,
    note: null,
    place: place && { name: title, address: null, ...place },
  }
}

function day(dayIndex: number, activities: PublicActivity[]): PublicDay {
  return { dayIndex, date: `2026-10-${String(10 + dayIndex).padStart(2, '0')}`, title: null, activities }
}

describe('publicStops', () => {
  it('lists the places of every day in order, each name prefixed with its day', () => {
    const stops = publicStops([
      day(1, [activity('Chùa Linh Ứng', { lat: 16.1, lng: 108.27 }), activity('Cầu Rồng', { lat: 16.06, lng: 108.23 })]),
      day(2, [activity('Bà Nà Hills', { lat: 15.99, lng: 107.99 })]),
    ])
    expect(stops.map((stop) => stop.title)).toEqual([
      'Ngày 1 · Chùa Linh Ứng',
      'Ngày 1 · Cầu Rồng',
      'Ngày 2 · Bà Nà Hills',
    ])
    expect(stops[2]).toMatchObject({ type: 'SIGHTSEEING', lat: 15.99, lng: 107.99 })
  })

  it('leaves out activities without a place and days without any, and keeps ids unique across days', () => {
    const stops = publicStops([
      day(1, [activity('Ăn sáng', null), activity('Chợ Hàn', { lat: 16.07, lng: 108.22 })]),
      day(2, []),
      day(3, [activity('Ngủ', null), activity('Hội An', { lat: 15.88, lng: 108.33 })]),
    ])
    expect(stops.map((stop) => stop.title)).toEqual(['Ngày 1 · Chợ Hàn', 'Ngày 3 · Hội An'])
    expect(stops.map((stop) => stop.activityId)).toEqual([1, 2])
  })

  it('gives an empty list for a trip without any place, so the page shows no map', () => {
    expect(publicStops([day(1, [activity('Tự do', null)]), day(2, [])])).toEqual([])
  })
})
