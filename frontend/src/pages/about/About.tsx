import { Lock, MapPin, ShieldCheck, Sparkles } from 'lucide-react'
import { BrandMotif } from '../../components/BrandMotif'
import { Card } from '../../components/Card'
import { LinkButton } from '../../components/Button'
import { Reveal } from '../../components/Reveal'
import { useTranslation } from '../../lib/i18n'

const sectionIcons = [Sparkles, ShieldCheck, MapPin, Lock]

export default function About() {
  const { t } = useTranslation()
  const sections = [1, 2, 3, 4].map((n) => ({
    icon: sectionIcons[n - 1],
    title: t(`about.section${n}Title`),
    body: t(`about.section${n}Body`),
  }))

  return (
    <main className="mx-auto max-w-3xl px-6 py-12 sm:py-16">
      <Reveal className="text-center">
        <BrandMotif size="md" className="mb-6" />
        <h1 className="font-display text-3xl text-ink sm:text-4xl">{t('about.title')}</h1>
        <p className="mx-auto mt-3 max-w-xl text-ink-soft">{t('about.subtitle')}</p>
      </Reveal>

      <div className="mt-12 space-y-6">
        {sections.map((section, i) => {
          const Icon = section.icon
          return (
            <Reveal key={section.title} delay={i * 80}>
              <Card className="p-6 sm:p-8">
                <div className="flex items-start gap-4">
                  <span className="flex h-10 w-10 flex-shrink-0 items-center justify-center rounded-full bg-teal-50 text-teal-600">
                    <Icon aria-hidden="true" size={20} />
                  </span>
                  <div>
                    <h2 className="text-lg text-ink">{section.title}</h2>
                    <p className="mt-2 text-sm text-ink-soft">{section.body}</p>
                  </div>
                </div>
              </Card>
            </Reveal>
          )
        })}
      </div>

      <div className="mt-10 flex flex-col items-center gap-3 text-center sm:flex-row sm:justify-center">
        <LinkButton href="/app/questionnaire" variant="primary">
          {t('about.startCta')}
        </LinkButton>
        <LinkButton href="/faq" variant="secondary">
          {t('about.faqCta')}
        </LinkButton>
      </div>
    </main>
  )
}
