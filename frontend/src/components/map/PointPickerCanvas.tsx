import { useEffect, useMemo, useState } from 'react'
import { createPortal } from 'react-dom'
import L from 'leaflet'
import { AttributionControl, MapContainer, Marker, TileLayer, ZoomControl, useMap, useMapEvents } from 'react-leaflet'
import { MapPin } from 'lucide-react'
import 'leaflet/dist/leaflet.css'
import { CITY_ZOOM, LEAFLET_CREDIT, MAP_TILES, STREET_ZOOM, VIETNAM_VIEW } from '../../lib/mapTiles'
import type { Coordinates } from '../../types/place'

// Same teardrop as the markers of a day, in the brand colour: a point, not an activity of some type
const PIN_SIZE: L.PointTuple = [28, 34]
const PIN_ANCHOR: L.PointTuple = [14, 34]

/** The server stores seven decimals (about 1 cm); a click gives many more */
const round7 = (degrees: number) => Number(degrees.toFixed(7))

interface PointPickerCanvasProps {
  /** The chosen point; null: none yet */
  value: Coordinates | null
  /** Where the map opens while no point is chosen; null: the whole country */
  around: Coordinates | null
  onPick: (point: Coordinates) => void
}

/** The Leaflet part of PointPicker, in its own file so it can be loaded on demand (default export for lazy()). */
export default function PointPickerCanvas({ value, around, onPick }: PointPickerCanvasProps) {
  // Where the map opens; later changes of the point move the view only when it would fall outside (KeepInView)
  const [initialView] = useState(() => {
    if (value) return { center: [value.lat, value.lng] as L.LatLngTuple, zoom: STREET_ZOOM }
    if (around) return { center: [around.lat, around.lng] as L.LatLngTuple, zoom: CITY_ZOOM }
    return VIETNAM_VIEW
  })

  return (
    <MapContainer
      center={initialView.center}
      zoom={initialView.zoom}
      zoomControl={false}
      attributionControl={false}
      className="h-full w-full font-sans!"
    >
      <TileLayer
        url={MAP_TILES.url}
        attribution={MAP_TILES.attribution}
        maxZoom={MAP_TILES.maxZoom}
        className={MAP_TILES.className}
      />
      <AttributionControl position="bottomright" prefix={LEAFLET_CREDIT} />
      <ZoomControl position="bottomright" zoomInTitle="Phóng to" zoomOutTitle="Thu nhỏ" />
      <ClickToPick onPick={onPick} />
      {value && (
        <>
          <KeepInView point={value} />
          <Pin point={value} />
        </>
      )}
    </MapContainer>
  )
}

function ClickToPick({ onPick }: Pick<PointPickerCanvasProps, 'onPick'>) {
  useMapEvents({
    click: (event) => {
      // wrap(): after dragging the map around the globe, the longitude is brought back into -180..180
      const point = event.latlng.wrap()
      onPick({ lat: round7(point.lat), lng: round7(point.lng) })
    },
  })
  return null
}

/**
 * A point chosen by clicking is already in view. One that arrives from elsewhere (a search result) may not be:
 * then the map goes there, at least at city level.
 */
function KeepInView({ point }: { point: Coordinates }) {
  const map = useMap()
  const { lat, lng } = point
  useEffect(() => {
    if (!map.getBounds().contains([lat, lng])) map.setView([lat, lng], Math.max(map.getZoom(), CITY_ZOOM))
  }, [map, lat, lng])
  return null
}

function Pin({ point }: { point: Coordinates }) {
  const [element] = useState(() => document.createElement('div'))
  const icon = useMemo(
    () => L.divIcon({ html: element, className: '', iconSize: PIN_SIZE, iconAnchor: PIN_ANCHOR }),
    [element],
  )
  return (
    // The pin only shows where the point is: a click on it goes to the map and moves the point
    <Marker position={[point.lat, point.lng]} icon={icon} interactive={false} keyboard={false}>
      {createPortal(
        <div className="flex size-7 -rotate-45 items-center justify-center rounded-[50%_50%_50%_0] border-2 border-white bg-jade shadow-md">
          <MapPin aria-hidden className="size-3.5 rotate-45 text-white" strokeWidth={2.25} />
        </div>,
        element,
      )}
    </Marker>
  )
}
