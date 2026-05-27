# Codebase Analysis — `tech-demo-test`

> Full-stack Java/Spring Boot + React/TypeScript reference application maintained by Foundation s.r.o. as a **starter template** for new projects. Generated 2026-05-27.

---

## 1. Project Overview

| Aspect | Detail |
|---|---|
| **Type** | Full-stack web application (REST API + SPA), packaged as a single deployable JAR |
| **Purpose** | "Standard project stack" template — demonstrates the full toolchain (observability, security scanning, CI/CD, Docker) rather than solving a business problem |
| **Backend** | Java 17, Spring Boot 3.4.3 |
| **Frontend** | React 19, TypeScript 5.8, Webpack 5 |
| **Database** | MariaDB 11.2 (JPA/Hibernate + Flyway migrations) |
| **Cache** | Redis via Redisson (disabled by default) |
| **Architecture** | Modular monolith. Clean/layered architecture per feature (domain → service → API). Frontend is a separate SPA served as static assets from the same JAR. |
| **Domain model** | A single `Person` CRUD entity + a mocked auth flow + an OpenAI proxy |
| **Build** | Maven (backend), npm/Webpack (frontend), multi-stage Docker |
| **License** | MIT (Copyright 2025 Foundation s.r.o.) |

The application is deliberately minimal in *business* terms — one entity, mocked login — but rich in *infrastructure*: tracing, metrics, centralized logging, license scanning, static analysis, and multiple deployment paths.

---

## 2. Directory Structure Analysis

```
tech-demo-test/
├── api/                  # Spring Boot backend (Maven module)
├── ui/                   # React/TypeScript SPA (npm project)
├── .github/workflows/    # GitHub Actions CI/CD (9 workflows)
├── .circleci/            # Alternative CircleCI pipeline (auto-generated)
├── docs/decisions/       # Architecture Decision Records (ADR)
├── grafana/ loki/ prometheus/ otel-config.yml  # Observability stack configs
├── performancetest/      # JMeter load test plan (TechDemo.jmx)
├── docker-compose.yml    # Local full-stack (9 services)
├── aws.docker-compose.yml# AWS-flavoured compose
├── Dockerfile            # Multi-stage build (API + UI → single JAR)
├── doctor                # One-shot health check script (build+lint+test+FOSSA)
└── run_application.sh     # Interactive local/docker launcher
```

### `api/` — Backend
Spring Boot service. Source under `src/main/java/sk/foundation/techdemo/`, organized **by feature, not by layer**:
- `persons/` — the reference CRUD feature (domain entity, repository, service) + `persons/api/` (controller, DTOs, filtering/sorting/paging, custom criteria repo)
- `auth/` — mocked authentication (login/logout/currentuser)
- `ai/` — OpenAI chat + assistant proxy
- `infrastructure/` — cross-cutting: API exception handling, paging/sorting DTOs, JPA base entity, DB naming strategy, logging & web config
- Root config classes: `TechDemoApplication` (entry point), `RedissonCacheConfig`, `OpenAIRestTemplateConfig`, `TracingConfiguration`, `TechDemoApplicationConfiguration`
- `src/integration-test/` — separate source root (wired via `build-helper-maven-plugin`) using Testcontainers; `src/test/` — fast unit tests
- `src/main/resources/` — `application.properties`, Flyway migration `V1__create_person_table.sql`, Loki/file logback config

### `ui/` — Frontend
Webpack-bundled React SPA:
- `src/api/` — generated OpenAPI TypeScript-Axios client (`generated/`) + hand-written wrappers (`api.ts`, `types.ts`)
- `src/common/` — auth context/provider/reducer, `axiosClient` (configured Axios), i18n, persistence, Yup validation helpers
- `src/components/` — reusable UI: `form/` (Formik fields), `shared/` (table, buttons, modals, accordion), layout (`header`, `footer`, `menu`, `layout`), `router/` (route table + auth guard), `app/`
- `src/pages/` — feature pages: `login/`, `home/`, `persons/` (List/Add/Edit/Form/Table/validation)
- `src/assets/` — SCSS (Bootstrap overrides), images, `translations/sk.json`
- `src/stories/` — Storybook stories
- `tests/` — Playwright E2E

