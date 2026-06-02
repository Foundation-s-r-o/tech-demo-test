# Development Guidelines

## Stack & Ground Rules (upgrade COMPLETE — 2026-05-27)

The backend upgrade is **done**. Current stack: **Spring Boot 4.0.6 / Java 21 LTS / embedded H2 / Spring Security session login / OpenPDF**. Full history, reasoning, and the Boot-4 migration notes live in [`docs/UPGRADE_PLAN.md`](docs/UPGRADE_PLAN.md).

**JDK:** the project targets **Java 21 LTS** (default, pinned in `api/.sdkmanrc` → `sdk env`). Boot 4 also builds green on Java 25 if you prefer it; just bump `<java.version>`. The global SDKMAN default is intentionally left alone (the workspace has Java 8/11 projects).

**Fast iteration is paramount.** Keep the local inner loop fast and **flag anything that slows local development** — Docker requirements, slow tests, heavyweight infra. Speed comes from removing infra, not from loosening the build gate.

**Current stack (in place):**
- **Spring Boot 4.0.6** (stable 4.0 line; do NOT chase 4.1+).
- **Java 21 LTS** (project default via `api/.sdkmanrc`; also verified green on Java 25).
- **DB: embedded H2 file-mode** (`./data/`, gitignored) — no Docker. Flyway migrations in **portable ANSI SQL**. **PostgreSQL is the future production DB (deferred)** — dialect/datasource stay **profile-driven** (never hardcode `H2Dialect` in shared config); avoid vendor-specific SQL/types so the H2→Postgres swap stays a non-event.
- **Auth: Spring Security session login**, users in DB, BCrypt. Bootstrap user **`admin` / `admin`** (seeded via Flyway `V3`). **Keycloak comes later** — auth is kept dead-simple and swappable; no premature OAuth2 abstraction. CSRF disabled + permissive CORS are **demo-only** (flagged in `SecurityConfig`/`WebConfiguration`); tighten before any real deployment.
- **PDF: OpenPDF** (`com.github.librepdf:openpdf`) — `GET /api/persons/{id}/pdf`.
- **JSON: Jackson 3** (`tools.jackson`, via `spring-boot-starter-json`).

**Removed / deferred:**
- **Removed:** Redis/Redisson, the `ai/` OpenAI package, `spring-cloud-sleuth-otel`, legacy `hibernate-types-60`, the custom `DbPhysicalNamingStrategy`.
- **Observability:** **Actuator + Micrometer** only. **OpenTelemetry + Prometheus deferred** (Phase 8, on Boot-4-stable versions, when needed).
- **Docker: not used** — returns later with Keycloak / real deployment / PostgreSQL ITs.

**Build gate stays strict** during the upgrade: Checkstyle `failOnViolation=true`, integration tests + JaCoCo on `verify`. With few tests, the gate is the main safety net. Verify before each commit; do not relax it for speed.

## Project Overview

This is a full-stack Java/Spring Boot + React/TypeScript demo application serving as a template for future projects. It implements a comprehensive tech stack with observability, security scanning, and modern development practices.

### Architecture

**Backend (Java/Spring Boot)**
- Entry Point: `TechDemoApplication.java:10` - Standard Spring Boot application
- Framework: Spring Boot 4.0.6 with Java 21 LTS (also builds on Java 25)
- Database: embedded H2 (file-mode) with JPA/Hibernate 7, Flyway migrations
- Key Features:
  - Person CRUD operations (`persons/` package)
  - Authentication: Spring Security session login (`auth/` package)
  - Infrastructure utilities (`infrastructure/` package)

**Frontend (React/TypeScript)**
- Entry Point: `index.tsx:10` - React 19 with ReactDOM
- Framework: React with TypeScript, React Router v7
- Styling: Bootstrap 5.3.3 with SCSS
- Key Features:
  - Authentication context and protected routes
  - Person management pages
  - Reusable UI components
  - Form handling with Formik + Yup validation

### Development Stack

**Build & Packaging**
- Backend: Maven with comprehensive plugin setup (Checkstyle, JaCoCo, Failsafe)
- Frontend: Webpack with TypeScript, ESLint, Playwright testing

