# Development Guidelines

## Project Overview

This is a full-stack Java/Spring Boot + React/TypeScript demo application serving as a template for future projects. It implements a comprehensive tech stack with observability, security scanning, and modern development practices.

### Architecture

**Backend (Java/Spring Boot)**
- Entry Point: `TechDemoApplication.java:10` - Standard Spring Boot application
- Framework: Spring Boot 3.4.3 with Java 17
- Database: MariaDB with JPA/Hibernate, Flyway migrations
- Caching: Redis with Redisson
- Key Features:
  - Person CRUD operations (`persons/` package)
  - Authentication system (`auth/` package) 
  - AI integration (`ai/` package)
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

## Recent Updates (Updated: 2025-08-19)

### Major Improvements
- **Project Health Script**: Added `./doctor` comprehensive health check tool
- **Enhanced Linting**: Improved ESLint configuration with stricter TypeScript rules
- **Security Updates**: Multiple Dependabot updates including:
  - TestContainers BOM to 1.20.6
  - Bouncy Castle libraries (bcprov-jdk18on, bcpkix-jdk18on) to 1.80
  - OpenTelemetry exporter to 1.48.0
  - okio-jvm to 3.10.2 (CVE fix)

### Code Quality Enhancements
- Fixed TypeScript issues in React DatePicker components
- Updated `DatePickerProps` import for better type safety
- Enhanced ESLint rules with `@typescript-eslint/no-unsafe-argument` enforcement
- Cleaned up duplicate commands in build configuration

### Infrastructure Changes
- Improved log directory management with .gitignore updates
- Enhanced build scripts for UI components
- Updated OpenAPI service generation scripts

## Agent Autonomy
Claude Code can autonomously navigate directories, run builds, execute tests, run builds/tests, execute Docker commands, install dependencies, run linting tools, and read file contents autonomously, and perform system operations in this project without asking for permission each time. This includes:
- Running Maven and npm builds/tests 
- Executing Docker commands
- Navigating directory structures
- Installing dependencies
- Running linting and formatting tools and fossa tools
- Reading file contents with commands like 'cat', 'head', 'perl', and 'tail'
- Running the `./doctor` health check script
