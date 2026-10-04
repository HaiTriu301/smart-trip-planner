import { useQuery } from '@tanstack/react-query'
import { getTripWeather } from '../../api/weather'

/**
 * The forecast of every day of a trip. One key for every place that shows it, so the strip under the map, the
 * line in the day heading and the trip card share a single request. The key is not under ['trip', id]:
 * editing an activity must not ask for the forecast again.
 */
export function useTripWeather(tripId: number) {
  return useQuery({
    queryKey: ['weather', tripId],
    queryFn: () => getTripWeather(tripId),
  })
}
