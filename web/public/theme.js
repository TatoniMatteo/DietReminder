;(() => {
  const root = document.documentElement
  root.lang = readLanguage()
  function readLanguage() {
    try {
      const saved = localStorage.getItem('dr-language')
      if (saved === 'it' || saved === 'en') return saved
    } catch {}
    return navigator.language?.toLowerCase().startsWith('en') ? 'en' : 'it'
  }
  const media = matchMedia('(prefers-color-scheme: dark)')
  const keys = ['dr-theme', 'dr-compact', 'dr-color-mode', 'dr-seed', 'dr-motion']
  const read = (key, fallback) => {
    try {
      return localStorage.getItem(key) ?? fallback
    } catch {
      return fallback
    }
  }
  function restore() {
    const theme = read('dr-theme', 'dark')
    const colorMode = read('dr-color-mode', 'brand')
    const seed = read('dr-seed', '#246a42')
    return {
      theme: ['dark', 'light', 'system'].includes(theme) ? theme : 'dark',
      compact: read('dr-compact', 'false') === 'true',
      colorMode: ['brand', 'custom', 'system'].includes(colorMode) ? colorMode : 'brand',
      seed: /^#[0-9a-f]{6}$/i.test(seed) ? seed : '#246a42',
      reducedMotion: read('dr-motion', 'auto') === 'reduced',
    }
  }
  function systemAccent() {
    if (!CSS.supports('color', 'AccentColor')) return null
    const probe = document.createElement('span')
    probe.style.cssText =
      'color:AccentColor;position:fixed;visibility:hidden;pointer-events:none;accent-color:auto'
    root.appendChild(probe)
    const color = getComputedStyle(probe).color
    probe.remove()
    const match = /^rgb\(\s*(\d+)[, ]+\s*(\d+)[, ]+\s*(\d+)\s*\)$/.exec(color)
    return match ? match.slice(1, 4).map(Number) : null
  }
  const toRgb = (hex) => [1, 3, 5].map((start) => parseInt(hex.slice(start, start + 2), 16))
  const mix = (a, b, ratio) =>
    a.map((value, index) => Math.round(value * (1 - ratio) + b[index] * ratio))
  const luminance = (rgb) =>
    rgb
      .map((v) => {
        const c = v / 255
        return c <= 0.04045 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4
      })
      .reduce((sum, v, index) => sum + v * [0.2126, 0.7152, 0.0722][index], 0)
  const contrast = (a, b) =>
    (Math.max(luminance(a), luminance(b)) + 0.05) / (Math.min(luminance(a), luminance(b)) + 0.05)
  const css = (rgb) => `rgb(${rgb.join(', ')})`
  const properties = ['--primary', '--on-primary', '--primary-container', '--secondary']
  let preferences = restore()
  function apply() {
    const theme =
      preferences.theme === 'system' ? (media.matches ? 'dark' : 'light') : preferences.theme
    root.dataset.theme = theme
    root.dataset.themePreference = preferences.theme
    root.dataset.compact = String(preferences.compact)
    root.dataset.motion = preferences.reducedMotion ? 'reduced' : 'auto'
    const accent = systemAccent()
    root.dataset.systemAccentAvailable = String(!!accent)
    root.dataset.colorMode = preferences.colorMode
    properties.forEach((property) => root.style.removeProperty(property))
    const seed =
      preferences.colorMode === 'system'
        ? accent
        : preferences.colorMode === 'custom'
          ? toRgb(preferences.seed)
          : null
    if (seed) {
      const dark = theme === 'dark'
      const background = dark ? [24, 26, 33] : [255, 255, 255]
      const text = dark ? [16, 17, 22] : [255, 255, 255]
      const target = dark ? [255, 255, 255] : [0, 0, 0]
      let primary = mix(seed, target, dark ? 0.55 : 0.16)
      const container = mix(seed, background, 0.84)
      for (
        let i = 0;
        i < 50 &&
        (contrast(primary, background) < 4.5 ||
          contrast(primary, text) < 4.5 ||
          contrast(primary, container) < 4.5);
        i++
      )
        primary = mix(primary, target, 0.08)
      root.style.setProperty('--primary', css(primary))
      root.style.setProperty('--on-primary', css(text))
      root.style.setProperty('--primary-container', css(container))
      root.style.setProperty('--secondary', css(primary))
    }
    document
      .querySelector('meta[name="theme-color"]')
      ?.setAttribute('content', theme === 'dark' ? '#101116' : '#f7f8fc')
    window.dispatchEvent(new Event('appearance-changed'))
  }
  window.DietReminderAppearance = {
    get: () => ({ ...preferences, systemAccentAvailable: !!systemAccent() }),
    update: (patch) => {
      preferences = { ...preferences, ...patch }
      let persisted = true
      try {
        localStorage.setItem('dr-theme', preferences.theme)
        localStorage.setItem('dr-compact', String(preferences.compact))
        localStorage.setItem('dr-color-mode', preferences.colorMode)
        localStorage.setItem('dr-seed', preferences.seed)
        localStorage.setItem('dr-motion', preferences.reducedMotion ? 'reduced' : 'auto')
      } catch {
        persisted = false
      }
      apply()
      return persisted
    },
  }
  media.addEventListener('change', apply)
  window.addEventListener('storage', (event) => {
    if (event.key === null || keys.includes(event.key)) {
      preferences = restore()
      apply()
    }
  })
  apply()
})()
