const sections = [
  {
    title: 'What we collect',
    body: 'Your questionnaire answers, an optional account (email and password, or a linked Google/GitHub sign-in), and, only when you search for a nearby health center, a one-time read of your device’s location.',
  },
  {
    title: 'How we use it',
    body: 'Questionnaire answers are used only to generate your risk assessment. Location is used only to search for nearby health centers at the moment you ask; it is not stored or shared beyond that search. Account details are used to let you sign in and, if you choose, keep a history of past assessments.',
  },
  {
    title: 'What we don’t do',
    body: 'We don’t sell your data, and we don’t share your questionnaire answers or assessment results with third parties. We don’t load fonts or other assets from third-party CDNs, so visiting the site doesn’t expose your browsing to anyone beyond AfyaCheck itself.',
  },
  {
    title: 'Who can see it',
    body: 'Your individual answers and results are visible only to you. AfyaCheck staff with admin access can see aggregate, anonymized usage statistics (such as total assessments completed) to operate the service, not your individual responses.',
  },
  {
    title: 'Your choices',
    body: 'You can take the assessment without an account. If you do create one, you can request that it, and any assessment history tied to it, be deleted at any time by contacting us.',
  },
]

export default function PrivacyPolicy() {
  return (
    <main className="mx-auto max-w-3xl px-6 py-12 sm:py-16">
      <h1 className="font-display text-3xl text-ink sm:text-4xl">Privacy Policy</h1>
      <p className="mt-3 text-ink-soft">
        AfyaCheck is built around one rule: your health information is yours. This page explains,
        in plain language, what we collect and why.
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