### Connecting flow
React pages → `axiosClient` / generated client → `/api/**` REST endpoints → Spring services → JPA repositories → MariaDB. The UI build output (`ui/dist`) is copied into the Spring JAR's `static/` directory by the Dockerfile, so one container serves both.

---

## 3. File-by-File Breakdown

### Core Application Files
| File | Role |
|---|---|
| `api/.../TechDemoApplication.java` | Spring Boot main class |
| `api/.../persons/api/PersonController.java` | REST CRUD endpoints for Person |
| `api/.../persons/PersonServiceImpl.java` / `PersonApiServiceImpl.java` | Business logic (domain service + API/DTO service split) |
| `api/.../persons/api/PersonApiRepositoryImpl.java` | Custom Criteria-API repo for filtering/sorting/paging |
| `api/.../auth/AuthController.java` + `AuthServiceImpl.java` | **Mocked** login/logout/currentuser |
| `api/.../ai/api/AiController.java` | OpenAI chat + assistant REST proxy |
| `api/.../infrastructure/api/ApiExceptionHandler.java` | Centralized `@RestControllerAdvice` error mapping |
| `ui/src/index.tsx` | React entry point |
| `ui/src/components/router/AppRouter.tsx` + `routes.tsx` | Route table + `withRequireAuth` guard |
| `ui/src/pages/persons/*` | CRUD UI (List/Add/Edit/Form/Table) |
| `ui/src/common/auth/*` | Auth context/provider/reducer/`RequireAuth` |

### Configuration Files
| File | Role |
|---|---|
| `api/pom.xml` | Maven build, dependencies, Checkstyle/JaCoCo/Failsafe plugins |
| `api/src/main/resources/application.properties` | Datasource, JPA, Flyway, Actuator, OTel, caching, OpenAI |
| `api/checkstyle/checkstyle.xml` | Code-style rules (enforced at `validate` phase, fails build) |
| `ui/package.json` | npm scripts & deps |
| `ui/webpack.config.js`, `tsconfig.json`, `eslint.config.cjs`, `.prettierrc`, `.babelrc` | Frontend build/lint/format |
| `ui/openapitools.json` | OpenAPI client generation config |
| `.env - Sample` | Template for required env vars |

### Data Layer
| File | Role |
|---|---|
| `api/.../persons/Person.java` | JPA entity (extends `IdentifiableEntity<>`) |
| `api/.../persons/PersonRepository.java` | Spring Data JPA repository |
| `api/.../infrastructure/db/IdentifiableEntity.java` | Base entity (shared ID handling) |
| `api/.../infrastructure/db/DbPhysicalNamingStrategy.java` | Hibernate naming strategy (UPPER_SNAKE column names) |
| `db/migration/V1__create_person_table.sql` | Flyway: `PERSON` table, unique email, `(LAST_NAME, FIRST_NAME)` index |

### Frontend / UI
Form components wrap Formik+Yup; `shared/table/` is a self-contained reducer-driven data table (pagination, sorting, context). Styling is Bootstrap 5 + SCSS with BEM. i18n via i18next (currently `sk.json` only).

### Testing
| Layer | Tooling |
|---|---|
| Backend unit | JUnit 5 — `PersonControllerTest.java` |
| Backend integration | Testcontainers (MariaDB) — `PersonControllerIT`, `PersonRepositoryIT`, `FlywayMigrationIT`, `BaseIT` |
| Frontend E2E | Playwright — `tests/test_authentication.spec.ts` + page-object `home_page.ts` |
| Performance | JMeter — `performancetest/TechDemo.jmx` |

### Documentation
`README.md` (run instructions, FOSSA), `CLAUDE.md` (dev guidelines), `api/README.md` & `ui/README.md` (per-module), `docs/decisions/` (ADRs using MADR template), `fossa_attribution*.txt`.

### DevOps
Multi-stage `Dockerfile`, `docker-compose.yml` (9 services), `doctor` script, `run_application.sh`, and 9 GitHub Actions workflows + a CircleCI config.

---

## 4. API Endpoints

Base path: `/api`. No API versioning in the path (OpenAPI doc version `v0`). SpringDoc/Swagger UI exposed.

