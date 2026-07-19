import { expect, test, type Page } from '@playwright/test'

// The questionnaire is adaptively sequenced by the decision-tree service, so the number and
// order of questions isn't fixed — this walks whatever question is on screen instead of
// scripting a specific sequence, with a generous cap as a safety net against infinite loops.
const MAX_QUESTIONS = 60

async function answerCurrentQuestionAndContinue(page: Page) {
  const radio = page.locator('input[type="radio"]').first()
  const numberField = page.locator('input[type="number"]')
  const textField = page.locator('input[type="text"]')

  if (await radio.isVisible().catch(() => false)) {
    await radio.check()
  } else if (await numberField.isVisible().catch(() => false)) {
    await numberField.fill('25')
  } else if (await textField.isVisible().catch(() => false)) {
    await textField.fill('N/A')
  } else {
    throw new Error('No recognizable question input found on the questionnaire page')
  }

  // Wait for the /next round-trip to actually resolve before letting the caller re-inspect
  // the page — otherwise the next loop iteration can grab a radio that's mid-removal from the
  // question still transitioning out, since clicking Continue doesn't synchronously swap the DOM.
  await Promise.all([
    page.waitForResponse(
      (res) => res.url().includes('/api/questionnaire/next') && res.request().method() === 'POST',
    ),
    page.getByRole('button', { name: 'Continue' }).click(),
  ])
}

test('completes the questionnaire and lands on a risk result', async ({ page }) => {
  // A full adaptive run is ~15 questions, each a real network round-trip to the backend —
  // comfortably over the default 30s Playwright test timeout on its own.
  test.setTimeout(90_000)

  await page.goto('/app/questionnaire')

  for (let i = 0; i < MAX_QUESTIONS; i++) {
    if (page.url().includes('/app/results')) break
    await page.getByRole('button', { name: 'Continue' }).waitFor({ state: 'visible' })
    await answerCurrentQuestionAndContinue(page)

    // The end-of-survey navigation to /app/results happens in a useEffect after the last
    // response lands, one render tick after the network round-trip we already awaited — give
    // it a beat before deciding whether to loop for another question.
    await page
      .waitForURL(/\/app\/results/, { timeout: 3000 })
      .catch(() => {})
  }

  await expect(page).toHaveURL(/\/app\/results/)
  await expect(page.getByText(/risk$/i)).toBeVisible()
  await expect(page.getByText('out of 100')).toBeVisible()
  await expect(page.getByRole('heading', { name: 'What this means for you' })).toBeVisible()
})
