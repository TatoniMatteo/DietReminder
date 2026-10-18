import { Languages } from 'lucide-react'
import { t, setLanguage, useLanguage, type Language } from '../lib/i18n'
export function LanguageSelector() {
  const language = useLanguage()
  return (
    <label className="language-selector">
      <span>
        <Languages size={18} />
        {t('Lingua')}
      </span>
      <select
        aria-label={t('Lingua')}
        value={language}
        onChange={(event) => setLanguage(event.target.value as Language)}
      >
        <option value="it">Italiano</option>
        <option value="en">English</option>
      </select>
    </label>
  )
}
