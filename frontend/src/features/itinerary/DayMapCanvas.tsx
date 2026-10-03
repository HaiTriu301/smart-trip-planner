import { useEffect, useMemo, useState } from 'react'
import { createPortal } from 'react-dom'
import L from 'leaflet'
import { AttributionControl, MapContainer, Marker, Polyline, TileLayer, ZoomControl, useMap } from 'react-leaflet'
import 'leaflet/dist/leaflet.css'
import { useMapLinkStore } from '../../stores/mapLinkStore'
import type { Coordinates } from '../../types/place'
import { ACTIVITY_ROUTE } from './activityType'
import type { DayStop } from './DayMap'

/**
 * Where the background of the map comes from (design.md 3.2). The standard OpenStreetMap tiles need no API
 * key, so the map works on a fresh clone; their terms ask for the credit line and forbid bulk downloads. The
 * browser fetches them directly, not through the backend. The colours are toned down so the markers stand out.
 */
const TILES = {
  url: 'https://tile.openstreetmap.org/{z}/{x}/{y}.png',
  attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
  maxZoom: 19,
  className: 'grayscale-[0.85] brightness-105 contrast-90',
}

const LEAFLET_CREDIT = '<a href="https://leafletjs.com">Leaflet</a>'

/** Viet Nam as a whole: what the map shows when neither the day nor the trip says where to look */
const DEFAULT_CENTER: L.LatLngTuple = [16.2, 107.8]
const DEFAULT_ZOOM = 5
/** A city and its surroundings: the view of a day without places, centred on the trip's destination */
const DESTINATION_ZOOM = 12
/** Leaflet draws markers further south on top; this lifts the highlighted one above any neighbour */
const HIGHLIGHT_Z_OFFSET = 1000

/** Street level; also the closest the map goes by itself when a day has a single place */
const MAX_FIT_ZOOM = 15

// Teardrop of 28px whose tip, 34px down, sits on the place
const MARKER_SIZE: L.PointTuple = [28, 34]
const MARKER_ANCHOR: L.PointTuple = [14, 34]

/**
 * The line that joins the places of the day in order (UI_GUIDE 9). Dashed on purpose: it is a straight line
 * from one place to the next, not the road, and a solid line would read as a street of the map. The colour
 * comes from the class (the jade token); Leaflet's own stroke colour is only an attribute, which CSS overrides.
 */
const ROUTE_LINE: L.PolylineOptions = {
  className: 'stroke-jade',
  weight: 2,
  opacity: 0.7,
  dashArray: '6 6',
  // Nothing to click: the pointer goes through to the map
  interactive: false,
}

interface DayMapCanvasProps {
  stops: DayStop[]
  /** Where to look when the day has no place; null when the trip has no position */
  destination: Coordinates | null
}

/** The Leaflet part of DayMap, in its own file so it can be loaded on demand (default export for lazy()). */
export default function DayMapCanvas({ stops, destination }: DayMapCanvasProps) {
  return (
    <MapContainer
      center={DEFAULT_CENTER}
      zoom={DEFAULT_ZOOM}
      zoomControl={false}
      attributionControl={false}
      className="h-full w-full font-sans!"
    >
      <TileLayer
        url={TILES.url}
        attribution={TILES.attribution}
        maxZoom={TILES.maxZoom}
        className={TILES.className}
      />
      <AttributionControl position="bottomright" prefix={LEAFLET_CREDIT} />
      <ZoomControl position="bottomright" zoomInTitle="Phóng to" zoomOutTitle="Thu nhỏ" />
      <FitToStops stops={stops} destination={destination} />
      {stops.length > 1 && (
        <Polyline positions={stops.map((stop) => [stop.lat, stop.lng])} pathOptions={ROUTE_LINE} />
      )}
      {stops.map((stop) => (
        <StopMarker key={stop.activityId} stop={stop} />
      ))}
    </MapContainer>
  )
}

/**
 * Frames all the places of the day; a day without places shows the destination of the trip. The view is
 * recomputed when the set of places (or the destination) changes, not when the order of the places does: dragging a card must not make the map jump, and the user's own zoom and pan survive a reorder.
 */
function FitToStops({ stops, destination }: DayMapCanvasProps) {
  const map = useMap()
  const destinationLat = destination?.lat
  const destinationLng = destination?.lng
  const placesKey = stops
    .map((stop) => `${stop.lat},${stop.lng}`)
    .sort()
    .join('|')

  useEffect(() => {
    if (placesKey === '') {
      // No place to frame: the destination of the trip if it has a position, the whole country otherwise
      if (destinationLat !== undefined && destinationLng !== undefined) {
        map.setView([destinationLat, destinationLng], DESTINATION_ZOOM)
      } else {
        map.setView(DEFAULT_CENTER, DEFAULT_ZOOM)
      }
      return
    }
    const points = placesKey.split('|').map((pair) => pair.split(',').map(Number) as L.LatLngTuple)
    map.fitBounds(L.latLngBounds(points), { padding: [48, 48], maxZoom: MAX_FIT_ZOOM })
  }, [map, placesKey, destinationLat, destinationLng])

  return null
}

/**
 * A marker drawn by the app itself (UI_GUIDE 9), not the image of the map library: teardrop in the route
 * colour of the activity type, its icon in white, the order number in a small badge. Leaflet is given an empty
 * element as the icon and React renders into it, so the marker uses the same tokens and icons as the cards.
 * The marker of the activity whose card is hovered or focused is 1.15 times larger, has a soft ring and comes
 * to the front.
 */
function StopMarker({ stop }: { stop: DayStop }) {
  const [element] = useState(() => document.createElement('div'))
  const icon = useMemo(
    () => L.divIcon({ html: element, className: '', iconSize: MARKER_SIZE, iconAnchor: MARKER_ANCHOR }),
    [element],
  )
  const route = ACTIVITY_ROUTE[stop.type]
  const label = `${stop.number}. ${stop.title}`
  // A boolean, so a marker redraws only when it gains or loses the highlight, not on every card the pointer crosses
  const highlighted = useMapLinkStore((state) => state.highlightedActivityId === stop.activityId)

  return (
    <Marker
      position={[stop.lat, stop.lng]}
      icon={icon}
      title={label}
      alt={label}
      zIndexOffset={highlighted ? HIGHLIGHT_Z_OFFSET : 0}
    >
      {createPortal(
        // Grows from its tip, so the point of the teardrop stays on the place
        <div
          className={`relative size-7 origin-[50%_121%] transition-transform duration-150 ${highlighted ? 'scale-115' : ''}`}
        >
          {/* A square with three round corners, turned so the sharp one points down */}
          <div
            className={`flex size-7 -rotate-45 items-center justify-center rounded-[50%_50%_50%_0] border-2 border-white shadow-md transition-shadow duration-150 ${route.marker} ${
              highlighted ? 'ring-4 ring-jade/35' : ''
            }`}
          >
            <route.Icon aria-hidden className="size-3.5 rotate-45 text-white" strokeWidth={2.25} />
          </div>
          <span
            className={`tabular absolute -top-1.5 -right-1.5 flex size-4 items-center justify-center rounded-full border bg-white text-[10px] leading-none font-bold ${route.dot} ${route.text}`}
          >
            {stop.number}
          </span>
        </div>,
        element,
      )}
    </Marker>
  )
}