**Containerization (deferred, do not rely on)**
- The repo retains `docker-compose.yml`, `run_application.sh`, and `.env - Sample` as
  forward scaffolding for the future PostgreSQL + Keycloak phase. They reference the
  **removed** MariaDB + Redis + OpenAI + OTel-collector stack and are not the current
  local-dev path. Local dev is two terminals: `./mvnw spring-boot:run` + `npm start`.

**Security & Quality (current — see `SECURITY.md` for full posture)**
- FOSSA license + vuln scan via `.github/workflows/fossa.yml` (scan-only, see SECURITY.md
  for why `run-tests` is off).
- GitHub CodeQL via default setup (categories `java-kotlin`, `javascript-typescript`,
  `actions`).
- GitGuardian secret scanning on PRs; GitHub secret scanning + push protection enabled.
- Dependabot weekly Maven + npm; lockfile **must stay on `registry.npmjs.org/`** —
  internal TFS-resolved URLs break Dependabot npm runs (see tripwire below).
- Checkstyle on `./mvnw validate` (hard gate, `failOnViolation=true`).
- Bouncy Castle for cryptography; explicit Tomcat 11.0.22 / okio 3.17.0 pins for CVE fixes.

**Observability (deferred)**
- Wired today: Spring Boot Actuator + Micrometer only.
- Deferred to a later phase (Boot-4-stable versions, when needed): OpenTelemetry,
  Prometheus, Loki, Jaeger, Grafana. The old Docker Compose scaffolding still references
  these but they are not running.

### Key Architectural Patterns
- Clean architecture with separate domain/API layers
- Constructor injection with Lombok `@RequiredArgsConstructor`
- Repository pattern with JPA
- React functional components with hooks
- Type-safe API client generation from OpenAPI specs

## Essential Commands

### Backend (Java/Spring Boot)
- Build: `./mvnw clean install`
- Run tests: `./mvnw test`
- Run single test: `./mvnw -Dtest=TestClass#testMethod test`
- Run integration tests: `./mvnw verify`
- Debug: `./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"`
- Generate code coverage: `./mvnw jacoco:report`
- Check code style: `./mvnw checkstyle:check`
- Start application: `./mvnw spring-boot:run`

### Frontend (React/TypeScript)
- Install: `npm install --legacy-peer-deps`
- Start dev server: `npm start`
- Build: `npm run build`
- Lint: `npm run lint`
- Run tests: `npm run playwright`
- Run specific test: `npx playwright test tests/path/to/test.spec.ts`
- Generate API client: `npm run generate-openapi-services`

### Docker Commands (deferred)
Docker is not used in the current local-dev path. The `docker-compose.yml` is kept as
forward scaffolding for the future PostgreSQL + Keycloak phase but **references the old
MariaDB / Redis / OTel stack and will not work as-is**. Don't run `docker-compose up`
expecting a functional app today.

### Project Health & Quality
- **Comprehensive health check**: `./doctor` - Runs build, lint, tests, and FOSSA scans
- **License scanning**: `fossa analyze` - Scan dependencies for license compliance
- **Security testing**: `fossa test` - Test for license or security issues

## Verifying Code Changes

Choose verification depth by **total net lines changed** (added + removed across all files — the `git diff --stat` total).

### Small change — ≤ 50 net lines
ESLint is sufficient for frontend-only changes:
```bash
cd ui && npm run lint          # tsc --noEmit + eslint src/ — both must pass
```
For backend-only small changes, run `cd api && ./mvnw checkstyle:check test`.
Fix every error. Warnings are usually fine — **flag any you leave behind** in your summary.

### Big change — > 50 net lines
Run the full suite and **fix all errors** before reporting done. Warnings are generally acceptable but must be flagged (call out anything new or security-relevant). All scan artifacts go under `docs/`.

