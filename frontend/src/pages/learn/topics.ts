// Static education content, prerendered at build time (see vite.config.ts
// ssgOptions.includedRoutes). Written for a general Kenyan audience: plain language,
// no jargon without explanation, and always pointing back to testing as the next step.
// Strings live in the i18n dictionaries (lib/i18n/locales/*.json, "learnTopics" namespace)
// so this module only holds the translation keys and section shape.

export interface TopicSection {
  headingKey: string
  paragraphKeys: string[]
  bulletKeys?: string[]
}

export interface Topic {
  slug: string
  titleKey: string
  summaryKey: string
  sections: TopicSection[]
}

export const topics: Topic[] = [
  {
    slug: 'sti-basics',
    titleKey: 'learnTopics.stiBasics.title',
    summaryKey: 'learnTopics.stiBasics.summary',
    sections: [
      {
        headingKey: 'learnTopics.stiBasics.s1Heading',
        paragraphKeys: ['learnTopics.stiBasics.s1P1', 'learnTopics.stiBasics.s1P2'],
      },
      {
        headingKey: 'learnTopics.stiBasics.s2Heading',
        paragraphKeys: ['learnTopics.stiBasics.s2P1', 'learnTopics.stiBasics.s2P2'],
      },
      {
        headingKey: 'learnTopics.stiBasics.s3Heading',
        paragraphKeys: ['learnTopics.stiBasics.s3P1'],
        bulletKeys: ['learnTopics.stiBasics.s3B1', 'learnTopics.stiBasics.s3B2', 'learnTopics.stiBasics.s3B3'],
      },
      {
        headingKey: 'learnTopics.stiBasics.s4Heading',
        paragraphKeys: ['learnTopics.stiBasics.s4P1'],
      },
    ],
  },
  {
    slug: 'hiv-prevention',
    titleKey: 'learnTopics.hivPrevention.title',
    summaryKey: 'learnTopics.hivPrevention.summary',
    sections: [
      {
        headingKey: 'learnTopics.hivPrevention.s1Heading',
        paragraphKeys: ['learnTopics.hivPrevention.s1P1'],
      },
      {
        headingKey: 'learnTopics.hivPrevention.s2Heading',
        paragraphKeys: ['learnTopics.hivPrevention.s2P1', 'learnTopics.hivPrevention.s2P2'],
      },
      {
        headingKey: 'learnTopics.hivPrevention.s3Heading',
        paragraphKeys: ['learnTopics.hivPrevention.s3P1'],
      },
      {
        headingKey: 'learnTopics.hivPrevention.s4Heading',
        paragraphKeys: ['learnTopics.hivPrevention.s4P1', 'learnTopics.hivPrevention.s4P2'],
      },
      {
        headingKey: 'learnTopics.hivPrevention.s5Heading',
        paragraphKeys: [],
        bulletKeys: [
          'learnTopics.hivPrevention.s5B1',
          'learnTopics.hivPrevention.s5B2',
          'learnTopics.hivPrevention.s5B3',
        ],
      },
    ],
  },
  {
    slug: 'testing',
    titleKey: 'learnTopics.testing.title',
    summaryKey: 'learnTopics.testing.summary',
    sections: [
      {
        headingKey: 'learnTopics.testing.s1Heading',
        paragraphKeys: ['learnTopics.testing.s1P1'],
      },
      {
        headingKey: 'learnTopics.testing.s2Heading',
        paragraphKeys: ['learnTopics.testing.s2P1'],
      },
      {
        headingKey: 'learnTopics.testing.s3Heading',
        paragraphKeys: ['learnTopics.testing.s3P1', 'learnTopics.testing.s3P2'],
      },
      {
        headingKey: 'learnTopics.testing.s4Heading',
        paragraphKeys: ['learnTopics.testing.s4P1'],
      },
    ],
  },
]
