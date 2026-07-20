import { useTranslation } from '../../lib/i18n'

export default function PrivacyPolicy() {
  const { t } = useTranslation()
  const sections = [1, 2, 3, 4, 5].map((n) => ({
    title: t(`legal.privacy.s${n}Title`),
    body: t(`legal.privacy.s${n}Body`),
  }))

  return (
    <main className="mx-auto max-w-3xl px-6 py-12 sm:py-16">
      <h1 className="font-display text-3xl text-ink sm:text-4xl">{t('legal.privacy.title')}</h1>
      <p className="mt-3 text-ink-soft">{t('legal.privacy.intro')}</p>

      <div className="mt-10 space-y-8">
        {sections.map((section) => (
          <section key={section.title}>
            <h2 className="text-lg text-ink">{section.title}</h2>
            <p className="mt-2 text-sm text-ink-soft">{section.body}</p>
          </section>
        ))}
      </div>
    </main>
  )
}
