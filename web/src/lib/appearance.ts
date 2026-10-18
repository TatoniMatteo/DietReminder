export interface Appearance {
  theme: 'light' | 'dark' | 'system'
  compact: boolean
  colorMode: 'brand' | 'custom' | 'system'
  seed: string
  reducedMotion: boolean
  systemAccentAvailable: boolean
}
declare global {
  interface Window {
    DietReminderAppearance: {
      get: () => Appearance
      update: (patch: Partial<Omit<Appearance, 'systemAccentAvailable'>>) => boolean
    }
  }
}
export const appearance = window.DietReminderAppearance
