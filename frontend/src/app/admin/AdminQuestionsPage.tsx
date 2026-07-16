import { useEffect, useState } from 'react'
import { AdminNav } from './AdminNav'
import { Button } from '../../components/Button'
import { Card } from '../../components/Card'
import { Skeleton } from '../../components/Skeleton'
import { StatusMessage } from '../../components/StatusMessage'
import { TextField } from '../../components/TextField'
import { apiFetch, apiPost } from '../../lib/api-client'
import type { AdminQuestion, QuestionsResponse } from './types'

const emptyDraft = { questionKey: '', questionText: '', questionType: 'yes_no', sectionTitle: '' }
const LOAD_ERROR = 'We couldn’t load the question bank. Check your connection and try again.'
const ADD_ERROR = 'We couldn’t add that question. Check the fields and try again.'
const DELETE_ERROR = 'We couldn’t delete that question. Check your connection and try again.'

export default function AdminQuestionsPage() {
  const [data, setData] = useState<QuestionsResponse | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)
  const [draft, setDraft] = useState(emptyDraft)
  const [saving, setSaving] = useState(false)

  function load() {
    setError(null)
    apiFetch<QuestionsResponse>('/api/admin/questions')
      .then(setData)
      .catch(() => setError(LOAD_ERROR))
  }

  useEffect(load, [])

  async function addQuestion() {
    if (!draft.questionKey || !draft.questionText) {
      setSuccess(null)
      setError('Question key and question text are required before you can add a question.')
      return
    }
    setSaving(true)
    setError(null)
    setSuccess(null)
    try {
      await apiPost('/api/admin/questions/add', draft)
      setDraft(emptyDraft)
      setSuccess(`“${draft.questionText}” was added to the question bank.`)
      load()
    } catch {
      setError(ADD_ERROR)
    } finally {
      setSaving(false)
    }
  }

  async function deleteQuestion(id: number, questionText: string) {
    setError(null)
    setSuccess(null)
    try {
      await apiPost('/api/admin/questions/delete', { questionId: id })
      setSuccess(`“${questionText}” was deleted.`)
      load()
    } catch {
      setError(DELETE_ERROR)
    }
  }

  return (
    <main className="mx-auto max-w-5xl px-6 py-10">
      <h1 className="text-2xl text-ink">Question management</h1>
      <AdminNav />

      {error && (
        <div className="mt-4">
          <StatusMessage
            tone="error"
            action={
              !data ? (
                <Button variant="secondary" onClick={load}>
                  Try again
                </Button>
              ) : undefined
            }
          >
            {error}
          </StatusMessage>
        </div>
      )}
      {success && (
        <div className="mt-4">
          <StatusMessage tone="success">{success}</StatusMessage>
        </div>
      )}

      <Card className="mt-4 p-6">
        <h2 className="text-lg text-ink">Add a question</h2>
        <div className="mt-4 grid gap-3 sm:grid-cols-2">
          <TextField
            label="Question key"
            value={draft.questionKey}
            onChange={(e) => setDraft({ ...draft, questionKey: e.target.value })}
          />
          <TextField
            label="Section"
            value={draft.sectionTitle}
            onChange={(e) => setDraft({ ...draft, sectionTitle: e.target.value })}
          />
          <div className="sm:col-span-2">
            <TextField
              label="Question text"
              value={draft.questionText}
              onChange={(e) => setDraft({ ...draft, questionText: e.target.value })}
            />
          </div>
          <label className="block text-sm">
            <span className="text-ink-soft">Type</span>
            <select
              value={draft.questionType}
              onChange={(e) => setDraft({ ...draft, questionType: e.target.value })}
              className="mt-1 w-full rounded-xl border border-teal-100 bg-white px-3 py-2 text-sm text-ink transition-shadow duration-200 focus:border-teal-500 focus:outline-none focus:ring-2 focus:ring-teal-300/40"
            >
              {(data?.questionTypes ?? ['yes_no', 'multiple_choice', 'text', 'number', 'choice']).map((t) => (
                <option key={t} value={t}>
                  {t}
                </option>
              ))}
            </select>
          </label>
        </div>
        <Button className="mt-4" onClick={addQuestion} loading={saving}>
          Add question
        </Button>
      </Card>

      {!data && !error && (
        <div aria-busy="true" className="mt-6 space-y-3">
          <span className="sr-only">Loading…</span>
          {Array.from({ length: 4 }).map((_, i) => (
            <Skeleton key={i} className="h-16" />
          ))}
        </div>
      )}

      <div className="mt-6 space-y-3">
        {data?.questions.map((question: AdminQuestion) => (
          <Card key={question.id} interactive className="flex items-center justify-between gap-4 p-4">
            <div>
              <p className="font-medium text-ink">{question.questionText}</p>
              <p className="text-sm text-ink-soft">
                {question.questionKey} &middot; {question.questionType} &middot; {question.sectionTitle}
              </p>
            </div>
            <Button variant="ghost" onClick={() => deleteQuestion(question.id, question.questionText)}>
              Delete
            </Button>
          </Card>
        ))}
      </div>
    </main>
  )
}
