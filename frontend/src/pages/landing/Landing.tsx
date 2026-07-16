import { Badge } from '../../components/Badge'
import { LinkButton } from '../../components/Button'
import { Card } from '../../components/Card'
import { login } from '../../lib/auth/keycloak'

const steps = [
  {
    title: 'Answer a few private questions',
    body: 'An adaptive questionnaire — each question is chosen based on what you’ve already told us, so it only takes a few minutes.',
  },
  {
    title: 'Get a clear risk assessment',
    body: 'A model trained on clinical risk factors gives you a plain-language result, not a diagnosis — with next steps either way.',
  },
  {
    title: 'Find care nearby, if you need it',
    body: 'If your result suggests testing or follow-up, we’ll show you the nearest health centers that can help.',
  },
]

export default function Landing() {
  return (
    <div className="min-h-screen">
      <header className="mx-auto flex max-w-5xl items-center justify-between px-6 py-6">
        <span className="font-display text-xl text-teal-700">AfyaCheck</span>
        <button
          type="button"
          onClick={() => login('/app/dashboard')}
          className="text-sm font-medium text-ink-soft hover:text-ink"
        >
          Sign in
        </button>
      </header>

      <main>
        <section className="mx-auto max-w-3xl px-6 pb-20 pt-10 text-center">
          <Badge tone="low">Free &middot; Confidential &middot; No account required to start</Badge>
          <h1 className="mt-6 text-4xl leading-tight text-ink sm:text-5xl">
            Know your risk. Privately, in a few minutes.
          </h1>
          <p className="mx-auto mt-5 max-w-xl text-lg text-ink-soft">
            AfyaCheck is a confidential STI/HIV risk assessment tool. Answer honestly, get a
            clear picture, and find care nearby if you need it &mdash; no judgment, no waiting room.
          </p>
          <div className="mt-8 flex flex-col items-center gap-3 sm:flex-row sm:justify-center">
            <LinkButton href="/app/questionnaire" variant="primary" className="w-full sm:w-auto">
              Start free assessment
            </LinkButton>
            <LinkButton href="/app/health-centers" variant="secondary" className="w-full sm:w-auto">
              Find a health center
            </LinkButton>
          </div>
        </section>

        <section className="mx-auto max-w-5xl px-6 pb-24">
          <div className="grid gap-5 sm:grid-cols-3">
            {steps.map((step, i) => (
              <Card key={step.title} className="p-6">
                <span className="font-display text-2xl text-teal-500">{i + 1}</span>
                <h2 className="mt-3 text-lg text-ink">{step.title}</h2>
                <p className="mt-2 text-sm text-ink-soft">{step.body}</p>
              </Card>
            ))}
          </div>
        </section>
      </main>

      <footer className="border-t border-teal-100 py-8 text-center text-sm text-ink-soft">
        Your answers are confidential and used only to generate your assessment.
      </footer>
    </div>
  )
}
