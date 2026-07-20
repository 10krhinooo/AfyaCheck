import { Link, useParams } from 'react-router-dom'
import { Card } from '../../components/Card'
import { Reveal } from '../../components/Reveal'
import { LinkButton } from '../../components/Button'
import NotFound from '../not-found/NotFound'
import { topics } from './topics'

export default function LearnTopic() {
  const { slug } = useParams<{ slug: string }>()
  const topic = topics.find((t) => t.slug === slug)

  if (!topic) {
    return <NotFound />
  }

  return (
    <main className="mx-auto max-w-3xl px-6 py-12 sm:py-16">
      <Reveal>
        <Link to="/learn" className="text-sm font-medium text-teal-700">
          ← All topics
        </Link>
        <h1 className="mt-4 font-display text-3xl text-ink sm:text-4xl">{topic.title}</h1>
        <p className="mt-3 text-ink-soft">{topic.summary}</p>
      </Reveal>

      <div className="mt-10 space-y-6">
        {topic.sections.map((section, i) => (
          <Reveal key={section.heading} delay={i * 60}>
            <Card className="p-6 sm:p-8">
              <h2 className="font-display text-xl text-ink">{section.heading}</h2>
              {section.paragraphs.map((paragraph) => (
                <p key={paragraph.slice(0, 40)} className="mt-3 text-ink-soft">
                  {paragraph}
                </p>
              ))}
              {section.bullets && (
                <ul className="mt-3 space-y-2">
                  {section.bullets.map((bullet) => (
                    <li key={bullet.slice(0, 40)} className="flex gap-3 text-ink-soft">
                      <span aria-hidden className="mt-2 h-1.5 w-1.5 flex-shrink-0 rounded-full bg-teal-500" />
                      <span>{bullet}</span>
                    </li>
                  ))}
                </ul>
              )}
            </Card>
          </Reveal>
        ))}
      </div>

      <Reveal className="mt-10">
        <Card className="p-6 text-center sm:p-8">
          <p className="text-ink-soft">
            This information is educational and doesn’t replace advice from a healthcare provider.
          </p>
          <div className="mt-4 flex flex-col justify-center gap-3 sm:flex-row">
            <LinkButton href="/app/questionnaire" variant="primary">
              Check your risk
            </LinkButton>
            <LinkButton href="/app/health-centers" variant="secondary">
              Find a health center
            </LinkButton>
          </div>
        </Card>
      </Reveal>
    </main>
  )
}
