// dist/index.html is vite-react-ssg's prerendered "/" page: its #root carries
// data-server-rendered="true" plus the Landing page's baked-in markup, and the client bundle
// hydrates against that on load. Every /app/** deep link is forwarded to that same index.html
// (see WebConfig) but renders a completely different route (dashboard, admin, ...) — hydrating
// mismatched markup against it trips a React error #418 warning on every load. This script
// strips the prerendered content and the data-server-rendered flag so /app/** gets an empty
// shell instead: the client bundle then does a plain client-side render() for those routes
// (see index.mjs's `isSSR` check) instead of a doomed-to-mismatch hydrate().
import { readFileSync, writeFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { dirname, join } from 'node:path'

const distDir = join(dirname(fileURLToPath(import.meta.url)), '..', 'dist')
const html = readFileSync(join(distDir, 'index.html'), 'utf-8')

const rootStart = html.indexOf('<div id="root"')
if (rootStart === -1) throw new Error('generate-app-shell: could not find #root in dist/index.html')
const openTagEnd = html.indexOf('>', rootStart) + 1

let depth = 1
let i = openTagEnd
while (depth > 0) {
  const nextOpen = html.indexOf('<div', i)
  const nextClose = html.indexOf('</div>', i)
  if (nextClose === -1) throw new Error('generate-app-shell: unbalanced #root div in dist/index.html')
  if (nextOpen !== -1 && nextOpen < nextClose) {
    depth++
    i = nextOpen + 4
  } else {
    depth--
    i = nextClose + 6
  }
}
const rootEnd = i // just past the matching </div>

const shellHtml = html.slice(0, rootStart) + '<div id="root"></div>' + html.slice(rootEnd)
writeFileSync(join(distDir, 'app-shell.html'), shellHtml)
console.log('[generate-app-shell] wrote dist/app-shell.html')