### Persons — `/api/persons`
| Method | Path | Description | Response |
|---|---|---|---|
| GET | `/api/persons/{id}` | Get one person | `PersonDetailResponseDTO` |
| GET | `/api/persons` | List (filter + paging + sorting via query params) | `PagedResultResponseDTO<PersonListItemResponseDTO>` |
| POST | `/api/persons` | Create (`@Valid` body) | `201` + `IdResponseDTO<Long>` |
| PUT | `/api/persons/{id}` | Update | `200` |
| DELETE | `/api/persons/{id}` | Delete | `200` |

### Auth — `/api/auth`
| Method | Path | Description |
|---|---|---|
| POST | `/api/auth/login` | Login → `UserResponseDTO` |
| GET | `/api/auth/currentuser` | Current user (or `401`) |
| POST | `/api/auth/logout` | Logout |

### AI — `/api/ai`
| Method | Path | Description |
|---|---|---|
| GET | `/api/ai/chat?message=` | Proxy to OpenAI chat completions |
| GET | `/api/ai/assistant?message=` | Proxy to OpenAI Assistants v2 (thread/run/poll) |

### Actuator — `/actuator/**`
All endpoints exposed (`management.endpoints.web.exposure.include=*`), including Prometheus metrics.

**Auth/authz pattern:** ⚠️ Authentication is **mocked** (`AuthServiceImpl`): a single hardcoded `Admin/admin` credential, login state tracked in an in-memory `ConcurrentLinkedQueue`. There is **no Spring Security on the wire** — the `/api/persons` and `/api/ai` endpoints are not actually protected server-side; the only access control is the React `RequireAuth` route guard (client-side only). This is acceptable for a template demo but **must not ship to production as-is**.

---

## 5. Architecture Deep Dive

### Request lifecycle (Person CRUD)
```
React page (e.g. ListPage.tsx)
  → useFndtTable / Formik form
  → axiosClient (adds no-cache headers, serializes params, formats dates)
  → GET/POST /api/persons
  → PersonController (@RestController, @Valid)
  → PersonApiService (DTO mapping, paging) → PersonService (domain) 
  → PersonApiRepositoryImpl (Criteria API) / PersonRepository (Spring Data)
  → Hibernate → HikariCP → MariaDB
  ← PagedResultResponseDTO ← ResponseEntity(JSON)
  ← ApiExceptionHandler maps exceptions → ApiExceptionResponseDTO on error
```

### Key design patterns
- **Feature-based packaging** with a layer split inside each feature (`persons` domain vs `persons/api`).
- **Constructor injection** via Lombok `@RequiredArgsConstructor` + `final` fields.
- **DTO boundary** — separate request/response DTOs per use case (`PersonModifyRequestDTO`, `PersonDetailResponseDTO`, `PersonListItemResponseDTO`).
- **Repository pattern** — Spring Data JPA + a custom Criteria repository for dynamic queries.
- **Centralized error handling** — `@RestControllerAdvice`.
- **Contract-first frontend** — TS client generated from `api-docs.json` via `generate-openapi-services`.
- **Context + reducer** state on the frontend (auth, modals, table) — no Zustand here despite workspace convention.

### Cross-cutting infrastructure
- **Tracing:** Spring Cloud Sleuth + OpenTelemetry OTLP exporter → Collector → Jaeger.
- **Metrics:** Micrometer → Prometheus (Actuator endpoint) → Grafana.
- **Logging:** Logback with Loki appender + rolling file (`loki.enabled` toggle).
- **Caching:** Redisson/Redis (`caching.enabled` toggle, off by default — Redis unavailable in CI).

---

## 6. Environment & Setup

### Required environment variables (`.env`)
| Var | Purpose |
|---|---|
| `MYSQL_DATABASE_USERNAME` / `MYSQL_DATABASE_PASSWORD` | DB credentials |
| `MARIADB_VERSION` / `MARIADB_PORT_EXPORTED` | DB image/port |
| `APP_API_SERVER_URL` | Frontend → API base URL |
| `API_PORT_EXPORTED` | API exposed port |
| `ADMIN_USERNAME` / `ADMIN_PASS` | Seed/admin (used by Playwright too) |
| `NEW_RELIC_KEY` | New Relic infra agent license |
| `OPENAI_API_KEY` (implied by `OpenAIRestTemplateConfig`) | OpenAI proxy auth |

> Note: `.env - Sample` references `MYSQL_*` while `docker-compose.yml` uses `MARIADB_*`/`MYSQL_*` inconsistently — verify the actual keys before first run.

