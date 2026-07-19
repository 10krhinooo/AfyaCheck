import { useState } from 'react'
import { Button } from '../../components/Button'
import { StatusMessage } from '../../components/StatusMessage'
import { apiDelete, apiFetch } from '../../lib/api-client'
import { useTranslation } from '../../lib/i18n'
import { clearStoredResult } from './resultStorage'

export function DataControls({ sessionId }: { sessionId: string }) {
  const { t } = useTranslation()
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
      setError(err instanceof Error ? err.message : t('results.downloadError'))
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
      setError(err instanceof Error ? err.message : t('results.deleteError'))
    } finally {
      setDeleting(false)
    }
  }

  if (deleted) {
    return <StatusMessage tone="success">{t('results.deleted')}</StatusMessage>
  }

  return (
    <div>
      <h2 className="text-lg text-ink">{t('results.yourData')}</h2>
      <p className="mt-2 text-sm text-ink-soft">{t('results.yourDataHint')}</p>

      {error && (
        <div className="mt-3">
          <StatusMessage tone="error">{error}</StatusMessage>
        </div>
      )}

      <div className="mt-4 flex flex-col gap-3 sm:flex-row">
        <Button variant="secondary" onClick={downloadData} loading={downloading}>
          {t('results.download')}
        </Button>

        {confirmingDelete ? (
          <div className="flex items-center gap-3">
            <span className="text-sm text-ink-soft">{t('results.deleteConfirm')}</span>
            <Button
              variant="ghost"
              className="text-coral-700 hover:opacity-80"
              onClick={deleteData}
              loading={deleting}
            >
              {t('results.deleteYes')}
            </Button>
            <Button variant="ghost" onClick={() => setConfirmingDelete(false)} disabled={deleting}>
              {t('results.cancel')}
            </Button>
          </div>
        ) : (
          <Button variant="ghost" className="text-coral-700 hover:opacity-80" onClick={() => setConfirmingDelete(true)}>
            {t('results.delete')}
          </Button>
        )}
      </div>
    </div>
  )
}
