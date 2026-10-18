import { createContext, useContext } from 'react'

/**
 * Where the lists and calendars that pop out of a field are drawn (Task 4.9 decision 8). Radix draws them at the
 * end of <body> by default, but a <dialog> opened with showModal() sits in the browser's top layer, above
 * everything in <body>: a list drawn there would be hidden under the dialog and could not be clicked. Modal
 * provides its own <dialog> element here; outside any dialog the value is null and the list goes to <body>.
 */
export const OverlayContainerContext = createContext<HTMLElement | null>(null)

export function useOverlayContainer(): HTMLElement | null {
  return useContext(OverlayContainerContext)
}