```bash
# 1. Frontend lint + type-check (gate)
cd ui && npm run lint

# 2. Backend: style + unit tests + integration tests + coverage
cd api && ./mvnw clean verify          # Checkstyle (validate phase), 7 unit + 13 IT, JaCoCo.
#   Current ITs run on in-memory H2 only — NO Docker / Testcontainers required.
#   Today: BUILD SUCCESS in ~12s on Java 21. If anything tries to start a Docker
#   container, that's a regression — flag it.

# 3. Playwright smoke tests (see below) — run after major changes
cd ui && npm run smoke                 # app smoke (auto-starts dev server :8090)
cd ui && npm run smoke:storybook       # Storybook smoke (auto-starts :6006)

# 4. Trivy — filesystem vuln scan, run in repo ROOT. Reports to docs/.
trivy fs . --scanners vuln --format table --output docs/trivy-report.txt
trivy fs . --scanners vuln --format json  --output docs/trivy-report.json

# 5. SonarQube — static analysis. Host: 10.16.35.93:9000 (server v26.x; needs SONAR_TOKEN in env).
# Use -Dsonar.token (not -Dsonar.login — removed in SonarQube 25+).
cd api && sonar-scanner -Dsonar.host.url=http://10.16.35.93:9000/ -Dsonar.token=$SONAR_TOKEN
cd ui  && sonar-scanner -Dsonar.host.url=http://10.16.35.93:9000/ -Dsonar.token=$SONAR_TOKEN
# Pull issues + gate metrics into docs/ for review (project keys: tech-demo-api, tech-demo-ui):
curl -s -u "$SONAR_TOKEN:" "http://10.16.35.93:9000/api/issues/search?componentKeys=tech-demo-api" | jq '.' > docs/sonar-issues-tech-demo-api.json
curl -s -u "$SONAR_TOKEN:" "http://10.16.35.93:9000/api/measures/component?component=tech-demo-api&metricKeys=bugs,vulnerabilities,security_hotspots,code_smells,coverage" | jq '.' > docs/sonar-metrics-tech-demo-api.json
```

**Then read the outputs and act on them:**
- ESLint / tsc / Checkstyle: errors → fix; warnings → flag.
- `docs/trivy-report.txt`: CRITICAL/HIGH CVEs → fix (minimal safe bump; see dependency constraints in `../CLAUDE.md`); MEDIUM/LOW → flag. CVEs with no released fix → flag, don't churn.
- Sonar: bugs and vulnerabilities → fix; CRITICAL code smells → fix; lesser smells / hotspots → flag and triage. Dashboards: `http://10.16.35.93:9000/dashboard?id=tech-demo-api` (and `...-ui`).
- Smoke tests: any failure → fix the underlying error, not the test.

Escalate to the full suite regardless of line count when touching: `ui/src/common/auth/*`, `ui/src/common/axiosClient.ts`, `ui/src/api/*`, or backend `auth/` / `infrastructure/`.

### Playwright smoke tests

Lightweight, **backend-free** tests that catch load/render/runtime-console errors. Run after major changes.

- **App smoke** (`ui/tests/smoke.spec.ts`, config `playwright.smoke.config.ts`): auto-starts the webpack dev server on a dedicated port (`:8090`, no overlay), loads the login shell, asserts `#login_username/#login_password/#login_submit` render, types into fields, and **fails on any browser console error / page error**. Does not use the backend (omits the E2E `global_setup` that logs into `:8082`).
- **Storybook smoke** (`ui/tests/smoke.storybook.spec.ts`, config `playwright.storybook.config.ts`): auto-starts Storybook on `:6006` and renders the `Button` and `Form` stories, failing on console errors.

```bash
cd ui
npm run smoke              # app smoke
npm run smoke:storybook    # storybook smoke
npx playwright install chromium   # one-time, if the browser binary is missing
```

**Reading browser console output:** `ui/tests/console-capture.ts` records all `console`, `pageerror`, and `requestfailed` events (filtering out dev-server/HMR noise) and writes them to `docs/smoke-app.log` and `docs/smoke-storybook-*.log` after each run — read these to spot errors. Console errors and page errors fail the test; warnings are logged but do not fail. The Playwright HTML report (with trace + failure screenshot) is at `ui/playwright-report-smoke/` and `ui/playwright-report-storybook/`. The smoke `*.log` files and report dirs are gitignored (regenerated each run).

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
- ESLint: Enhanced configuration with `@typescript-eslint/no-unsafe-argument` error enforcement

## CI / Workflows (current — `.github/workflows/`)

Active workflows after the 2026-06-01 cleanup (9 → 3 files):

| File | Trigger | What it does |
| ---- | ------- | ------------ |
| `fossa.yml` | push/PR on `main` | FOSSA license + vuln scan (scan-only, no test gate) |
| `deploy-to-aws-eb.yml` | manual (`workflow_dispatch`) | Elastic Beanstalk deploy scaffold |
| `push-to-ecr.yml` | manual (`workflow_dispatch`) | Docker → ECR push scaffold |

