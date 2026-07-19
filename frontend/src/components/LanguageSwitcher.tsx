import { Languages } from 'lucide-react'
import { SUPPORTED_LANGUAGES, useTranslation } from '../lib/i18n'

const LABELS: Record<string, string> = { en: 'EN', sw: 'SW' }

export function LanguageSwitcher({ className }: { className?: string }) {
  const { t, language, setLanguage } = useTranslation()

  return (
    <div className={`inline-flex items-center gap-1 ${className ?? ''}`}>
      <Languages aria-hidden="true" size={16} className="text-ink-soft" />
      <div role="group" aria-label={t('nav.language')} className="inline-flex overflow-hidden rounded-full border border-teal-100">
        {SUPPORTED_LANGUAGES.map((lang) => (
          <button
            key={lang}
            type="button"
            aria-pressed={language === lang}
            onClick={() => setLanguage(lang)}
            className={`px-2.5 py-1 text-xs font-medium transition-colors ${
              language === lang ? 'bg-teal-600 text-white' : 'text-ink-soft hover:text-ink'
            }`}
          >
            {LABELS[lang]}
          </button>
        ))}
      </div>
    </div>
  )
}
