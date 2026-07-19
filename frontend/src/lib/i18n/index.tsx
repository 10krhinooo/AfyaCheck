import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import en from './locales/en.json'
import sw from './locales/sw.json'

export const SUPPORTED_LANGUAGES = ['en', 'sw'] as const
export type SupportedLanguage = (typeof SUPPORTED_LANGUAGES)[number]

const STORAGE_KEY = 'afyacheck-lang'

// Nested string dictionaries, e.g. { nav: { home: "Home" } } — dot-path lookup below.
type Dictionary = { [key: string]: string | Dictionary }
const resources: Record<SupportedLanguage, Dictionary> = { en, sw }

function lookup(dict: Dictionary, path: string): string | undefined {
  const value = path.split('.').reduce<Dictionary | string | undefined>((node, segment) => {
    if (typeof node !== 'object' || node === null) return undefined
    return node[segment]
  }, dict)
  return typeof value === 'string' ? value : undefined
}

interface I18nState {
  language: SupportedLanguage
  setLanguage: (lang: SupportedLanguage) => void
  t: (key: string) => string
}

const I18nContext = createContext<I18nState>({
  language: 'en',
  setLanguage: () => {},
  t: (key) => lookup(resources.en, key) ?? key,
})

export function I18nProvider({ children }: { children: ReactNode }) {
  const [language, setLanguageState] = useState<SupportedLanguage>('en')

  // Client-only, like AuthContext's getUserManager() call — never runs during
  // vite-react-ssg's build-time prerender, which has no window/localStorage.
  useEffect(() => {
    const stored = window.localStorage.getItem(STORAGE_KEY)
    if (stored && (SUPPORTED_LANGUAGES as readonly string[]).includes(stored)) {
      setLanguageState(stored as SupportedLanguage)
    }
  }, [])

  useEffect(() => {
    document.documentElement.lang = language
  }, [language])

  const setLanguage = (lang: SupportedLanguage) => {
    window.localStorage.setItem(STORAGE_KEY, lang)
    setLanguageState(lang)
  }

  const value = useMemo<I18nState>(
    () => ({
      language,
      setLanguage,
      t: (key: string) => lookup(resources[language], key) ?? lookup(resources.en, key) ?? key,
    }),
    [language],
  )

  return <I18nContext.Provider value={value}>{children}</I18nContext.Provider>
}

export function useTranslation() {
  return useContext(I18nContext)
}