GitHub-managed (not in `.github/workflows/`):
- **CodeQL default setup** — runs on push/PR/weekly across `java-kotlin`,
  `javascript-typescript`, `actions` categories. **This is the real CodeQL coverage**;
  do not add a custom `code-scan.yml` (the previous one was deleted as redundant +
  missing `init` step → wouldn't run anyway).
- **Dependabot Updates** — `.github/dependabot.yml` schedules weekly Maven (`/api`)
  and npm (`/ui`) bumps.

**CircleCI** runs the actual build gate: backend `./mvnw verify` + frontend webpack
build. Key config notes in `.circleci/config.yml`:
- `cimg/openjdk:21.0` (matches project Java 21 LTS — NOT JDK 17).
- `override-ci-command: npm install --legacy-peer-deps --no-audit --no-fund` — NOT
  `npm ci`. The lockfile only pins the dev-host's `@parcel/watcher-darwin-arm64`
  binary; the other 12 platform variants lack full lockfile entries, so strict
  `npm ci` fails on Linux. `npm install` honors the lockfile when accurate and
  fills the missing optional binaries at install time.

All workflows have least-privilege `permissions: contents: read` (closes CodeQL
`actions/missing-workflow-permissions` warning).

## Dependency / lockfile tripwires (hard-learned)

1. **`ui/package-lock.json` must stay on `registry.npmjs.org/`.** No `tfs-app1:8080`
   URLs in `resolved` fields. If they creep back in, Dependabot npm runs blow up
   (Server error 500 reaching the unreachable internal feed). Sanity check:
   `grep -c 'tfs-app1' ui/package-lock.json` → must return `0`.

2. **Cross-platform `@parcel/watcher` lockfile gotcha.** `sass@1.x → @parcel/watcher@2.x`
   ships a different native binary per OS/arch. `npm install` on macOS only writes
   the `darwin-arm64` entry. Don't fight this on a Mac dev machine; let CircleCI
   resolve the Linux entry via `npm install` (not `npm ci`). See CircleCI note above.

3. **`--legacy-peer-deps` is required** at every npm install: `storybook-addon-react-router-v6`
   pulls `react-inspector@6.0.2` which still lists React 16-18 in peers, conflicting
   with our React 19 tree. `npm install --legacy-peer-deps` papers over it.

4. **Public-only npm registry locally.** If your global `~/.npmrc` points at the
   internal TFS feed (or anything else), pass `--registry=https://registry.npmjs.org/`
   explicitly when regenerating the lockfile. Don't edit `.npmrc` (other workspace
   projects need it).

## Documenting deliberate suppressions (CodeQL pattern)

When a finding is *deliberately* allowed (e.g. CSRF disabled for the demo), do two things:

1. **Add a `// SECURITY:` comment at the line** explaining what, why, and when it
   must be re-evaluated. Include the rule id (`// lgtm[java/spring-disabled-csrf-protection]`)
   so future re-scans link back. Example in `SecurityConfig.java:48-53`.
2. **Dismiss the alert via API or UI** with a short reason pointing at SECURITY.md:
   ```bash
   gh api -X PATCH /repos/<org>/<repo>/code-scanning/alerts/<N> \
     -f state=dismissed -f dismissed_reason="won't fix" \
     -f dismissed_comment='Deliberately disabled for demo — see SECURITY.md'
   # 280-char limit on dismissed_comment
   ```

Don't leave deliberate-by-design findings in `open` state — they hide real issues.
Don't silently dismiss without the source comment — the next reader won't know why.

## Security
- No hardcoded credentials or secrets in code.
- Use environment variables for sensitive data.
- Follow OWASP secure coding practices.
- Run FOSSA scans to check dependencies.
- **Vulnerability reporting + demo-mode caveats + supported-versions policy live in
  [`SECURITY.md`](SECURITY.md).** Update SECURITY.md whenever a demo-mode caveat
  changes (CSRF re-enabled, CORS tightened, H2 console removed from `PUBLIC_PATHS`, etc.).

## Recent Updates (Updated: 2026-06-02)

### Stack upgrade (Spring Boot 3.4 → 4.0.6, Java 17 → 21 LTS) — see `docs/UPGRADE_PLAN.md`
- **Spring Boot 4.0.6 / Java 21 LTS** (project default); also builds on Java 25.
  Hibernate 7; Jackson 3 (`tools.jackson`).
