import { BrandMotif } from '../../components/BrandMotif'
import { LinkButton } from '../../components/Button'

export default function NotFound() {
  return (
    <main className="mx-auto max-w-xl px-6 py-20 text-center">
      <BrandMotif size="sm" className="mb-6" />
      <p className="font-display text-6xl text-teal-700">404</p>
      <h1 className="mt-3 font-display text-2xl text-ink">Page not found</h1>
      <p className="mt-2 text-ink-soft">
        The page you’re looking for doesn’t exist, or may have moved.
      </p>
      <LinkButton href="/" variant="primary" className="mt-6">
        Back to home
      </LinkButton>
    </main>
  )
}
