import { t, localizedArray } from '../../lib/i18n'
import { useEffect, useState } from 'react'
import { Monitor, Moon, Sun, Rows3, Palette, Sparkles, Accessibility } from 'lucide-react'
import { appearance, type Appearance } from '../../lib/appearance'
import { LanguageSelector } from '../../components/LanguageSelector'
const presets = localizedArray([
  ['#246a42', 'Verde'],
  ['#6750a4', 'Lavanda'],
  ['#2563eb', 'Blu'],
  ['#be5143', 'Corallo'],
  ['#a66b00', 'Ambra'],
])
export function AppearanceSettings() {
  const [settings, setSettings] = useState(appearance.get)
  const [temporary, setTemporary] = useState(false)
  useEffect(() => {
    const changed = () => setSettings(appearance.get())
    window.addEventListener('appearance-changed', changed)
    return () => window.removeEventListener('appearance-changed', changed)
  }, [])
  function update(patch: Partial<Appearance>) {
    setTemporary(!appearance.update(patch))
  }
  return (
    <section className="settings-section" id="settings-appearance">
      <h2>
        <Palette size={21} />
        {t('A tuo agio')}
      </h2>
      <p className="muted">{t('Lo stesso stile, con il ritmo che preferisci.')}</p>
      <LanguageSelector />
      <span className="preference-label">{t('Aspetto')}</span>
      <div className="segmented" role="group" aria-label={t('Tema dell’interfaccia')}>
        {(
          [
            ['system', t('Sistema'), Monitor],
            ['light', t('Chiaro'), Sun],
            ['dark', t('Scuro'), Moon],
          ] as const
        ).map(([value, label, Icon]) => (
          <button
            key={value}
            className={settings.theme === value ? 'selected' : ''}
            aria-pressed={settings.theme === value}
            onClick={() => update({ theme: value })}
          >
            <Icon size={17} /> {label}
          </button>
        ))}
      </div>
      <div className="preference-row">
        <Sparkles size={22} />
        <span>
          <strong>{t('Colori dinamici del sistema')}</strong>
          <small>
            {settings.systemAccentAvailable
              ? t('Usa il colore che il browser rende disponibile.')
              : t('Questo browser non espone un colore di sistema.')}
          </small>
        </span>
        <input
          type="checkbox"
          role="switch"
          aria-label={t('Colori dinamici del sistema')}
          disabled={!settings.systemAccentAvailable}
          checked={settings.colorMode === 'system' && settings.systemAccentAvailable}
          onChange={(e) =>
            update({
              colorMode: e.target.checked
                ? 'system'
                : settings.seed === '#246a42'
                  ? 'brand'
                  : 'custom',
            })
          }
        />
      </div>
      <label className="accent-picker">
        {t('Colore dell’app')}
        <input
          type="color"
          aria-label={t('Colore dell’app')}
          value={settings.seed}
          disabled={settings.colorMode === 'system' && settings.systemAccentAvailable}
          onChange={(e) => update({ seed: e.target.value, colorMode: 'custom' })}
        />
      </label>
      <div className="color-presets" role="group" aria-label={t('Colori predefiniti')}>
        {presets.map(([seed, label]) => (
          <button
            key={seed}
            type="button"
            className="color-preset"
            aria-label={t('Colore {0}', { '0': label })}
            aria-pressed={settings.colorMode === 'custom' && settings.seed === seed}
            disabled={settings.colorMode === 'system' && settings.systemAccentAvailable}
            onClick={() => update({ seed, colorMode: 'custom' })}
            style={{ background: seed }}
          />
        ))}
      </div>
      <button
        className="text-button"
        onClick={() => update({ colorMode: 'brand', seed: '#246a42' })}
      >
        {t('Ripristina colori DietReminder')}
      </button>
      <p className="muted small">
        {t(
          'La palette si adatta al tema chiaro o scuro. I colori dello sfondo Android non sono sempre disponibili nel browser.',
        )}
      </p>
      <div className="preference-row">
        <Rows3 size={22} />
        <span>
          <strong>{t('Più compatto')}</strong>
          <small>{t('Meno spazio, la stessa chiarezza.')}</small>
        </span>
        <input
          type="checkbox"
          role="switch"
          aria-label={t('Interfaccia compatta')}
          checked={settings.compact}
          onChange={(e) => update({ compact: e.target.checked })}
        />
      </div>
      <div className="preference-row">
        <Accessibility size={22} />
        <span>
          <strong>{t('Riduci le animazioni')}</strong>
          <small>
            {t('Le preferenze di accessibilità del sistema vengono sempre rispettate.')}
          </small>
        </span>
        <input
          type="checkbox"
          role="switch"
          aria-label={t('Riduci le animazioni')}
          checked={settings.reducedMotion}
          onChange={(e) => update({ reducedMotion: e.target.checked })}
        />
      </div>
      <p className="muted small">
        {temporary
          ? t('Il browser non consente il salvataggio: le preferenze valgono per questa sessione.')
          : t('Queste preferenze restano in questo browser.')}
      </p>
    </section>
  )
}
