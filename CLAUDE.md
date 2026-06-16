# Development Guidelines

## Stack & Ground Rules

Current stack: **Spring Boot 4.0.6 / Java 21 LTS / embedded H2 (file-mode) / Spring Security
session login / OpenPDF**, with a **React 19.2 + TypeScript 6.0 (strict)** SPA.

**JDK:** Java 21 LTS, pinned in `api/.sdkmanrc` (`sdk env`). The global SDKMAN default is left
alone (the workspace has Java 8/11 projects).

**Fast iteration is paramount.** Keep the local inner loop fast and **flag anything that slows
local development** (Docker requirements, slow tests, heavyweight infra). Speed comes from
removing infra, not from loosening the build gate.

**Build gate is strict:** Checkstyle `failOnViolation=true`; integration tests + JaCoCo on
`verify`. With few tests, the gate is the main safety net — verify before each commit; do not
relax it for speed.

**Stack details:**
- **Spring Boot 4.0.6** — stay on the 4.0 line.
- **Java 21 LTS** (`api/.sdkmanrc`).
- **DB: embedded H2 file-mode** (`./data/`, gitignored) — no Docker. Flyway migrations in
  **portable ANSI SQL**; datasource + Hibernate dialect are **profile-driven** (never hardcode
  `H2Dialect` in shared config; avoid vendor-specific SQL/types).
- **Auth: Spring Security session login**, users in DB, BCrypt, CSRF cookie/header flow,
  exact-origin CORS, and session-ID rotation. The known **`admin` / `admin`** account is created
  only by explicit `local` and integration-test profiles; see [`SECURITY.md`](SECURITY.md).
- **PDF: OpenPDF** (`com.github.librepdf:openpdf`) — `GET /api/persons/{id}/pdf`.
- **JSON: Jackson 3** (`tools.jackson`, via `spring-boot-starter-json`).
- **Observability:** Spring Boot Actuator + Micrometer.

## Project Overview

Full-stack Java/Spring Boot + React/TypeScript demo application, serving as a template for new
projects.

### Architecture

**Backend (Java/Spring Boot)**
- Entry point: `TechDemoApplication.java`
- Spring Boot 4.0.6 on Java 21 LTS
- Embedded H2 (file-mode) with JPA/Hibernate 7, Flyway migrations
- Features: Person CRUD (`persons/`), Spring Security session auth (`auth/`), infrastructure
  utilities (`infrastructure/`)

**Frontend (React/TypeScript)**
- Entry point: `index.tsx`
- React 19.2, **TypeScript 6.0 (strict mode)**, React Router v7
- Bootstrap 5.3 with SCSS
- i18n: i18next 26 + react-i18next 17 (Slovak translations in `assets/translations/sk.json`)
- Features: auth context + protected routes, person management pages, reusable UI components,
  Formik + Yup forms

### Key Architectural Patterns
- Clean architecture with separate domain/API layers
- Constructor injection with Lombok `@RequiredArgsConstructor` and final fields
- Repository pattern with JPA
- React functional components with hooks
- Type-safe API client generated from OpenAPI specs

### Build & Packaging
- Backend: Maven (Checkstyle, JaCoCo, Failsafe)
- Frontend: Webpack with TypeScript, ESLint, Playwright

### Local development
Two terminals: `./mvnw spring-boot:run -Dspring-boot.run.profiles=local` (API on `:8082`) +
`npm start` (UI on `:8080`). The `local` profile is required for the test-only `admin/admin`
account. Docker and cloud-deployment scaffolding are intentionally absent.

### Security & Quality tooling
- FOSSA license + vuln scan (`.github/workflows/fossa.yml`, scan-only — see `SECURITY.md`).
- GitHub CodeQL default setup (`java-kotlin`, `javascript-typescript`, `actions`).
- GitGuardian secret scanning on PRs; GitHub secret scanning + push protection enabled.
- Dependabot weekly Maven + npm.
- Checkstyle on `./mvnw validate` (hard gate, `failOnViolation=true`).
- Bouncy Castle for cryptography; explicit Tomcat / okio pins for CVE fixes.

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
- **Comprehensive health check**: `./doctor` — build, lint, tests, and FOSSA scans
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
cd api && ./mvnw clean verify          # Checkstyle (validate phase), 7 unit + 19 IT, JaCoCo.
#   ITs run on in-memory H2 only — NO Docker / Testcontainers. If anything tries to start a
#   Docker container, that's a regression — flag it.

