import { describe, expect, it } from 'vitest'
import { parseTimeInput, slotIndexFrom, timeSlots } from './timeInput'

describe('parseTimeInput', () => {
  it('reads one or two digits as the hour', () => {
    expect(parseTimeInput('9')).toBe('09:00')
    expect(parseTimeInput('09')).toBe('09:00')
    expect(parseTimeInput('21')).toBe('21:00')
    expect(parseTimeInput('0')).toBe('00:00')
  })

  it('reads three or four digits as hour and minutes', () => {
    expect(parseTimeInput('930')).toBe('09:30')
    expect(parseTimeInput('0930')).toBe('09:30')
    expect(parseTimeInput('2145')).toBe('21:45')
  })

  it('reads an hour with a separator and optional minutes', () => {
    expect(parseTimeInput('9:30')).toBe('09:30')
    expect(parseTimeInput('09:30')).toBe('09:30')
    expect(parseTimeInput('9.30')).toBe('09:30')
    expect(parseTimeInput('9h30')).toBe('09:30')
    expect(parseTimeInput('21h')).toBe('21:00')
    expect(parseTimeInput(' 7 H 05 ')).toBe('07:05')
  })

  it('gives an empty string for an empty field', () => {
    expect(parseTimeInput('')).toBe('')
    expect(parseTimeInput('   ')).toBe('')
  })

  it('refuses what is not a time of day', () => {
    expect(parseTimeInput('24')).toBeNull()
    expect(parseTimeInput('24:00')).toBeNull()
    expect(parseTimeInput('960')).toBeNull()
    expect(parseTimeInput('12:60')).toBeNull()
    expect(parseTimeInput('12345')).toBeNull()
    expect(parseTimeInput('abc')).toBeNull()
    expect(parseTimeInput('9 sáng')).toBeNull()
  })

  it('refuses a single digit of minutes after a separator, which is ambiguous', () => {
    expect(parseTimeInput('9:5')).toBeNull()
  })
})

describe('timeSlots', () => {
  it('lists the day in steps of 15 minutes', () => {
    const slots = timeSlots()
    expect(slots).toHaveLength(96)
    expect(slots.slice(0, 3)).toEqual(['00:00', '00:15', '00:30'])
    expect(slots.at(-1)).toBe('23:45')
  })
})

describe('slotIndexFrom', () => {
  const slots = timeSlots()

  it('finds the slot of a time, or the next one', () => {
    expect(slots[slotIndexFrom(slots, '09:30')]).toBe('09:30')
    expect(slots[slotIndexFrom(slots, '09:20')]).toBe('09:30')
  })

  it('gives -1 after the last slot of the day', () => {
    expect(slotIndexFrom(slots, '23:50')).toBe(-1)
  })
})
