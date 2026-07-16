import { BrandMotif } from '../../components/BrandMotif'
import { Card } from '../../components/Card'
import { Reveal } from '../../components/Reveal'

const faqs = [
  {
    question: 'Do I need to create an account?',
    answer:
      'No. You can take the assessment and see your result without signing in. An account is only needed if you want to keep a history of past assessments or, for staff, to access the admin dashboard.',
  },
  {
    question: 'Is my information kept private?',
    answer:
      'Yes. Your answers are used only to generate your risk assessment. We don’t sell or share your responses, and location access is used only at the moment you search for a nearby health center, see our Privacy Policy for details.',
  },
  {
    question: 'How accurate is the risk assessment?',
    answer:
      'The model is trained on established clinical risk factors and gives a plain-language risk level, low, moderate, or high, to help you decide on next steps. It is a screening tool, not a medical diagnosis, and it doesn’t replace testing or a conversation with a healthcare provider.',
  },
  {
    question: 'What happens if I get a high-risk result?',
    answer:
      'You’ll see recommended next steps, and you can search for the nearest health centers that offer testing and follow-up care directly from your results page.',
  },
  {
    question: 'Can I retake the assessment?',
    answer:
      'Yes, at any time. Circumstances change, and you can retake the assessment as often as you’d like from your results page or the dashboard.',
  },
  {
    question: 'Does the health center search track my location?',
    answer:
      'Your device’s location is used only to search for nearby centers when you ask, it isn’t stored or shared beyond that search.',
  },
]

export default function Faq() {
  return (
    <main className="mx-auto max-w-3xl px-6 py-12 sm:py-16">
      <Reveal className="text-center">
        <BrandMotif size="sm" className="mb-6" />
        <h1 className="font-display text-3xl text-ink sm:text-4xl">Frequently asked questions</h1>
        <p className="mx-auto mt-3 max-w-xl text-ink-soft">
          Answers to the questions we hear most about privacy, accuracy, and what comes next.
        </p>
      </Reveal>

      <div className="mt-10 space-y-3">
        {faqs.map((faq, i) => (
          <Reveal key={faq.question} delay={i * 60}>
            <Card className="overflow-hidden">
              <details className="group">
                <summary className="flex cursor-pointer list-none items-center justify-between gap-4 p-5 text-ink marker:content-none">
                  <span className="font-medium">{faq.question}</span>
                  <span
                    aria-hidden="true"
                    className="flex-shrink-0 text-teal-600 transition-transform duration-200 ease-editorial group-open:rotate-45"
                  >
                    +
                  </span>
                </summary>
                <p className="px-5 pb-5 text-sm text-ink-soft">{faq.answer}</p>
              </details>
            </Card>
          </Reveal>
        ))}
      </div>
    </main>
  )
}