# 3. Playwright smoke tests (see below) — run after major changes
cd ui && npm run smoke                 # app smoke (auto-starts dev server :8090)
cd ui && npm run smoke:storybook       # Storybook smoke (auto-starts :6006)

# 4. Trivy — filesystem vuln scan, run in repo ROOT. Reports to docs/.
#    NOTE: Trivy pulls its vuln DB from a public OCI mirror via the Docker config.
#    If Docker Desktop is absent the `docker-credential-desktop` helper is missing
#    and the DB download FATAL-errors — but `--output` leaves the PREVIOUS report
#    in place, so a stale file looks like a fresh pass. Neutralise the Docker
#    config first so Trivy does an anonymous pull (security_scan.sh does this too):
export DOCKER_CONFIG=$(mktemp -d)
trivy fs . --scanners vuln --format table --output docs/trivy-report.txt
trivy fs . --scanners vuln --format json  --output docs/trivy-report.json

# 5. SonarQube — static analysis. Host: 10.16.35.93:9000 (needs SONAR_TOKEN in env).
# Use -Dsonar.token (not -Dsonar.login — removed in SonarQube 25+).
cd api && sonar-scanner -Dsonar.host.url=http://10.16.35.93:9000/ -Dsonar.token=$SONAR_TOKEN
cd ui  && sonar-scanner -Dsonar.host.url=http://10.16.35.93:9000/ -Dsonar.token=$SONAR_TOKEN
# Pull issues + gate metrics into docs/ for review (project keys: tech-demo-api, tech-demo-ui):
curl -s -u "$SONAR_TOKEN:" "http://10.16.35.93:9000/api/issues/search?componentKeys=tech-demo-api" | jq '.' > docs/sonar-issues-tech-demo-api.json
curl -s -u "$SONAR_TOKEN:" "http://10.16.35.93:9000/api/measures/component?component=tech-demo-api&metricKeys=bugs,vulnerabilities,security_hotspots,code_smells,coverage" | jq '.' > docs/sonar-metrics-tech-demo-api.json
```

**Then read the outputs and act on them:**
- ESLint / tsc / Checkstyle: errors → fix; warnings → flag.
- `docs/trivy-report.txt`: CRITICAL/HIGH CVEs → fix (minimal safe bump; see dependency
  constraints in `../CLAUDE.md`); MEDIUM/LOW → flag. CVEs with no released fix → flag, don't churn.
- Sonar: bugs and vulnerabilities → fix; CRITICAL code smells → fix; lesser smells / hotspots →
  flag and triage. Dashboards: `http://10.16.35.93:9000/dashboard?id=tech-demo-api` (and `...-ui`).
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
  auto-starts Storybook on `:6006` and renders the `Button` and `Form` stories, failing on
  console errors.

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
HTML report (with trace + failure screenshot) is at `ui/playwright-report-smoke/` and
`ui/playwright-report-storybook/`. The smoke `*.log` files and report dirs are gitignored
(regenerated each run).

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
- Styling: React Bootstrap with SCSS using BEM convention
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
- `override-ci-command: npm install --legacy-peer-deps --no-audit --no-fund` — NOT `npm ci`. The
  lockfile only pins the dev-host's `@parcel/watcher-darwin-arm64` binary; the other platform
  variants lack full lockfile entries, so strict `npm ci` fails on Linux. `npm install` honors
  the lockfile when accurate and fills the missing optional binaries at install time.

All workflows use least-privilege `permissions: contents: read`.

## Dependency / lockfile constraints

1. **`ui/package-lock.json` must stay on `registry.npmjs.org/`.** No `tfs-app1:8080` URLs in
   `resolved` fields — they break Dependabot npm runs (it can't reach the internal feed). Sanity
   check: `grep -c 'tfs-app1' ui/package-lock.json` → must return `0`.
2. **`--legacy-peer-deps` is required** at every npm install: `storybook-addon-react-router-v6`
   pulls `react-inspector@6.0.2`, which lists React 16-18 in peers and conflicts with the React 19
   tree.
3. **Cross-platform `@parcel/watcher` lockfile gotcha.** `sass → @parcel/watcher` ships a
   different native binary per OS/arch; `npm install` on macOS only writes the `darwin-arm64`
   entry. Let CircleCI resolve the Linux entry via `npm install` (not `npm ci`).
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
