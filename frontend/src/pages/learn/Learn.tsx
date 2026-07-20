import { Link } from 'react-router-dom'
import { BrandMotif } from '../../components/BrandMotif'
import { Card } from '../../components/Card'
import { Reveal } from '../../components/Reveal'
import { LinkButton } from '../../components/Button'
import { useTranslation } from '../../lib/i18n'
import { topics } from './topics'

export default function Learn() {
  const { t } = useTranslation()
  return (
    <main className="mx-auto max-w-3xl px-6 py-12 sm:py-16">
      <Reveal className="text-center">
        <BrandMotif size="sm" className="mb-6" />
        <h1 className="font-display text-3xl text-ink sm:text-4xl">{t('learn.title')}</h1>
        <p className="mx-auto mt-3 max-w-xl text-ink-soft">{t('learn.subtitle')}</p>
      </Reveal>

      <div className="mt-10 space-y-4">
        {topics.map((topic, i) => (
          <Reveal key={topic.slug} delay={i * 60}>
            <Link to={`/learn/${topic.slug}`} className="block">
              <Card interactive className="p-6">
                <h2 className="font-display text-xl text-ink">{t(topic.titleKey)}</h2>
                <p className="mt-2 text-sm text-ink-soft">{t(topic.summaryKey)}</p>
                <span className="mt-3 inline-block text-sm font-medium text-teal-700">{t('learn.readMore')}</span>
              </Card>
            </Link>
          </Reveal>
        ))}
      </div>

      <Reveal className="mt-12 text-center">
        <p className="text-ink-soft">{t('learn.notSure')}</p>
        <LinkButton href="/app/questionnaire" variant="primary" className="mt-3">
          {t('learn.takeAssessment')}
        </LinkButton>
      </Reveal>
    </main>
  )
}
