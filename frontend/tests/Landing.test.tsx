import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import Landing from '../src/pages/landing/Landing'

describe('Landing', () => {
  it('renders the primary CTA', () => {
    render(<Landing />)
    expect(screen.getByRole('link', { name: /start free assessment/i })).toBeInTheDocument()
  })
})
