import { Link } from 'react-router-dom'
import { BrandMotif } from '../../components/BrandMotif'
import { Card } from '../../components/Card'
import { Reveal } from '../../components/Reveal'
import { LinkButton } from '../../components/Button'
import { topics } from './topics'

export default function Learn() {
  return (
    <main className="mx-auto max-w-3xl px-6 py-12 sm:py-16">
      <Reveal className="text-center">
        <BrandMotif size="sm" className="mb-6" />
        <h1 className="font-display text-3xl text-ink sm:text-4xl">Learn about sexual health</h1>
        <p className="mx-auto mt-3 max-w-xl text-ink-soft">
          Clear, judgment-free answers about STIs, HIV prevention, and testing, so decisions about your
          health start from facts, not fear.
        </p>
      </Reveal>

      <div className="mt-10 space-y-4">
        {topics.map((topic, i) => (
          <Reveal key={topic.slug} delay={i * 60}>
            <Link to={`/learn/${topic.slug}`} className="block">
              <Card interactive className="p-6">
                <h2 className="font-display text-xl text-ink">{topic.title}</h2>
                <p className="mt-2 text-sm text-ink-soft">{topic.summary}</p>
                <span className="mt-3 inline-block text-sm font-medium text-teal-700">Read more →</span>
              </Card>
            </Link>
          </Reveal>
        ))}
      </div>

      <Reveal className="mt-12 text-center">
        <p className="text-ink-soft">Not sure where you stand?</p>
        <LinkButton href="/app/questionnaire" variant="primary" className="mt-3">
          Take the free assessment
        </LinkButton>
      </Reveal>
    </main>
  )
}
