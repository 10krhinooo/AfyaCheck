import { useEffect, useState } from 'react'
import { apiClient } from '../../api/client'

interface Question {
  id?: number
  questionKey: string
  questionText: string
  description?: string
  questionType: string
  options?: string[]
  sectionTitle?: string
  displayOrder?: number
  isActive: boolean
}

const emptyQuestion: Question = {
  questionKey: '',
  questionText: '',
  questionType: 'yes_no',
  isActive: true,
}

export function AdminQuestionsPage() {
  const [questions, setQuestions] = useState<Question[]>([])
  const [form, setForm] = useState<Question>(emptyQuestion)
  const [editingId, setEditingId] = useState<number | null>(null)

  function loadQuestions() {
    apiClient.get<{ questions: Question[] }>('/admin/questions').then((response) => setQuestions(response.data.questions))
  }

  useEffect(loadQuestions, [])

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault()
    if (editingId) {
      await apiClient.put(`/admin/questions/${editingId}`, form)
    } else {
      await apiClient.post('/admin/questions', form)
    }
    setForm(emptyQuestion)
    setEditingId(null)
    loadQuestions()
  }

  function startEdit(question: Question) {
    setForm(question)
    setEditingId(question.id ?? null)
  }

  async function deleteQuestion(id: number) {
    await apiClient.delete(`/admin/questions/${id}`)
    loadQuestions()
  }

  return (
    <div className="container py-4">
      <h1 className="h3 mb-4">Question Management</h1>

      <form className="card mb-4" onSubmit={handleSubmit}>
        <div className="card-body">
          <h5 className="card-title">{editingId ? 'Edit Question' : 'Add Question'}</h5>
          <div className="row g-3">
            <div className="col-md-4">
              <label className="form-label">Key</label>
              <input
                className="form-control"
                value={form.questionKey}
                onChange={(e) => setForm({ ...form, questionKey: e.target.value })}
                required
              />
            </div>
            <div className="col-md-4">
              <label className="form-label">Type</label>
              <select
                className="form-select"
                value={form.questionType}
                onChange={(e) => setForm({ ...form, questionType: e.target.value })}
              >
                <option value="yes_no">Yes/No</option>
                <option value="multiple_choice">Multiple Choice</option>
                <option value="choice">Single Choice</option>
                <option value="text">Text</option>
                <option value="number">Number</option>
              </select>
            </div>
            <div className="col-md-4">
              <label className="form-label">Section</label>
              <input
                className="form-control"
                value={form.sectionTitle ?? ''}
                onChange={(e) => setForm({ ...form, sectionTitle: e.target.value })}
              />
            </div>
            <div className="col-12">
              <label className="form-label">Question Text</label>
              <textarea
                className="form-control"
                value={form.questionText}
                onChange={(e) => setForm({ ...form, questionText: e.target.value })}
                required
              />
            </div>
          </div>
          <div className="mt-3 d-flex gap-2">
            <button type="submit" className="btn btn-success">
              {editingId ? 'Update' : 'Add'} Question
            </button>
            {editingId && (
              <button
                type="button"
                className="btn btn-outline-secondary"
                onClick={() => {
                  setForm(emptyQuestion)
                  setEditingId(null)
                }}
              >
                Cancel
              </button>
            )}
          </div>
        </div>
      </form>

      <table className="table">
        <thead>
          <tr>
            <th>Key</th>
            <th>Text</th>
            <th>Type</th>
            <th>Section</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {questions.map((question) => (
            <tr key={question.id}>
              <td>{question.questionKey}</td>
              <td>{question.questionText}</td>
              <td>{question.questionType}</td>
              <td>{question.sectionTitle}</td>
              <td className="d-flex gap-2">
                <button className="btn btn-sm btn-outline-secondary" onClick={() => startEdit(question)}>
                  Edit
                </button>
                <button className="btn btn-sm btn-outline-danger" onClick={() => question.id && deleteQuestion(question.id)}>
                  Delete
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
