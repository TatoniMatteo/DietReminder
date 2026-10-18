import { Select } from './Select'
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
      <Select
        aria-label={t('Lingua')}
        value={language}
        onValueChange={(value) => setLanguage(value as Language)}
      >
        <option value="it">Italiano</option>
        <option value="en">English</option>
      </Select>
    </label>
  )
}
