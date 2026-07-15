#!/usr/bin/env node
// One-time bulk import of AfyaCheck's existing `users` table into Keycloak, via the Admin
// REST API — per the migration plan (floofy-honking-storm.md, "User migration"). Run once
// per environment during the Phase 7 cutover, NOT on every deploy.
//
// Deliberately does NOT attempt to carry over bcrypt password hashes: every imported user is
// created with `requiredActions: ["UPDATE_PASSWORD"]`, so they set a fresh password (or use
// "Forgot password") on first Keycloak-hosted login. This avoids taking a permanent dependency
// on Keycloak's bcrypt-compatible hash provider just to preserve credentials nobody re-enters
// after the very first login anyway.
//
// After a user's row is imported, this script does NOT delete it from the local `users`
// table — see KeycloakUserSyncFilter.java, which re-adopts/updates that same row (by email,
// since keycloak_id is null pre-migration) the first time that person authenticates via
// Keycloak, backfilling `keycloak_id`. FK integrity (questionnaire.user_id, etc.) is
// preserved throughout since the row's `id` never changes.
//
// Usage:
//   DB_URL=postgresql://postgres:postgres@localhost:5432/AfyaCheck \
//   KEYCLOAK_URL=http://localhost:8180 \
//   KEYCLOAK_REALM=afyacheck \
//   KEYCLOAK_ADMIN_USER=admin \
//   KEYCLOAK_ADMIN_PASSWORD=admin \
//   node keycloak/scripts/migrate-users.mjs [--dry-run]

import pg from 'pg'

const {
  DB_URL = 'postgresql://postgres:postgres@localhost:5432/AfyaCheck',
  KEYCLOAK_URL = 'http://localhost:8180',
  KEYCLOAK_REALM = 'afyacheck',
  KEYCLOAK_ADMIN_USER = 'admin',
  KEYCLOAK_ADMIN_PASSWORD = 'admin',
} = process.env

const dryRun = process.argv.includes('--dry-run')

async function getAdminToken() {
  const response = await fetch(`${KEYCLOAK_URL}/realms/master/protocol/openid-connect/token`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({
      grant_type: 'password',
      client_id: 'admin-cli',
      username: KEYCLOAK_ADMIN_USER,
      password: KEYCLOAK_ADMIN_PASSWORD,
    }),
  })
  if (!response.ok) throw new Error(`Failed to authenticate to Keycloak admin API: ${response.status}`)
  return (await response.json()).access_token
}

async function createKeycloakUser(token, row) {
  const body = {
    username: row.email,
    email: row.email,
    firstName: row.name,
    enabled: row.enabled ?? true,
    emailVerified: row.email_verified ?? false,
    requiredActions: ['UPDATE_PASSWORD'],
    attributes: { legacyUserId: [String(row.id)] },
  }

  if (dryRun) {
    console.log(`[dry-run] would create ${row.email} (role=${row.role})`)
    return
  }

  const createResponse = await fetch(`${KEYCLOAK_URL}/admin/realms/${KEYCLOAK_REALM}/users`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })

  if (createResponse.status === 409) {
    console.log(`skip ${row.email}: already exists in Keycloak`)
    return
  }
  if (!createResponse.ok) {
    throw new Error(`Failed to create ${row.email}: ${createResponse.status} ${await createResponse.text()}`)
  }

  const location = createResponse.headers.get('Location')
  const keycloakUserId = location?.split('/').pop()

  if (row.role === 'ADMIN' && keycloakUserId) {
    await assignRealmRole(token, keycloakUserId, 'ADMIN')
  }

  console.log(`created ${row.email} (keycloak id ${keycloakUserId})`)
}

async function assignRealmRole(token, keycloakUserId, roleName) {
  const roleResponse = await fetch(`${KEYCLOAK_URL}/admin/realms/${KEYCLOAK_REALM}/roles/${roleName}`, {
    headers: { Authorization: `Bearer ${token}` },
  })
  if (!roleResponse.ok) throw new Error(`Realm role ${roleName} not found — check realm-export.json was imported`)
  const role = await roleResponse.json()

  await fetch(`${KEYCLOAK_URL}/admin/realms/${KEYCLOAK_REALM}/users/${keycloakUserId}/role-mappings/realm`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
    body: JSON.stringify([role]),
  })
}

async function main() {
  const client = new pg.Client({ connectionString: DB_URL })
  await client.connect()
  const { rows } = await client.query(
    'SELECT id, email, name, role, enabled, email_verified FROM users WHERE provider = $1',
    ['LOCAL'],
  )
  await client.end()

  console.log(`Found ${rows.length} LOCAL-provider users to migrate.`)
  const token = dryRun ? null : await getAdminToken()

  for (const row of rows) {
    await createKeycloakUser(token, row)
  }

  console.log('Done.')
}

main().catch((err) => {
  console.error(err)
  process.exit(1)
})
