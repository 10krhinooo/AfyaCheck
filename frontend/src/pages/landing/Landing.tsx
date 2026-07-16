import { ClipboardList, MapPin, ShieldCheck } from 'lucide-react'
import { Badge } from '../../components/Badge'
import { BrandMotif } from '../../components/BrandMotif'
import { LinkButton } from '../../components/Button'
import { Card } from '../../components/Card'
import { Reveal } from '../../components/Reveal'

const steps = [
  {
    icon: ClipboardList,
    title: 'Answer a few private questions',
    body: 'An adaptive questionnaire where each question is chosen based on what you’ve already told us, so it only takes a few minutes.',
  },
  {
    icon: ShieldCheck,
    title: 'Get a clear risk assessment',
    body: 'A model trained on clinical risk factors gives you a plain-language result, not a diagnosis, with next steps either way.',
  },
  {
    icon: MapPin,
    title: 'Find care nearby, if you need it',
    body: 'If your result suggests testing or follow-up, we’ll show you the nearest health centers that can help.',
  },
]

export default function Landing() {
  return (
    <div className="min-h-screen">
      <main>
        <Reveal className="mx-auto max-w-7xl px-6 pb-20 pt-10">
          <div className="lg:grid lg:grid-cols-[3fr_2fr] lg:items-center lg:gap-12">
            <div>
              <Badge tone="low">Free &middot; Confidential &middot; No account required to start</Badge>
              <h1 className="text-display-hero mt-6 text-5xl leading-[1.05] text-ink sm:text-6xl lg:text-7xl">
                Know your risk. Privately, in a few minutes.
              </h1>
              <p className="mt-5 max-w-xl text-lg text-ink-soft">
                AfyaCheck is a confidential STI/HIV risk assessment tool. Answer honestly, get a
                clear picture, and find care nearby if you need it, with no judgment and no waiting room.
              </p>
              <div className="mt-8 flex flex-col items-start gap-3 sm:flex-row">
                <LinkButton href="/app/questionnaire" variant="primary" className="w-full sm:w-auto">
                  Start free assessment
                </LinkButton>
                <LinkButton href="/app/health-centers" variant="secondary" className="w-full sm:w-auto">
                  Find a health center
                </LinkButton>
              </div>
            </div>

            <div className="mt-12 lg:mt-0">
              <BrandMotif size="lg" className="mb-6" />
              <Card className="p-8">
                <p className="font-display text-3xl leading-tight text-teal-700 sm:text-4xl">
                  A calm, private space, not a clinical scan.
                </p>
                <p className="mt-4 text-sm text-ink-soft">
                  Built for the way people actually make decisions about their health: privately,
                  on their own time, with a clear next step either way.
                </p>
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