### Local run options
1. **Script:** copy `.env - Sample` → `.env`, fill values, `./run_application.sh` (choose local/docker/both).
2. **UI only:** `cd ui && npm install --legacy-peer-deps && npm start` (port 8080).
3. **API only:** `cd api && ./mvnw spring-boot:run` (with datasource args).
4. **Docker:** `docker compose up --build -d` — brings up all 9 services.

### Dev workflow
Static checks early (Checkstyle backend, ESLint+tsc frontend). `./doctor` runs build + lint + tests + FOSSA in one shot. Dependabot keeps deps current.

### Production deployment
Multi-stage `Dockerfile` builds API and UI, then assembles a layered Spring Boot JAR serving the UI from `static/`. Deploy paths: **AWS ECR** (`push-to-ecr.yml`) + **Elastic Beanstalk** (`deploy-to-aws-eb.yml`, region `eu-central-1`).

---

## 7. Technology Stack Breakdown

| Category | Technologies |
|---|---|
| **Runtime** | JVM (Java 17, Temurin); Node 18 (build only) |
| **Backend framework** | Spring Boot 3.4.3 (Web, Data JPA, Actuator), Spring Cloud 2024.0.0 |
| **Persistence** | Hibernate/JPA, MariaDB JDBC 3.3.3, HikariCP, Flyway, hibernate-types-60 |
| **Cache** | Redisson 3.32, Spring Data Redis |
| **Crypto** | Bouncy Castle (bcpkix/bcprov 1.80) |
| **API docs** | SpringDoc OpenAPI 2.6 (Swagger UI) |
| **Frontend** | React 19, React Router 7, TypeScript 5.8, Bootstrap 5.3, Formik+Yup, i18next, Axios, react-select, react-datepicker, date-fns, lodash |
| **Build tools** | Maven + Spring Boot plugin (layered JAR); Webpack 5 + Babel + ts-loader; OpenAPI Generator CLI |
| **Testing** | JUnit 5, Testcontainers 1.20.6, Spring Boot Test; Playwright; JMeter |
| **Observability** | OpenTelemetry, Jaeger, Prometheus, Grafana, Loki, Micrometer, New Relic |
| **Quality/Security** | Checkstyle 10.16, JaCoCo 0.8.12, ESLint 9, Prettier, SonarCloud, FOSSA, CodeClimate, goodparts, Semgrep (ignore file present) |
| **Containers** | Docker (multi-stage), Docker Compose (9 services) |
| **CI/CD** | GitHub Actions (9 workflows) + CircleCI; Dependabot |
| **Deploy targets** | AWS ECR + Elastic Beanstalk |

---

## 8. Architecture Diagram

```
                          ┌───────────────────────────────────────────────┐
                          │              Single Spring Boot JAR             │
                          │                                                 │
  Browser  ──HTTP──▶  ┌───┴────────────┐        ┌──────────────────────┐    │
  (React SPA          │  Static assets │        │  REST API  /api/**    │    │
   served from        │  (ui/dist →    │        │  ┌─────────────────┐  │    │
   /static)           │   static/)     │        │  │ PersonController │  │    │
                      └────────────────┘        │  │ AuthController   │  │    │
        │                                        │  │ AiController     │  │    │
        │  axiosClient / generated OpenAPI client│  └────────┬────────┘  │    │
        └────────────────────────────────────────▶          │           │    │
                                                 │   Service layer        │    │
                                                 │   (domain + api/DTO)   │    │
                                                 │           │           │    │
                                                 │   JPA repositories     │    │
                                                 └───────────┼───────────┘    │
                                                             │                │
                          └──────────────────────────────────┼───────────────┘
                                                             │
        ┌──────────────┬──────────────┬─────────────────────┼───────────────┐
        ▼              ▼              ▼                       ▼               ▼
  ┌──────────┐  ┌──────────┐  ┌────────────┐         ┌──────────────┐  ┌──────────┐
  │ MariaDB  │  │  Redis   │  │ OpenAI API │         │ OTel Collector│  │ Actuator │
  │ (Flyway) │  │(Redisson)│  │  (proxy)   │         └──────┬───────┘  │/prometheus│
  └──────────┘  └──────────┘  └────────────┘                │          └────┬─────┘
                                                    ┌────────┴───────┐       │
                                                    ▼                ▼       ▼
                                                ┌───────┐      ┌──────────────────┐
                                                │Jaeger │      │Prometheus→Grafana │
                                                └───────┘      └──────────────────┘
                                                          Loki ◀── Logback appender

  CI/CD:  GitHub Actions (Maven, npm, Sonar, FOSSA, code-scan, goodparts, ECR, EB) + CircleCI
  Deploy: Docker image → AWS ECR → Elastic Beanstalk (eu-central-1)
```

