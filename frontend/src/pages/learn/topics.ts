// Static education content, prerendered at build time (see vite.config.ts
// ssgOptions.includedRoutes). Written for a general Kenyan audience: plain language,
// no jargon without explanation, and always pointing back to testing as the next step.

export interface TopicSection {
  heading: string
  paragraphs: string[]
  bullets?: string[]
}

export interface Topic {
  slug: string
  title: string
  summary: string
  sections: TopicSection[]
}

export const topics: Topic[] = [
  {
    slug: 'sti-basics',
    title: 'Understanding STIs',
    summary:
      'What sexually transmitted infections are, why most show no symptoms, and which ones are fully curable.',
    sections: [
      {
        heading: 'What is an STI?',
        paragraphs: [
          'A sexually transmitted infection (STI) is an infection passed from one person to another mainly through sexual contact — vaginal, anal, or oral. Anyone who is sexually active can get one, regardless of age, gender, or how many partners they have had.',
          'Common STIs in Kenya include chlamydia, gonorrhoea, syphilis, trichomoniasis, genital herpes, human papillomavirus (HPV), and HIV.',
        ],
      },
      {
        heading: 'Most STIs have no symptoms',
        paragraphs: [
          'This is the most important fact to know: the majority of STIs cause no symptoms at all, especially early on. You can feel completely healthy, and so can a partner, while an infection is present and passing on. Waiting for symptoms before testing means most infections are missed.',
          'When symptoms do appear, they can include unusual discharge, pain or burning when urinating, sores or bumps in the genital area, or pain during sex. Any of these is a reason to visit a health facility promptly.',
        ],
      },
      {
        heading: 'Curable vs. manageable',
        paragraphs: [
          'Many people avoid testing out of fear, but the news is better than most expect.',
        ],
        bullets: [
          'Fully curable with medicine: chlamydia, gonorrhoea, syphilis, and trichomoniasis are cured with a course of antibiotics — usually a single visit.',
          'Manageable for a normal, healthy life: herpes and HIV cannot yet be cured, but daily treatment keeps them controlled. A person on effective HIV treatment can live a full life and cannot pass HIV to partners once the virus is undetectable.',
          'Preventable by vaccine: HPV, the virus behind most cervical cancer, is vaccine-preventable — the vaccine is offered free to girls aged 10–14 in Kenya.',
        ],
      },
      {
        heading: 'Untreated STIs cause real harm',
        paragraphs: [
          'Left untreated, STIs can cause infertility in both women and men, pregnancy complications, chronic pelvic pain, and a higher chance of getting or passing HIV. Early treatment prevents all of this — which is why regular testing matters even when you feel fine.',
        ],
      },
    ],
  },
  {
    slug: 'hiv-prevention',
    title: 'Preventing HIV',
    summary: 'The full toolkit: condoms, PrEP, PEP, treatment as prevention, and how each one works.',
    sections: [
      {
        heading: 'Condoms',
        paragraphs: [
          'Used correctly and consistently, condoms are highly effective against HIV and most other STIs, and they remain the only method that also prevents pregnancy. They are free at public health facilities across Kenya.',
        ],
      },
      {
        heading: 'PrEP — medicine before exposure',
        paragraphs: [
          'Pre-exposure prophylaxis (PrEP) is a daily pill taken by someone who is HIV-negative to prevent infection. Taken as prescribed, it reduces the risk of getting HIV from sex by about 99%. PrEP is available free at many public and partner facilities in Kenya.',
          'PrEP is worth discussing with a provider if your partner is living with HIV, you have partners whose status you don’t know, or condoms are not always used.',
        ],
      },
      {
        heading: 'PEP — emergency medicine after exposure',
        paragraphs: [
          'Post-exposure prophylaxis (PEP) is a 28-day course of medicine started within 72 hours of a possible exposure — the sooner the better. If a condom broke, or you had unprotected sex with someone whose status you don’t know, go to a health facility immediately. After 72 hours PEP no longer works.',
        ],
      },
      {
        heading: 'Treatment as prevention (U=U)',
        paragraphs: [
          'A person living with HIV who takes treatment daily can reach an undetectable viral load — so little virus in the body that tests can’t find it. Someone who is undetectable cannot transmit HIV through sex. This is often written as U=U: Undetectable = Untransmittable.',
          'This is why knowing your status helps protect the people you care about: treatment protects both your health and your partners.',
        ],
      },
      {
        heading: 'Other protection that adds up',
        paragraphs: [],
        bullets: [
          'Voluntary medical male circumcision reduces a man’s risk of getting HIV from a female partner by about 60%.',
          'Treating other STIs promptly lowers HIV risk, because sores and inflammation make transmission easier.',
          'Regular testing with your partner turns guesswork into a plan.',
        ],
      },
    ],
  },
  {
    slug: 'testing',
    title: 'Getting tested',
    summary: 'When to test, what actually happens at the clinic, and why the window period matters.',
    sections: [
      {
        heading: 'When should you test?',
        paragraphs: [
          'Test at least once a year if you are sexually active — more often (every 3–6 months) if you have new or multiple partners, a partner living with HIV, or don’t always use condoms. Also test at the start of a new relationship, during pregnancy, and any time you have symptoms or a partner tells you they tested positive for an STI.',
        ],
      },
      {
        heading: 'The window period',
        paragraphs: [
          'After an exposure, there is a short period before a test can detect the infection — for the rapid HIV tests used in Kenya this is usually up to about 6 weeks. A negative test taken too soon can be falsely reassuring. If a recent exposure worries you, test now and again after the window period has passed.',
        ],
      },
      {
        heading: 'What happens at the clinic',
        paragraphs: [
          'HIV testing in Kenya is free at public facilities, quick, and confidential. A counsellor talks with you first, a finger-prick rapid test gives a result in about 15–20 minutes, and the counsellor explains what the result means and what happens next. Testing for other STIs may involve a urine sample, a swab, or a blood draw.',
          'Self-test kits are also sold in pharmacies and offered at many facilities: you swab your gums or prick a finger and read the result yourself in private. A positive self-test always needs confirmation at a facility.',
        ],
      },
      {
        heading: 'If the result is positive',
        paragraphs: [
          'A positive result is the beginning of treatment, not the end of anything. Curable STIs are treated on the spot. For HIV, treatment starts immediately, is free, and works — people who start treatment early live as long as anyone else. Your result is confidential, and clinic staff will help you think through talking to partners.',
        ],
      },
    ],
  },
]
