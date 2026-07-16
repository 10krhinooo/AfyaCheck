import { Lock, MapPin, ShieldCheck, Sparkles } from 'lucide-react'
import { BrandMotif } from '../../components/BrandMotif'
import { Card } from '../../components/Card'
import { LinkButton } from '../../components/Button'
import { Reveal } from '../../components/Reveal'

const sections = [
  {
    icon: Sparkles,
    title: 'An adaptive questionnaire, not a fixed form',
    body: 'Every question you see is chosen based on what you’ve already answered, so you’re never asked something irrelevant to your situation. Most people finish in a few minutes.',
  },
  {
    icon: ShieldCheck,
    title: 'A model trained on clinical risk factors',
    body: 'Your answers are scored by a model trained on established HIV/STI risk factors for the Kenyan context. The result is a plain-language risk level, low, moderate, or high, with next steps either way. It’s a screening tool, not a diagnosis.',
  },
  {
    icon: MapPin,
    title: 'A path to care, if you need one',
    body: 'If your result suggests testing or follow-up, we help you find the nearest health centers using your device’s location, searched at the moment you ask, never stored beyond that search.',
  },
  {
    icon: Lock,
    title: 'Privacy by default',
    body: 'You can take the assessment without creating an account. Fonts and other assets are self-hosted rather than pulled from third-party CDNs, so simply loading the site doesn’t leak information about you to anyone else. See our Privacy Policy for the full detail.',
  },
]

export default function About() {
  return (
    <main className="mx-auto max-w-3xl px-6 py-12 sm:py-16">
      <Reveal className="text-center">
        <BrandMotif size="md" className="mb-6" />
        <h1 className="font-display text-3xl text-ink sm:text-4xl">How AfyaCheck works</h1>
        <p className="mx-auto mt-3 max-w-xl text-ink-soft">
          A confidential, adaptive STI/HIV risk assessment built for the way people actually make
          decisions about their health.
        </p>
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
          Start free assessment
        </LinkButton>
        <LinkButton href="/faq" variant="secondary">
          Read the FAQ
        </LinkButton>
      </div>
    </main>
  )
}
