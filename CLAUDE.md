# Development Guidelines

## Stack & Ground Rules (upgrade COMPLETE — 2026-05-27)

The backend upgrade is **done**. Current stack: **Spring Boot 4.0.6 / Java 25 LTS / embedded H2 / Spring Security session login / OpenPDF**. Full history, reasoning, and the Boot-4 migration notes live in [`docs/UPGRADE_PLAN.md`](docs/UPGRADE_PLAN.md).

**Build requirement:** Java 25 must be active before building — `sdk use java 25.0.3-zulu`. (The global SDKMAN default is intentionally left alone; the workspace has Java 8/11/21 projects.)

**Fast iteration is paramount.** Keep the local inner loop fast and **flag anything that slows local development** — Docker requirements, slow tests, heavyweight infra. Speed comes from removing infra, not from loosening the build gate.

**Current stack (in place):**
- **Spring Boot 4.0.6** (stable 4.0 line; do NOT chase 4.1+).
- **Java 25 LTS** (`sdk use java 25.0.3-zulu` to build).
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
- Framework: Spring Boot 4.0.6 with Java 25 LTS
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

**Containerization**
- Full Docker Compose stack with 9 services:
  - Application (API + UI), MariaDB, Redis
  - Observability: Prometheus, Grafana, Jaeger, Loki, OpenTelemetry Collector
  - Redis Commander, New Relic agent

**Security & Quality**
- FOSSA license scanning and vulnerability detection
- Checkstyle code style enforcement  
- Dependabot automated dependency updates
- Bouncy Castle cryptography libraries
- Explicit security patches (Tomcat, okio)

**Observability**
- Distributed tracing with OpenTelemetry and Jaeger
- Metrics collection with Prometheus
- Centralized logging with Loki
- Grafana dashboards

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

### Docker Commands
- Start all services: `docker-compose up -d`
- Run tests in container: `docker-compose exec api ./mvnw test`
- View logs: `docker-compose logs -f [service_name]`

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

# 2. Backend: style + unit tests + coverage
cd api && ./mvnw clean test            # Checkstyle (validate phase), unit tests, JaCoCo
#   Integration tests need Docker/Testcontainers; when Docker is available run:
#   ./mvnw clean verify                # adds Testcontainers integration tests
#   If Docker is absent, fall back to `./mvnw clean test` and FLAG the skipped IT.

# 3. Playwright smoke tests (see below) — run after major changes
cd ui && npm run smoke                 # app smoke (auto-starts dev server :8090)
cd ui && npm run smoke:storybook       # Storybook smoke (auto-starts :6006)

# 4. Trivy — filesystem vuln scan, run in repo ROOT. Reports to docs/.
trivy fs . --scanners vuln --format table --output docs/trivy-report.txt
trivy fs . --scanners vuln --format json  --output docs/trivy-report.json

# 5. SonarQube — static analysis. Host: 10.16.35.93:9000 (needs SONAR_TOKEN in env).
cd api && sonar-scanner -Dsonar.host.url=http://10.16.35.93:9000/ -Dsonar.login=$SONAR_TOKEN
cd ui  && sonar-scanner -Dsonar.host.url=http://10.16.35.93:9000/ -Dsonar.login=$SONAR_TOKEN
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

## Security
- No hardcoded credentials or secrets in code
- Use environment variables for sensitive data
- Follow OWASP secure coding practices
- Run FOSSA scans to check dependencies

## Recent Updates (Updated: 2026-05-27)

### Stack upgrade (Spring Boot 3.4 → 4.0.6, Java 17 → 25) — see `docs/UPGRADE_PLAN.md`
- **Spring Boot 4.0.6 / Java 25 LTS**; Hibernate 7; Jackson 3 (`tools.jackson`).
- **Database**: MariaDB → embedded **H2 file-mode** (no Docker); portable ANSI Flyway migrations; integration tests run Docker-free.
- **Auth**: replaced the in-memory mock with **Spring Security** session login backed by a Flyway-seeded `USERS` table (`admin/admin`, BCrypt).
- **PDF**: added **OpenPDF** — `GET /api/persons/{id}/pdf`.
- **Removed**: Redis/Redisson, OpenAI `ai/` package, `spring-cloud-sleuth-otel`, legacy `hibernate-types-60`.
- **Security**: Tomcat 11.0.22 (CVE fixes), JaCoCo 0.8.14 (Java 25), Trivy HIGH/CRITICAL = 0.

### Notes / flags
- CSRF disabled + permissive CORS are **demo-only** (`SecurityConfig`, `WebConfiguration`) — tighten for production.
- Observability (OpenTelemetry + Prometheus) deferred to a later phase; only Actuator + Micrometer are wired now.
- Backend coverage is low (~23% instructions) — characterization tests cover the core paths; expand as features grow.

## Agent Autonomy
Claude Code can autonomously navigate directories, run builds, execute tests, run builds/tests, execute Docker commands, install dependencies, run linting tools, and read file contents autonomously, and perform system operations in this project without asking for permission each time. This includes:
- Running Maven and npm builds/tests 
- Executing Docker commands
- Navigating directory structures
- Installing dependencies
- Running linting and formatting tools and fossa tools
- Reading file contents with commands like 'cat', 'head', 'perl', and 'tail'
- Running the `./doctor` health check script
