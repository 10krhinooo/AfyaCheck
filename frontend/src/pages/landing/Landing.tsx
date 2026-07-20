import { ClipboardList, MapPin, ShieldCheck } from 'lucide-react'
import { Badge } from '../../components/Badge'
import { BrandMotif } from '../../components/BrandMotif'
import { LinkButton } from '../../components/Button'
import { Card } from '../../components/Card'
import { Reveal } from '../../components/Reveal'
import { useTranslation } from '../../lib/i18n'

const stepIcons = [ClipboardList, ShieldCheck, MapPin]

export default function Landing() {
  const { t } = useTranslation()
  const steps = [1, 2, 3].map((n) => ({
    icon: stepIcons[n - 1],
    title: t(`landing.step${n}Title`),
    body: t(`landing.step${n}Body`),
  }))

  return (
    <div className="min-h-screen">
      <main>
        <Reveal className="mx-auto max-w-7xl px-6 pb-20 pt-10">
          <div className="lg:grid lg:grid-cols-[3fr_2fr] lg:items-center lg:gap-12">
            <div>
              <Badge tone="low">{t('landing.badge')}</Badge>
              <h1 className="text-display-hero mt-6 text-5xl leading-[1.05] text-ink sm:text-6xl lg:text-7xl">
                {t('landing.heroTitle')}
              </h1>
              <p className="mt-5 max-w-xl text-lg text-ink-soft">{t('landing.heroBody')}</p>
              <div className="mt-8 flex flex-col items-start gap-3 sm:flex-row">
                <LinkButton href="/app/questionnaire" variant="primary" className="w-full sm:w-auto">
                  {t('landing.startCta')}
                </LinkButton>
                <LinkButton href="/app/health-centers" variant="secondary" className="w-full sm:w-auto">
                  {t('landing.findCenterCta')}
                </LinkButton>
              </div>
            </div>

            <div className="mt-12 lg:mt-0">
              <BrandMotif size="lg" className="mb-6" />
              <Card className="p-8">
                <p className="font-display text-3xl leading-tight text-teal-700 sm:text-4xl">
                  {t('landing.calloutTitle')}
                </p>
                <p className="mt-4 text-sm text-ink-soft">{t('landing.calloutBody')}</p>
              </Card>
            </div>
          </div>
        </Reveal>

        <section className="mx-auto max-w-6xl px-6 pb-24 pt-8">
          <div className="space-y-10">
            {steps.map((step, i) => {
              const Icon = step.icon
              const alignEnd = i % 2 === 1
              return (
                <Reveal key={step.title} delay={i * 80}>
                  <div
                    className={`lg:grid lg:grid-cols-[1fr_2fr] lg:items-center lg:gap-10 ${
                      alignEnd ? 'lg:[&>*:first-child]:order-2' : ''
                    }`}
                  >
                    <div className="flex items-center gap-4 lg:justify-center">
                      <span className="font-display text-5xl text-teal-300">{i + 1}</span>
                      <Icon aria-hidden="true" size={28} className="text-teal-500" />
                    </div>
                    <Card className="mt-4 p-6 lg:mt-0">
                      <h2 className="text-lg text-ink">{step.title}</h2>
                      <p className="mt-2 text-sm text-ink-soft">{step.body}</p>
                    </Card>
                  </div>
                </Reveal>
              )
            })}
          </div>
        </section>
      </main>
    </div>
  )
}
