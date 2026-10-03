// What every map of the app shares (UI_GUIDE 9): the background and the standard views.

/**
 * Where the background of the map comes from (design.md 3.2). The standard OpenStreetMap tiles need no API
 * key, so the map works on a fresh clone; their terms ask for the credit line and forbid bulk downloads. The
 * browser fetches them directly, not through the backend. The colours are toned down so the markers stand out.
 */
export const MAP_TILES = {
  url: 'https://tile.openstreetmap.org/{z}/{x}/{y}.png',
  attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
  maxZoom: 19,
  className: 'grayscale-[0.85] brightness-105 contrast-90',
}

export const LEAFLET_CREDIT = '<a href="https://leafletjs.com">Leaflet</a>'

/** Viet Nam as a whole: what a map shows when nothing says where to look */
export const VIETNAM_VIEW: { center: [number, number]; zoom: number } = { center: [16.2, 107.8], zoom: 5 }

/** A city and its surroundings */
export const CITY_ZOOM = 12

/** Street level: close enough to tell one building from the next */
export const STREET_ZOOM = 15
