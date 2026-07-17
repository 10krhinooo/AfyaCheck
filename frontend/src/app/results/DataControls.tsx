import { useState } from 'react'
import { Button } from '../../components/Button'
import { StatusMessage } from '../../components/StatusMessage'
import { apiDelete, apiFetch } from '../../lib/api-client'
import { clearStoredResult } from './resultStorage'

export function DataControls({ sessionId }: { sessionId: string }) {
  const [confirmingDelete, setConfirmingDelete] = useState(false)
  const [deleting, setDeleting] = useState(false)
  const [deleted, setDeleted] = useState(false)
  const [downloading, setDownloading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function downloadData() {
    setDownloading(true)
    setError(null)
    try {
      const data = await apiFetch(`/api/results/export?sessionId=${encodeURIComponent(sessionId)}`)
      const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' })
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = 'afyacheck-my-data.json'
      link.click()
      URL.revokeObjectURL(url)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'We couldn’t download your data. Please try again.')
    } finally {
      setDownloading(false)
    }
  }

  async function deleteData() {
    setDeleting(true)
    setError(null)
    try {
      await apiDelete(`/api/results?sessionId=${encodeURIComponent(sessionId)}`)
      clearStoredResult(sessionId)
      setDeleted(true)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'We couldn’t delete your data. Please try again.')
    } finally {
      setDeleting(false)
    }
  }

  if (deleted) {
    return <StatusMessage tone="success">Your data has been deleted.</StatusMessage>
  }

  return (
    <div>
      <h2 className="text-lg text-ink">Your data</h2>
      <p className="mt-2 text-sm text-ink-soft">
        Download a copy of the answers and results tied to this assessment, or delete them
        permanently.
      </p>

      {error && (
        <div className="mt-3">
          <StatusMessage tone="error">{error}</StatusMessage>
        </div>
      )}

      <div className="mt-4 flex flex-col gap-3 sm:flex-row">
        <Button variant="secondary" onClick={downloadData} loading={downloading}>
          Download my data
        </Button>

        {confirmingDelete ? (
          <div className="flex items-center gap-3">
            <span className="text-sm text-ink-soft">Delete permanently?</span>
            <Button
              variant="ghost"
              className="text-coral-700 hover:opacity-80"
              onClick={deleteData}
              loading={deleting}
            >
              Yes, delete
            </Button>
            <Button variant="ghost" onClick={() => setConfirmingDelete(false)} disabled={deleting}>
              Cancel
            </Button>
          </div>
        ) : (
          <Button variant="ghost" className="text-coral-700 hover:opacity-80" onClick={() => setConfirmingDelete(true)}>
            Delete my data
          </Button>
        )}
      </div>
    </div>
  )
}
