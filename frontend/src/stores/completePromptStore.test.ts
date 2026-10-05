import { beforeEach, describe, expect, it } from 'vitest'
import { useAuthStore } from './authStore'
import { readPostponed, useCompletePromptStore, writePostponed } from './completePromptStore'

const KEY = 'trip-planner.complete-later'

/** localStorage of a browser, as far as the store uses it */
function fakeStorage(initial?: string) {
  const values = new Map<string, string>(initial === undefined ? [] : [[KEY, initial]])
  return {
    values,
    getItem: (key: string) => values.get(key) ?? null,
    setItem: (key: string, value: string) => void values.set(key, value),
    removeItem: (key: string) => void values.delete(key),
  }
}

const brokenStorage = {
  getItem: () => {
    throw new Error('blocked')
  },
  setItem: () => {
    throw new Error('quota exceeded')
  },
  removeItem: () => {
    throw new Error('blocked')
  },
}

describe('readPostponed', () => {
  it('reads the ids that were stored', () => {
    expect(readPostponed(fakeStorage('[3,12]'))).toEqual([3, 12])
  })

  it('gives an empty list when nothing was stored', () => {
    expect(readPostponed(fakeStorage())).toEqual([])
  })

  it.each([
    ['text that is not JSON', 'not json'],
    ['JSON that is not a list', '{"3":true}'],
    ['an empty text', ''],
  ])('gives an empty list for %s', (_name, stored) => {
    expect(readPostponed(fakeStorage(stored))).toEqual([])
  })

  it('keeps only whole numbers of a list someone tampered with', () => {
    expect(readPostponed(fakeStorage('[3,"4",null,5.5,7]'))).toEqual([3, 7])
  })

  it('gives an empty list when the browser has no storage or refuses to read it', () => {
    expect(readPostponed(undefined)).toEqual([])
    expect(readPostponed(brokenStorage)).toEqual([])
  })
})

describe('writePostponed', () => {
  it('stores the ids, and reads them back', () => {
    const storage = fakeStorage()

    writePostponed(storage, [3, 12])

    expect(storage.values.get(KEY)).toBe('[3,12]')
    expect(readPostponed(storage)).toEqual([3, 12])
  })

  it('removes the entry for an empty list', () => {
    const storage = fakeStorage('[3]')

    writePostponed(storage, [])

    expect(storage.values.has(KEY)).toBe(false)
  })

  it('does not fail when the browser refuses the write', () => {
    expect(() => writePostponed(brokenStorage, [3])).not.toThrow()
    expect(() => writePostponed(brokenStorage, [])).not.toThrow()
    expect(() => writePostponed(undefined, [3])).not.toThrow()
  })
})

describe('useCompletePromptStore', () => {
  const signIn = () => useAuthStore.getState().setSession({ accessToken: 'test', user: {} } as never)

  beforeEach(() => {
    useAuthStore.setState({ status: 'unknown', accessToken: null, user: null })
    useCompletePromptStore.setState({ postponedTripIds: [] })
  })

  it('remembers each postponed trip once', () => {
    const { postpone } = useCompletePromptStore.getState()

    postpone(3)
    postpone(12)
    postpone(3)

    expect(useCompletePromptStore.getState().postponedTripIds).toEqual([3, 12])
  })

  it('forgets everything when the user signs out', () => {
    signIn()
    useCompletePromptStore.getState().postpone(3)

    useAuthStore.getState().clearSession()

    expect(useCompletePromptStore.getState().postponedTripIds).toEqual([])
  })

  it('forgets everything when a visit finds the earlier session expired', () => {
    // Restored from the browser at start-up, then the first refresh is rejected: unknown → anonymous
    useCompletePromptStore.setState({ postponedTripIds: [3] })

    useAuthStore.getState().clearSession()

    expect(useCompletePromptStore.getState().postponedTripIds).toEqual([])
  })

  it('keeps the list when a reload restores the session', () => {
    // unknown → authenticated: the same sign-in goes on
    useCompletePromptStore.setState({ postponedTripIds: [3] })

    signIn()

    expect(useCompletePromptStore.getState().postponedTripIds).toEqual([3])
  })
})
