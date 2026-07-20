import { BrandMotif } from '../../components/BrandMotif'
import { LinkButton } from '../../components/Button'
import { useTranslation } from '../../lib/i18n'

export default function NotFound() {
  const { t } = useTranslation()
  return (
    <main className="mx-auto max-w-xl px-6 py-20 text-center">
      <BrandMotif size="sm" className="mb-6" />
      <p className="font-display text-6xl text-teal-700">404</p>
      <h1 className="mt-3 font-display text-2xl text-ink">{t('notFound.title')}</h1>
      <p className="mt-2 text-ink-soft">{t('notFound.body')}</p>
      <LinkButton href="/" variant="primary" className="mt-6">
        {t('notFound.backHome')}
      </LinkButton>
    </main>
  )
}