---

## 9. Key Insights & Recommendations

### Code quality assessment
**Strong:** clean feature-based layering, consistent DI style, DTO boundaries, contract-first frontend, comprehensive observability and quality tooling, integration tests with real DB via Testcontainers, ADRs in place. For a *template*, the infrastructure breadth is its main value.

**Weak spots / risks:**

| # | Issue | Severity | Recommendation |
|---|---|---|---|
| 1 | **No server-side auth.** Endpoints are unprotected; only the React route guard restricts access. | 🔴 High | Add Spring Security (JWT/session) before any real use. Document clearly that auth is mocked. |
| 2 | **Hardcoded credentials** in `AuthServiceImpl` (`Admin/admin`). | 🔴 High | Replace with real identity provider; never ship hardcoded creds. |
| 3 | **`AiController.assistantThread` is a shared mutable instance field** on a singleton bean — not thread-safe and leaks one user's thread to all callers. | 🟠 Med | Make thread-per-session/request; the controller is stateful by accident. |
| 4 | **CI branch mismatch.** `maven.yml`, `npm.yml`, `sonarcloud.yml` trigger on `master`, but the repo default branch is `main`. These workflows likely never run. | 🟠 Med | Update `branches:` to `main`. |
| 5 | **`npm.yml` pins Node 14**, while the app requires Node ≥18 and the Dockerfile uses Node 18. | 🟠 Med | Align Node version to 18+. |
| 6 | **`.env` is committed** (in repo root, though `.gitignore` lists `.env`). Verify no secrets are tracked; `NEW_RELIC_KEY:<...>` placeholder uses `:` not `=`. | 🟠 Med | Confirm `.env` is untracked; fix sample syntax. |
| 7 | **Actuator fully exposed** (`include=*`) — leaks env, mappings, etc. | 🟠 Med | Restrict to `health,info,metrics,prometheus` in production. |
| 8 | **Committed build artifacts** — `api/target/` (incl. `tech-demo-0.0.2.jar`), `api/logs/*.zip`, `ui/dist/`, `.m2/repository/` appear in the tree. | 🟡 Low | Add to `.gitignore`; keeps the repo lean (currently 100M, 54k files). |
| 9 | **Dual CI systems** (GitHub Actions + CircleCI) partly overlap. | 🟡 Low | Pick one to reduce maintenance/confusion. |
| 10 | **State management drift** — workspace standard is Zustand; this UI uses Context+reducer. | 🟡 Low | Reconcile the template with the documented standard, or update the standard. |

### Security considerations
- Implement real authn/authz (items 1–2) — the single biggest gap.
- Lock down Actuator (7); ensure Prometheus endpoint isn't publicly reachable.
- OpenAI key must come from env only (verify `OpenAIRestTemplateConfig` reads `OPENAI_API_KEY`, never hardcoded).
- AI endpoints take user input via GET query param — consider POST + input validation + rate limiting (and the chat logs the message, which is partly sanitized for CRLF but still logs content).
- Keep leveraging FOSSA + Dependabot + Sonar; add OWASP Dependency-Check/Trivy (already standard per workspace `CLAUDE.md`).

### Performance opportunities
- Caching is off; enable Redisson caching for read-heavy `Person` lookups once it's needed.
- `AiController.assistant` polls with `TimeUnit.SECONDS.sleep(10)` up to 10× — a blocking 100s worst case on a request thread. Move to async / webhook / shorter backoff.
- HikariCP is well-tuned already; confirm pool size matches deployment.

### Maintainability suggestions
- Fix the CI branch/Node mismatches so the pipeline actually gates merges.
- Remove committed artifacts and trim the repo.
- Add README clarity that auth is mocked and the template's "production-readiness" checklist (security being item one).
- Expand i18n beyond `sk.json` if the template targets multilingual projects.
- Consolidate to a single CI provider and a single documented state-management approach.

---

*Analysis written to `codebase_analysis.md`.*
