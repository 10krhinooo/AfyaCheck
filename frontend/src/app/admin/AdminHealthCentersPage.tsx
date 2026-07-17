import { useEffect, useState } from 'react'
import { AdminNav } from './AdminNav'
import { Button } from '../../components/Button'
import { Card } from '../../components/Card'
import { Skeleton } from '../../components/Skeleton'
import { StatusMessage } from '../../components/StatusMessage'
import { TextField } from '../../components/TextField'
import { apiFetch, apiPost } from '../../lib/api-client'
import type { AdminHealthCenter, HealthCentersResponse } from './types'

const emptyDraft = { name: '', address: '', latitude: '', longitude: '', phone: '', hours: '', stiTestingAvailable: false }
const LOAD_ERROR = 'We couldn’t load the health centers. Check your connection and try again.'
const ADD_ERROR = 'We couldn’t add that health center. Check the fields and try again.'
const DELETE_ERROR = 'We couldn’t delete that health center. Check your connection and try again.'

export default function AdminHealthCentersPage() {
  const [data, setData] = useState<HealthCentersResponse | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)
  const [draft, setDraft] = useState(emptyDraft)
  const [saving, setSaving] = useState(false)

  function load() {
    setError(null)
    apiFetch<HealthCentersResponse>('/api/admin/health-centers')
      .then(setData)
      .catch(() => setError(LOAD_ERROR))
  }

  useEffect(load, [])

  async function addHealthCenter() {
    const latitude = Number(draft.latitude)
    const longitude = Number(draft.longitude)
    if (!draft.name || !draft.latitude || !draft.longitude || Number.isNaN(latitude) || Number.isNaN(longitude)) {
      setSuccess(null)
      setError('Name, latitude, and longitude are required before you can add a health center.')
      return
    }
    setSaving(true)
    setError(null)
    setSuccess(null)
    try {
      await apiPost('/api/admin/health-centers/add', {
        name: draft.name,
        address: draft.address || null,
        latitude,
        longitude,
        phone: draft.phone || null,
        hours: draft.hours || null,
        stiTestingAvailable: draft.stiTestingAvailable,
      })
      setDraft(emptyDraft)
      setSuccess(`“${draft.name}” was added.`)
      load()
    } catch {
      setError(ADD_ERROR)
    } finally {
      setSaving(false)
    }
  }

  async function deleteHealthCenter(id: number, name: string) {
    setError(null)
    setSuccess(null)
    try {
      await apiPost('/api/admin/health-centers/delete', { healthCenterId: id })
      setSuccess(`“${name}” was deleted.`)
      load()
    } catch {
      setError(DELETE_ERROR)
    }
  }

  return (
    <main className="w-full px-6 py-10">
      <h1 className="text-2xl text-ink">Health center management</h1>
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
        <h2 className="text-lg text-ink">Add a health center</h2>
        <div className="mt-4 grid gap-3 sm:grid-cols-2">
          <TextField label="Name" value={draft.name} onChange={(e) => setDraft({ ...draft, name: e.target.value })} />
          <TextField label="Phone" value={draft.phone} onChange={(e) => setDraft({ ...draft, phone: e.target.value })} />
          <div className="sm:col-span-2">
            <TextField
              label="Address"
              value={draft.address}
              onChange={(e) => setDraft({ ...draft, address: e.target.value })}
            />
          </div>
          <TextField
            label="Latitude"
            type="number"
            value={draft.latitude}
            onChange={(e) => setDraft({ ...draft, latitude: e.target.value })}
          />
          <TextField
            label="Longitude"
            type="number"
            value={draft.longitude}
            onChange={(e) => setDraft({ ...draft, longitude: e.target.value })}
          />
          <TextField label="Hours" value={draft.hours} onChange={(e) => setDraft({ ...draft, hours: e.target.value })} />
          <label className="flex items-center gap-2 text-sm text-ink-soft">
            <input
              type="checkbox"
              checked={draft.stiTestingAvailable}
              onChange={(e) => setDraft({ ...draft, stiTestingAvailable: e.target.checked })}
              className="h-4 w-4 accent-teal-600"
            />
            STI testing available
          </label>
        </div>
        <Button className="mt-4" onClick={addHealthCenter} loading={saving}>
          Add health center
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
        {data?.healthCenters.map((center: AdminHealthCenter) => (
          <Card key={center.id} interactive className="flex items-center justify-between gap-4 p-4">
            <div>
              <p className="font-medium text-ink">
                {center.name}
                {!center.isActive && <span className="ml-2 text-xs text-ink-soft">(inactive)</span>}
              </p>
              <p className="text-sm text-ink-soft">
                {center.address ?? 'No address'} &middot; {center.latitude}, {center.longitude}
                {center.stiTestingAvailable ? ' · STI testing available' : ''}
              </p>
            </div>
            <Button variant="ghost" onClick={() => deleteHealthCenter(center.id, center.name)}>
              Delete
            </Button>
          </Card>
        ))}
      </div>
    </main>
  )
}
