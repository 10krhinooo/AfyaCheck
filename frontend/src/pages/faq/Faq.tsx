import { BrandMotif } from '../../components/BrandMotif'
import { Card } from '../../components/Card'
import { Reveal } from '../../components/Reveal'
import { useTranslation } from '../../lib/i18n'

export default function Faq() {
  const { t } = useTranslation()
  const faqs = [1, 2, 3, 4, 5, 6].map((n) => ({
    question: t(`faq.q${n}`),
    answer: t(`faq.a${n}`),
  }))

  return (
    <main className="mx-auto max-w-3xl px-6 py-12 sm:py-16">
      <Reveal className="text-center">
        <BrandMotif size="sm" className="mb-6" />
        <h1 className="font-display text-3xl text-ink sm:text-4xl">{t('faq.title')}</h1>
        <p className="mx-auto mt-3 max-w-xl text-ink-soft">{t('faq.subtitle')}</p>
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
