import { useEffect, useRef, useState, type ReactNode } from 'react'

// Scroll-triggered fade/slide-in for the editorial landing page. Content is ALWAYS
// visible by default (no opacity-0 baked into the initial render). This matters
// concretely because "/" is the one SSG-prerendered route (see vite.config.ts
// ssgOptions.includedRoutes), so crawlers and no-JS/slow-JS users must see the full
// page immediately. Only once we've confirmed, client-side, that (a) the user hasn't
// asked for reduced motion and (b) the element isn't already on screen, do we hide it
// and animate it back in when it scrolls into view.
export function useInView<T extends HTMLElement>() {
  const ref = useRef<T>(null)
  const [pending, setPending] = useState(false)
  const [revealed, setRevealed] = useState(false)

  useEffect(() => {
    const node = ref.current
    if (!node) return

    const reducedMotion =
      typeof window.matchMedia === 'function' && window.matchMedia('(prefers-reduced-motion: reduce)').matches
    if (reducedMotion) return
    // No IntersectionObserver support (very old browsers, some test environments), stay
    // visible rather than risk content that never reveals.
    if (typeof IntersectionObserver === 'undefined') return

    const rect = node.getBoundingClientRect()
    const alreadyVisible = rect.top < window.innerHeight && rect.bottom > 0
    if (alreadyVisible) return

    setPending(true)
    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          setRevealed(true)
          observer.unobserve(node)
        }
      },
      { threshold: 0.15, rootMargin: '0px 0px -10% 0px' },
    )
    observer.observe(node)
    return () => observer.disconnect()
  }, [])

  return { ref, pending: pending && !revealed, revealed }
}

export function Reveal({
  children,
  delay = 0,
  className = '',
}: {
  children: ReactNode
  delay?: number
  className?: string
}) {
  const { ref, pending, revealed } = useInView<HTMLDivElement>()

  return (
    <div
      ref={ref}
      className={`${pending ? 'opacity-0 translate-y-3' : ''} ${revealed ? 'animate-fade-in-up' : ''} ${className}`}
      style={pending || revealed ? { animationDelay: `${delay}ms` } : undefined}
    >
      {children}
    </div>
  )
}
