const sections = [
  {
    title: 'What AfyaCheck is',
    body: 'AfyaCheck is a confidential STI/HIV risk screening tool. It gives you a plain-language risk level based on your answers to an adaptive questionnaire, and helps you find nearby health centers if you need testing or follow-up care.',
  },
  {
    title: 'What it isn’t',
    body: 'AfyaCheck is not a medical diagnosis, and it doesn’t replace testing or advice from a qualified healthcare provider. If you have symptoms or concerns, please seek care regardless of your assessment result.',
  },
  {
    title: 'Using the service',
    body: 'You agree to answer questions honestly to the best of your knowledge, and not to use the service to assess anyone other than yourself without their consent. Accounts, where used, are for individual, non-commercial use.',
  },
  {
    title: 'Availability',
    body: 'We aim to keep AfyaCheck available and working correctly, but the assessment, risk scoring, and health center search all depend on external services (mapping and machine learning components) that can occasionally be unavailable. We’ll always try to show a clear message rather than fail silently.',
  },
  {
    title: 'Changes to these terms',
    body: 'If these terms change in a way that materially affects how the service works, we’ll update this page and note the change.',
  },
]

export default function TermsOfService() {
  return (
    <main className="mx-auto max-w-3xl px-6 py-12 sm:py-16">
      <h1 className="font-display text-3xl text-ink sm:text-4xl">Terms of Service</h1>
      <p className="mt-3 text-ink-soft">
        The short version: use AfyaCheck honestly, treat it as a screening tool rather than a
        diagnosis, and let us know if something isn’t working.
      </p>

      <div className="mt-10 space-y-8">
        {sections.map((section) => (
          <section key={section.title}>
            <h2 className="text-lg text-ink">{section.title}</h2>
            <p className="mt-2 text-sm text-ink-soft">{section.body}</p>
          </section>
        ))}
      </div>
    </main>
  )
}
