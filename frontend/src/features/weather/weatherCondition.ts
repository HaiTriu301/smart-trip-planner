import { Cloud, CloudFog, CloudLightning, CloudRain, CloudSnow, CloudSun, Sun, type LucideIcon } from 'lucide-react'
import type { WeatherCondition } from '../../types/weather'

interface ConditionLook {
  Icon: LucideIcon
  /** Said by screen readers, and written out where there is room for words */
  label: string
  /** Colour of the icon: warm for sun, blue for anything falling from the sky, grey for clouds (UI_GUIDE 9) */
  color: string
}

/** How each weather condition is drawn. One table for every place that shows the weather. */
export const WEATHER_CONDITIONS: Record<WeatherCondition, ConditionLook> = {
  CLEAR: { Icon: Sun, label: 'Trời nắng', color: 'text-sun' },
  PARTLY_CLOUDY: { Icon: CloudSun, label: 'Nắng nhẹ', color: 'text-sun' },
  CLOUDY: { Icon: Cloud, label: 'Có mây', color: 'text-gray-500' },
  FOG: { Icon: CloudFog, label: 'Sương mù', color: 'text-gray-500' },
  RAIN: { Icon: CloudRain, label: 'Có mưa', color: 'text-info' },
  THUNDERSTORM: { Icon: CloudLightning, label: 'Mưa dông', color: 'text-info' },
  SNOW: { Icon: CloudSnow, label: 'Có tuyết', color: 'text-info' },
}

/** 31.6 → "32°". The server sends one decimal; a whole degree is enough to plan a day. */
export function formatDegrees(celsius: number): string {
  // Math.round(-0.4) is -0, which would print as "-0°"
  return `${Math.round(celsius) || 0}°`
}
