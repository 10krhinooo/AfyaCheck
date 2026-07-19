import { Check } from 'lucide-react'
import { useState, type FormEvent } from 'react'
import { Button } from '../../components/Button'
import { TextField } from '../../components/TextField'
import { useTranslation } from '../../lib/i18n'
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
  const { t } = useTranslation()

  // Option VALUES are canonical English (the ML pipeline's inputs); only the LABEL is
  // translated. t() returns the key itself when no translation exists, so fall back to
  // showing the raw option value in that case (and always in English).
  function optionLabel(option: string): string {
    const translated = t(`options.${option}`)
    return translated === `options.${option}` ? option : translated
  }

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
              className="relative flex cursor-pointer items-center gap-3 rounded-xl border border-teal-100 bg-white px-4 py-3 pr-10 text-ink transition-[border-color,background-color,transform] duration-200 ease-editorial hover:border-teal-300 has-[:checked]:border-teal-500 has-[:checked]:bg-teal-50 has-[:checked]:ring-2 has-[:checked]:ring-teal-100 motion-safe:has-[:checked]:scale-[1.01]"
            >
              <input
                type="radio"
                name={question.key}
                value={option}
                checked={value === option}
                onChange={(e) => setValue(e.target.value)}
                className="peer h-4 w-4 accent-teal-600"
              />
              <span>{optionLabel(option)}</span>
              <Check aria-hidden="true" size={18} className="absolute right-4 hidden text-teal-600 peer-checked:block" />
            </label>
          ))}
        </fieldset>
      )}

      {question.type === 'number' && (
        <TextField
          label={question.text}
          visuallyHiddenLabel
          type="number"
          min={question.min}
          max={question.max}
          value={value}
          onChange={(e) => setValue(e.target.value)}
          required
        />
      )}

      {question.type === 'text' && (
        <TextField
          label={question.text}
          visuallyHiddenLabel
          type="text"
          value={value}
          onChange={(e) => setValue(e.target.value)}
          required
        />
      )}

      <Button
        type="submit"
        loading={submitting}
        disabled={value.trim() === ''}
        className="mt-6 w-full sm:w-auto"
      >
        {t('questionnaire.continue')}
      </Button>
    </form>
  )
}
