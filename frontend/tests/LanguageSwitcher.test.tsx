import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { LanguageSwitcher } from '../src/components/LanguageSwitcher'
import { I18nProvider } from '../src/lib/i18n'

describe('LanguageSwitcher', () => {
  it('switches the document language when a different option is chosen', async () => {
    const user = userEvent.setup()
    render(
      <I18nProvider>
        <LanguageSwitcher />
      </I18nProvider>,
    )

    expect(document.documentElement.lang).toBe('en')

    await user.click(screen.getByRole('button', { name: 'SW' }))

    expect(document.documentElement.lang).toBe('sw')
    expect(screen.getByRole('button', { name: 'SW' })).toHaveAttribute('aria-pressed', 'true')
  })
})
