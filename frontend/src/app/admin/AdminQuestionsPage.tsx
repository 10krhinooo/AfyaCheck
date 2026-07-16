import { useEffect, useState } from 'react'
import { AdminNav } from './AdminNav'
import { Button } from '../../components/Button'
import { Card } from '../../components/Card'
import { apiFetch, apiPost } from '../../lib/api-client'
import type { AdminQuestion, QuestionsResponse } from './types'

const emptyDraft = { questionKey: '', questionText: '', questionType: 'yes_no', sectionTitle: '' }

export default function AdminQuestionsPage() {
  const [data, setData] = useState<QuestionsResponse | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [draft, setDraft] = useState(emptyDraft)
  const [saving, setSaving] = useState(false)

  function load() {
    apiFetch<QuestionsResponse>('/api/admin/questions')
      .then(setData)
      .catch((err: Error) => setError(err.message))
  }

  useEffect(load, [])

  async function addQuestion() {
    if (!draft.questionKey || !draft.questionText) return
    setSaving(true)
    try {
      await apiPost('/api/admin/questions/add', draft)
      setDraft(emptyDraft)
      load()
    } catch (err) {
      setError((err as Error).message)
    } finally {
      setSaving(false)
    }
  }

  async function deleteQuestion(id: number) {
    try {
      await apiPost('/api/admin/questions/delete', { questionId: id })
      load()
    } catch (err) {
      setError((err as Error).message)
    }
  }

  return (
    <main className="mx-auto max-w-5xl px-6 py-10">
      <h1 className="text-2xl text-ink">Question management</h1>
      <AdminNav />

      {error && <p className="text-coral-700">{error}</p>}

      <Card className="p-6">
        <h2 className="text-lg text-ink">Add a question</h2>
        <div className="mt-4 grid gap-3 sm:grid-cols-2">
          <label className="block text-sm">
            <span className="text-ink-soft">Question key</span>
            <input
              value={draft.questionKey}
              onChange={(e) => setDraft({ ...draft, questionKey: e.target.value })}
              className="mt-1 w-full rounded-lg border border-teal-100 px-3 py-2"
            />
          </label>
          <label className="block text-sm">
            <span className="text-ink-soft">Section</span>
            <input
              value={draft.sectionTitle}
              onChange={(e) => setDraft({ ...draft, sectionTitle: e.target.value })}
              className="mt-1 w-full rounded-lg border border-teal-100 px-3 py-2"
            />
          </label>
          <label className="block text-sm sm:col-span-2">
            <span className="text-ink-soft">Question text</span>
            <input
              value={draft.questionText}
              onChange={(e) => setDraft({ ...draft, questionText: e.target.value })}
              className="mt-1 w-full rounded-lg border border-teal-100 px-3 py-2"
            />
          </label>
          <label className="block text-sm">
            <span className="text-ink-soft">Type</span>
            <select
              value={draft.questionType}
              onChange={(e) => setDraft({ ...draft, questionType: e.target.value })}
              className="mt-1 w-full rounded-lg border border-teal-100 px-3 py-2"
            >
              {(data?.questionTypes ?? ['yes_no', 'multiple_choice', 'text', 'number', 'choice']).map((t) => (
                <option key={t} value={t}>
                  {t}
                </option>
              ))}
            </select>
          </label>
        </div>
        <Button className="mt-4" onClick={addQuestion} disabled={saving}>
          {saving ? 'Adding…' : 'Add question'}
        </Button>
      </Card>

      <div className="mt-6 space-y-3">
        {data?.questions.map((question: AdminQuestion) => (
          <Card key={question.id} className="flex items-center justify-between gap-4 p-4">
            <div>
              <p className="font-medium text-ink">{question.questionText}</p>
              <p className="text-sm text-ink-soft">
                {question.questionKey} &middot; {question.questionType} &middot; {question.sectionTitle}
              </p>
            </div>
            <Button variant="ghost" onClick={() => deleteQuestion(question.id)}>
              Delete
            </Button>
          </Card>
        ))}
      </div>
    </main>
  )
}
