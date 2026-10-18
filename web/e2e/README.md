# Browser integration tests

Run from the repository root:

```sh
npm --prefix web ci
npx --prefix web playwright install --with-deps chromium firefox
npm --prefix web run test:e2e:integration
```

The command builds and starts an isolated local Docker Compose project with the
production backend/web Dockerfiles, PostgreSQL and Mailpit. It waits for health
checks, runs Playwright and removes only that project's containers and volumes
on exit. It does not use the developer's `.env`, database or Google credentials.
SMTP stays inside Docker. A remote Docker daemon is refused, and global setup
rejects non-loopback or HTTPS test URLs before making any request.

Default local ports are 18088 (web), 18080 (backend), 15432 (PostgreSQL) and
18025 (Mailpit). Override them with `E2E_WEB_PORT`, `E2E_BACKEND_PORT`,
`E2E_DB_PORT` and `E2E_MAIL_PORT` if needed.

The 35 scenarios run on Chromium desktop, Firefox desktop and Chromium with a
393×852 mobile viewport (105 executions). Every API response comes from the real
backend. API calls provision independent accounts and graphs, and verify the
state actually persisted by browser actions; no route returns simulated JSON.
The connection-loss scenario temporarily takes its browser context offline.

Coverage includes:

- First registration, SMTP email verification and invalid codes, approval and
  rejection, duplicate accounts, login failures, recovery and single-use codes.
- Empty workspaces; creation, renaming, activation and deletion of plans; six meal
  types; notes, time, units, notifications; removal of courses and cascading data.
- Shopping list isolation/deletion, custom ingredients, fresh daily portions,
  purchase versus usage day, categories, bought/unbought state and reminders.
- Separate accounts and browser sessions, revision conflicts, retained drafts,
  reconnection, logout and revoked sessions, HttpOnly cookies and CSRF rejection.
- Real admin search, suspension/reactivation, permissions, audit history, code-owned
  release metadata and rejection of removed administration endpoints.
- Profile and password changes, language, appearance persistence, responsive
  layouts, themed dropdowns, keyboard navigation and the actual disabled Google/Drive configuration.

Real Google sign-in and Drive operations require external services and are not
exercised by this local suite.

For a focused run, forward Playwright options:

```sh
npm --prefix web run test:e2e:integration -- --project=chromium-desktop --grep 'shopping'
```

Reports: `web/playwright-report/html/index.html`, JUnit/JSON in
`web/playwright-report/`, failure screenshots/traces and `stack.log` in
`web/test-results/`. The PR workflow runs the same isolated command and uploads
these artifacts even on failure. Direct `npm --prefix web run test:e2e` requires
an already-running **local, disposable** stack with `WEB_TEST_URL` and
`MAILPIT_TEST_URL` set; prefer the isolated command above.
