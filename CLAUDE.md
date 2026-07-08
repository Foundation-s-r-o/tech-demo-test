# Development Guidelines

## Stack & Ground Rules

Stack: **Spring Boot 4 / Java 21 LTS / embedded H2 file-mode / Spring Security session login /
OpenPDF**, with a **React 19 + TypeScript (strict)** SPA. The pinned-version table lives in
[`README.md`](README.md#stack-snapshot); architecture, layout, and data flows in
[`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) — read those rather than restating them here.

**JDK:** `cd api && sdk env` picks up Java 21 from `api/.sdkmanrc`. Leave the global SDKMAN
default alone — the workspace has Java 8/11 projects.

**Fast iteration is paramount.** Keep the local inner loop fast and **flag anything that slows
local development** (Docker requirements, slow tests, heavyweight infra). Speed comes from
removing infra, not from loosening the build gate.

**Build gate is strict:** Checkstyle `failOnViolation=true`; integration tests + JaCoCo on
`verify`. With few tests, the gate is the main safety net — verify before each commit; do not
relax it for speed.

**Non-negotiable conventions:** Flyway owns the schema in **portable ANSI SQL** — datasource and
Hibernate dialect are **profile-driven** (never hardcode `H2Dialect` or vendor-specific
SQL/types). Security is configured completely and **in the correct layer** (CORS in the security
filter chain, ahead of authorization — not the MVC layer). The known **`admin`/`admin`** account
exists only under the `local` / `it` profiles ([`SECURITY.md`](SECURITY.md)).

## Project Overview

Full-stack Spring Boot + React/TypeScript demo that serves as Foundation's reference template
for new projects. Its business surface is a **civil registry** — a person aggregate with
bitemporal detail/address/religion, append-only vital records (births/marriages/deaths), a GDPR
workflow, a tamper-evident SHA-256 audit hash chain, and six-role session auth — ported from
`db/DESIGN.md` (see [ADR-0011](docs/ADR/0011-civil-registry-schema-port.md)). PostgreSQL-only
mechanisms (RLS, triggers, exclusion constraints) are enforced in the application layer; the
`-- PG-delta:` markers in `V5` and [`db/DESIGN.md` §12](db/DESIGN.md) are the re-hardening path.

**Architecture, repository layout, HTTP surface, and data flows are documented in
[`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md)** — read it before structural changes instead of
duplicating it here. In short: both backend and frontend are organized **by feature**
(`registry`, `records`, `gdpr`, `auditlog`, `codelist`, `auth` on the backend; matching `pages/`
on the frontend) over a shared `infrastructure` / `common` layer. Load-bearing patterns: clean
layering (controller → API service → domain services + JPA repositories), constructor injection
via Lombok `@RequiredArgsConstructor` with `final` fields, and a type-safe frontend client
generated from the OpenAPI spec (`npm run generate-openapi-services`).

Local run, commands, and the quality/security toolchain live in [`README.md`](README.md) and
[`SECURITY.md`](SECURITY.md); the per-change verification recipe is below.

## Essential Commands

### Backend (Java/Spring Boot)
- Build: `./mvnw clean install`
- Run tests: `./mvnw test`
- Run single test: `./mvnw -Dtest=TestClass#testMethod test`
- Run integration tests: `./mvnw verify`
- Debug: `./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"`
- Generate code coverage: `./mvnw jacoco:report`
- Check code style: `./mvnw checkstyle:check`
- Start local application: `./mvnw spring-boot:run -Dspring-boot.run.profiles=local`

### Frontend (React/TypeScript)
- Install: `npm install --legacy-peer-deps`
- Start dev server: `npm start`
- Build: `npm run build`
- Lint: `npm run lint`
- Run E2E tests: `npm run playwright` (Playwright auto-starts **both** app servers via its
  `webServer` config — API on `:8082`, webpack dev server on `:8080`; existing servers are
  never reused. Auth runs as a normal test flow against the managed servers. Test files match
  `test_*.spec.ts`.)
- Run specific test: `npx playwright test tests/path/to/test.spec.ts`
- Generate API client: `npm run generate-openapi-services`

### Project Health & Quality
- **Comprehensive health check / single harness entry point**: `./doctor` — UI build + lint,
  `mvnw clean verify` (incl. the JaCoCo coverage gate), Playwright smoke + Storybook smoke +
  **full E2E suite**, OpenAPI client-drift check, security scans (Semgrep/Trivy/npm audit),
  SonarQube (skipped with a warning when `SONAR_TOKEN`/host is unavailable), and FOSSA (policy
  gate advisory). Slow (starts both app servers for E2E); requires ports 8080/8082 free.
  Test-harness strategy: [`docs/ADR/0010`](docs/ADR/0010-test-harness-playwright-e2e.md).
- **License scanning**: `fossa analyze`
- **Security testing**: `fossa test`

## Verifying Code Changes

Choose verification depth by **total net lines changed** (added + removed across all files — the
`git diff --stat` total).

### Small change — ≤ 50 net lines
ESLint is sufficient for frontend-only changes:
```bash
cd ui && npm run lint          # tsc --noEmit + eslint src/ — both must pass
```
For backend-only small changes, run `cd api && ./mvnw checkstyle:check test`.
Fix every error. Warnings are usually fine — **flag any you leave behind** in your summary.

### Big change — > 50 net lines
Run the full suite and **fix all errors** before reporting done. Warnings are generally
acceptable but must be flagged (call out anything new or security-relevant). All scan artifacts
go under `docs/`.

```bash
# 1. Frontend lint + type-check (gate)
cd ui && npm run lint

# 2. Backend: style + unit tests + integration tests + coverage
cd api && ./mvnw clean verify          # Checkstyle (validate phase), all unit + integration tests, JaCoCo.
#   ITs run on in-memory H2 only — NO Docker / Testcontainers. If anything tries to start a
#   Docker container, that's a regression — flag it.
#   jacoco:check gates merged unit+IT line coverage (floor: jacoco.line-coverage.minimum
#   in api/pom.xml) — if it trips, add tests; don't lower the floor.

# 3. Playwright smoke tests (see below) — run after major changes
cd ui && npm run smoke                 # app smoke (auto-starts dev server :8090)
cd ui && npm run smoke:storybook       # Storybook smoke (auto-starts :6007)

# 3b. Playwright full E2E suite — run after major changes (and always for auth/API changes).
#     Auto-starts BOTH servers (API :8082 with profiles local,e2e on in-mem H2; UI :8080);
#     ports must be free (reuseExistingServer: false). Covers login/failed login/logout,
#     main page, persons list/create/edit, client-side validation, the ROLE_USER
#     authorization matrix, axe-core accessibility (serious/critical gate, run in BOTH
#     Nord color modes + theme-toggle persistence), and the
#     live OpenAPI spec vs committed ui/api-docs.json.
cd ui && npx playwright test           # or: npm run playwright

# 4. Trivy — filesystem vuln scan, run in repo ROOT. Reports to docs/.
#    NOTE: Trivy pulls its vuln DB from a public OCI mirror via the Docker config.
#    If Docker Desktop is absent the `docker-credential-desktop` helper is missing
#    and the DB download FATAL-errors — but `--output` leaves the PREVIOUS report
#    in place, so a stale file looks like a fresh pass. Neutralise the Docker
#    config first so Trivy does an anonymous pull (security_scan.sh does this too):
export DOCKER_CONFIG=$(mktemp -d)
trivy fs . --scanners vuln --format table --output docs/trivy-report.txt
trivy fs . --scanners vuln --format json  --output docs/trivy-report.json

# 5. SonarQube — static analysis. Host: 10.16.35.113:9000 (needs SONAR_TOKEN in env).
# Use -Dsonar.token (not -Dsonar.login — removed in SonarQube 25+).
cd api && sonar-scanner -Dsonar.host.url=http://10.16.35.113:9000/ -Dsonar.token=$SONAR_TOKEN
cd ui  && sonar-scanner -Dsonar.host.url=http://10.16.35.113:9000/ -Dsonar.token=$SONAR_TOKEN
# Pull issues + gate metrics into docs/ for review (project keys: tech-demo-api, tech-demo-ui):
curl -s -u "$SONAR_TOKEN:" "http://10.16.35.113:9000/api/issues/search?componentKeys=tech-demo-api" | jq '.' > docs/sonar-issues-tech-demo-api.json
curl -s -u "$SONAR_TOKEN:" "http://10.16.35.113:9000/api/measures/component?component=tech-demo-api&metricKeys=bugs,vulnerabilities,security_hotspots,code_smells,coverage" | jq '.' > docs/sonar-metrics-tech-demo-api.json
```

**Then read the outputs and act on them:**
- ESLint / tsc / Checkstyle: errors → fix; warnings → flag.
- `docs/trivy-report.txt`: CRITICAL/HIGH CVEs → fix (minimal safe bump; see dependency
  constraints in `../CLAUDE.md`); MEDIUM/LOW → flag. CVEs with no released fix → flag, don't churn.
- Sonar: bugs and vulnerabilities → fix; CRITICAL code smells → fix; lesser smells / hotspots →
  flag and triage. Dashboards: `http://10.16.35.113:9000/dashboard?id=tech-demo-api` (and `...-ui`).
- Smoke tests: any failure → fix the underlying error, not the test.

Escalate to the full suite regardless of line count when touching: `ui/src/common/auth/*`,
`ui/src/common/axiosClient.ts`, `ui/src/api/*`, or backend `auth/` / `infrastructure/`.

### Playwright smoke tests

Lightweight, **backend-free** tests that catch load/render/runtime-console errors. Run after
major changes.

- **App smoke** (`ui/tests/smoke.spec.ts`, config `playwright.smoke.config.ts`): auto-starts the
  webpack dev server on a dedicated port (`:8090`, no overlay), loads the login shell, asserts
  `#login_username/#login_password/#login_submit` render, types into fields, and **fails on any
  browser console error / page error**. Backend-free — unlike the full E2E suite, smoke does not
  start the API.
- **Storybook smoke** (`ui/tests/smoke.storybook.spec.ts`, config `playwright.storybook.config.ts`):
  auto-starts Storybook on `:6007` (dedicated port — not the interactive `:6006`) and renders the
  `Button` and `Form` stories, failing on console errors.

```bash
cd ui
npm run smoke              # app smoke
npm run smoke:storybook    # storybook smoke
npx playwright install chromium   # one-time, if the browser binary is missing
```

**Reading browser console output:** `ui/tests/console-capture.ts` records all `console`,
`pageerror`, and `requestfailed` events (filtering out dev-server/HMR noise) and writes them to
`docs/smoke-app.log` and `docs/smoke-storybook-*.log` after each run — read these to spot errors.
Console errors and page errors fail the test; warnings are logged but do not fail. The Playwright
HTML report (with trace + screenshots) is at `ui/playwright-report-smoke/` and
`ui/playwright-report-storybook/`; the full E2E report is at `ui/playwright-report/`.

**Screenshots:** all Playwright configs set `screenshot: 'on'` — every test, **passed and
failed**, saves a final-state screenshot and attaches it to the HTML report. Each suite has its
own `outputDir` so runs never wipe each other's images: `ui/test-results/e2e/<test-name>/`
(full E2E), `ui/test-results/smoke/…`, `ui/test-results/storybook/…`. Review them directly on
disk or via the HTML reports. The `*.log` files, `test-results/`, and report dirs are gitignored
(regenerated each run).

### Playwright E2E test conventions

- Specs match `test_*.spec.ts` in `ui/tests/`; shared login flow lives in
  `ui/tests/helpers/auth.ts` — use `login(page)` (defaults to admin; pass a `Credentials`
  object for the seeded `user/user` ROLE_USER account) instead of re-typing the form steps.
- Tests are self-contained: create data through the UI as the `local`-profile admin; the `e2e`
  profile runs on a fresh in-memory H2, so no cleanup/seed fixtures.
- Use stable `id` selectors (`#login_*`, `#PersonEditForm_*`, `#app_header_*`,
  `#error_dialog_close`) — never text or Bootstrap utility classes.
- **OpenAPI contract**: after changing controllers/DTOs, refresh the contract — start the API
  (`local` profile), save `http://localhost:8082/v3/api-docs` over `ui/api-docs.json`, run
  `npm run generate-openapi-services`, and commit both. `test_openapi.spec.ts` (E2E) and the
  doctor drift step fail otherwise. springdoc emits OpenAPI 3.0 (`springdoc.api-docs.version`)
  because the pinned generator CLI 6.0.1 cannot parse 3.1 — bump both together.

## Code Style Guidelines

### Java
- Indentation: tabs (4 spaces width), max line length 120
- Imports: avoid star imports, no unused imports
- Tests: JUnit 5 with given/when/then pattern
- DI: constructor injection with `@RequiredArgsConstructor` and final fields
- SQL: keywords lowercase, explicit column names
- Exceptions: specialized exceptions handled in `ApiExceptionHandler`
- Lombok: use annotations to reduce boilerplate
- JPA: entities extend `IdentifiableEntity<>`, use proper column mappings

### TypeScript/React
- Paths: use aliases (@components, @api, @pages, @common)
- Syntax: single quotes, no semicolons
- Components: functional with hooks, avoid class components
- Types: strong typing with interfaces, avoid `any` type
- Naming: camelCase for variables/methods, PascalCase for types/components
- Props: explicit interfaces with proper naming (ComponentProps)
- Styling: React Bootstrap + SCSS with BEM. **Never hardcode colors in component SCSS** — use
  the Nord theme-token layer (`--fndt-*` CSS custom properties); both light and dark
  (`data-bs-theme`) must render correctly. Icons via the `FndtIcon` wrapper (Heroicons), no icon font
- ESLint: `@typescript-eslint/no-unsafe-argument` enforced as an error

## CI / Workflows

Active workflows (`.github/workflows/`):

| File | Trigger | What it does |
| ---- | ------- | ------------ |
| `fossa.yml` | push/PR on `main` | FOSSA license + vuln scan (scan-only, no test gate) |

GitHub-managed (not in `.github/workflows/`):
- **CodeQL default setup** — runs on push/PR/weekly across `java-kotlin`,
  `javascript-typescript`, `actions`. This is the real CodeQL coverage; do not add a custom
  `code-scan.yml`.
- **Dependabot Updates** — `.github/dependabot.yml` schedules weekly Maven (`/api`) and npm
  (`/ui`) bumps.

**CircleCI** runs the actual build gate: backend `./mvnw verify` + frontend audit, lint, and build. Key
notes in `.circleci/config.yml`:
- `cimg/openjdk:21.0` (matches project Java 21 LTS).
- `override-ci-command: npm ci --legacy-peer-deps --no-audit --no-fund`. `npm ci` works because
  the lockfile carries full entries (resolved + integrity) for **all** `@parcel/watcher` platform
  variants, not just the dev host's `darwin-arm64`. If `npm ci` ever fails on Linux with a missing
  optional-binary error, the lockfile has regressed — regenerate it against the public registry
  rather than switching CI to `npm install`.

All workflows use least-privilege `permissions: contents: read`.

## Dependency / lockfile constraints

1. **`ui/package-lock.json` must stay on `registry.npmjs.org/`.** No `tfs-app1:8080` URLs in
   `resolved` fields — they break Dependabot npm runs (it can't reach the internal feed). Sanity
   check: `grep -c 'tfs-app1' ui/package-lock.json` → must return `0`.
2. **`--legacy-peer-deps` is required** at every npm install: `storybook-addon-react-router-v6`
   pulls `react-inspector@6.0.2`, which lists React 16-18 in peers and conflicts with the React 19
   tree.
3. **Cross-platform `@parcel/watcher` lockfile gotcha.** `sass → @parcel/watcher` ships a
   different native binary per OS/arch. The lockfile must keep full entries for **all** platform
   variants so CircleCI's `npm ci` works on Linux; when regenerating the lockfile, verify with
   `grep -c 'watcher-linux-x64-glibc' ui/package-lock.json` (must be ≥ 1).
4. **Public-only npm registry locally.** If your global `~/.npmrc` points at the internal TFS
   feed, pass `--registry=https://registry.npmjs.org/` explicitly when regenerating the lockfile.
   Don't edit `.npmrc` (other workspace projects need it).

## Security scans

Run `./scripts/security_scan.sh`. The script exports `SEMGREP_SEND_METRICS=off` and passes both
`--metrics off` and `--no-trace` before invoking Semgrep. It also runs Trivy plus the npm audit
gate. Do not run repository Semgrep checks through an unwrapped command that restores telemetry.

## Security
- No hardcoded credentials or secrets in code.
- Use environment variables for sensitive data.
- Follow OWASP secure coding practices.
- Run FOSSA scans to check dependencies.
- **Vulnerability reporting + demo-mode caveats + supported-versions policy live in
  [`SECURITY.md`](SECURITY.md).** Update SECURITY.md whenever a demo-mode caveat changes (CSRF
  re-enabled, CORS tightened, H2 console removed from `PUBLIC_PATHS`, etc.).

## Agent Autonomy
Claude Code may navigate directories, run builds/tests, install dependencies, run linting and
FOSSA tools, read file contents, and perform system operations in this project without asking for
permission each time:
- Running Maven and npm builds/tests
- Navigating directory structures
- Installing dependencies
- Running linting and formatting tools and FOSSA tools
- Reading file contents (`cat`, `head`, `tail`, etc.)
- Running the `./doctor` health check script
