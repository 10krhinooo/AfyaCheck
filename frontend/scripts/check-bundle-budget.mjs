#!/usr/bin/env node
// Fails the build if any dist/assets/*.js chunk exceeds its gzip budget from budget.json.
// Chunks are matched to a budget key by filename substring; unmatched chunks are reported
// but not enforced (add them to budget.json to gate new routes).
import { gzipSync } from 'node:zlib'
import { readFileSync, readdirSync } from 'node:fs'
import { join, dirname } from 'node:path'
import { fileURLToPath } from 'node:url'

const root = dirname(fileURLToPath(import.meta.url))
const distAssets = join(root, '..', 'dist', 'assets')
const budget = JSON.parse(readFileSync(join(root, '..', 'budget.json'), 'utf-8'))

let files
try {
  files = readdirSync(distAssets).filter((f) => f.endsWith('.js'))
} catch {
  console.error(`No dist/assets directory found at ${distAssets} — run "npm run build" first.`)
  process.exit(1)
}

let failed = false
for (const file of files) {
  const gzipKb = gzipSync(readFileSync(join(distAssets, file))).length / 1024
  const key = Object.keys(budget).find((k) => k !== '_comment' && file.includes(k))
  if (!key) {
    console.log(`  (no budget) ${file}: ${gzipKb.toFixed(1)} KB gzip`)
    continue
  }
  const limit = budget[key]
  const status = gzipKb > limit ? 'FAIL' : 'ok'
  console.log(`  [${status}] ${file}: ${gzipKb.toFixed(1)} KB gzip (budget: ${limit} KB, key: ${key})`)
  if (gzipKb > limit) failed = true
}

if (failed) {
  console.error('\nBundle budget exceeded.')
  process.exit(1)
}
console.log('\nAll chunks within budget.')
