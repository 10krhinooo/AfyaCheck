import { useState, type FormEvent } from 'react'
import { Button } from '../../components/Button'
import type { Question } from '../../lib/types'

export function QuestionForm({
  question,
  submitting,
  onSubmit,
}: {
  question: Question
  submitting: boolean
  onSubmit: (value: string) => void
}) {
  const [value, setValue] = useState('')

  function handleSubmit(e: FormEvent) {
    e.preventDefault()
    if (value.trim() === '') return
    onSubmit(value)
    setValue('')
  }

  return (
    <form onSubmit={handleSubmit} className="mt-6">
      {question.type === 'radio' && (
        <fieldset className="space-y-3">
          <legend className="sr-only">{question.text}</legend>
          {(question.options ?? []).map((option) => (
            <label
              key={option}
              className="flex cursor-pointer items-center gap-3 rounded-xl border border-teal-100 bg-white px-4 py-3 text-ink transition-colors hover:border-teal-300 has-[:checked]:border-teal-500 has-[:checked]:bg-teal-50"
            >
              <input
                type="radio"
                name={question.key}
                value={option}
                checked={value === option}
                onChange={(e) => setValue(e.target.value)}
                className="h-4 w-4 accent-teal-600"
              />
              <span>{option}</span>
            </label>
          ))}
        </fieldset>
      )}

      {question.type === 'number' && (
        <label className="block">
          <span className="sr-only">{question.text}</span>
          <input
            type="number"
            min={question.min}
            max={question.max}
            value={value}
            onChange={(e) => setValue(e.target.value)}
            className="w-full rounded-xl border border-teal-100 bg-white px-4 py-3 text-lg"
            required
          />
        </label>
      )}

      {question.type === 'text' && (
        <label className="block">
          <span className="sr-only">{question.text}</span>
          <input
            type="text"
            value={value}
            onChange={(e) => setValue(e.target.value)}
            className="w-full rounded-xl border border-teal-100 bg-white px-4 py-3 text-lg"
            required
          />
        </label>
      )}

      <Button type="submit" disabled={submitting || value.trim() === ''} className="mt-6 w-full sm:w-auto">
        {submitting ? 'Saving…' : 'Continue'}
      </Button>
    </form>
  )
}