- **Database**: MariaDB → embedded **H2 file-mode** (no Docker); portable ANSI Flyway
  migrations; integration tests run Docker-free (`./mvnw verify`: 7 unit + 13 IT).
- **Auth**: replaced the in-memory mock with **Spring Security** session login backed
  by a Flyway-seeded `USERS` table (`admin/admin`, BCrypt).
- **PDF**: added **OpenPDF 2.0.3** — `GET /api/persons/{id}/pdf`. (3.x major bump
  deliberately deferred — namespace `com.lowagie.text` → `org.openpdf`; not worth the
  migration churn yet.)
- **Removed**: Redis/Redisson, OpenAI `ai/` package, `spring-cloud-sleuth-otel`,
  legacy `hibernate-types-60`.

### Frontend / tooling (2026-05-31 → 2026-06-01)
- **React 19.0 → 19.2.x**; dropped `react-helmet` (React 19 hoists `<title>` natively).
- **Node engine raised to ≥ 22.11.0** (forced by sass-loader 17 peer requirement).
- **Lockfile rewritten against public `registry.npmjs.org/`** — removed 304 leftover
  TFS-feed `resolved` URLs that were breaking Dependabot npm runs.
- **Checkstyle 10.16 → 13.5**, **sass-loader 16 → 17**, **type-fest 4 → 5**, plus
  3 backend minors (spotbugs-annotations, okio, micrometer-prometheus).

### Workflow / CI cleanup (2026-06-01)
- Deleted 6 dead workflows (`maven.yml`, `npm.yml`, `sonarcloud.yml`, `goodparts.yml`,
  `goodparts_lint.yml`, `code-scan.yml`). Kept + permissions-hardened: `fossa.yml`,
  `deploy-to-aws-eb.yml`, `push-to-ecr.yml`.
- CircleCI: `cimg/openjdk:17.0 → 21.0`; install command flipped from `npm ci` to
  `npm install --legacy-peer-deps` (see Tripwires above).
- FOSSA workflow modernised: `actions/checkout@v2 → @v4`,
  `fossas/fossa-action@v1 → @v1.9.0` + `FORCE_JAVASCRIPT_ACTIONS_TO_NODE24` for
  the June-2 GitHub Actions Node 24 cutover.

### Security (2026-06-01)
- New `SECURITY.md`: GitHub Private Vulnerability Reporting, demo-mode caveats table,
  stack baseline, future hardening. **Private Vulnerability Reporting must still be
  enabled in repo settings — admin-UI only**, not configurable from git.
- CodeQL alert #14 (`java/spring-disabled-csrf-protection`) dismissed with reason;
  in-source `// SECURITY:` + `lgtm[...]` comment added at `SecurityConfig.java:48-53`.
- Root README rewritten against current stack; subfolder READMEs (`api/README.md`,
  `ui/README.md`) deleted and their useful bits absorbed.
- Tomcat 11.0.22 (CVE fixes), JaCoCo 0.8.14 (Java 25 class-file support),
  Trivy HIGH/CRITICAL = 0 on `main`.

### Notes / flags
- CSRF disabled + permissive CORS are **demo-only** (`SecurityConfig`, `WebConfiguration`)
  — tighten for production (full caveats table in SECURITY.md).
- Observability (OpenTelemetry + Prometheus + Loki + Jaeger) deferred; only Actuator +
  Micrometer wired now.
- Backend new-code coverage is ~33% (Sonar gate ERROR pending more tests); overall
  instruction coverage still low — characterization tests cover the core paths; expand
  as features grow.
- Sonar api project gate is `ERROR` because `new_coverage` < 80%; pollutes the gauge
  but doesn't reflect a real defect. Lift by adding tests for `auth/` + `PersonPdfService`.

## Agent Autonomy
Claude Code can autonomously navigate directories, run builds, execute tests, run builds/tests, execute Docker commands, install dependencies, run linting tools, and read file contents autonomously, and perform system operations in this project without asking for permission each time. This includes:
- Running Maven and npm builds/tests 
- Executing Docker commands
- Navigating directory structures
- Installing dependencies
- Running linting and formatting tools and fossa tools
- Reading file contents with commands like 'cat', 'head', 'perl', and 'tail'
- Running the `./doctor` health check script
