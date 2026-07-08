# Architecture

This document describes how `tech-demo-test` is built and why it is built that
way. It is the technical companion to the setup-focused [`README.md`](../README.md)
and the operational [`SECURITY.md`](../SECURITY.md), and it cross-references the
architecture decision records under [`docs/ADR/`](./ADR/).

`tech-demo-test` is Foundation s.r.o.'s **reference template** for new
backend + frontend projects. Its business surface is a **civil registry**
(ported from the `db/` design — see [ADR-0011](./ADR/0011-civil-registry-schema-port.md)):
a person aggregate with bitemporal identity/address/religion history, append-only
vital records (births, marriages, deaths), a GDPR workflow, and a tamper-evident
audit hash chain — so that the repository's real payload is its *shape*: the
preferred application structure, dependency baseline, testing strategy, and the
CI / quality / security gate that every new project inherits. It is a **rolling
demo** on one branch (`main`), not a versioned product.

> **Scope & maintenance rule.** This document describes what is *stable* —
> structure, layering, patterns, boundaries, and decisions. It deliberately does
> **not** restate facts that change with ordinary code evolution; those are
> referenced at their authoritative source instead of copied here, so the doc
> cannot silently go stale:
>
> | Volatile fact | Lives in (authoritative source) |
> | ------------- | ------------------------------- |
> | Endpoints, request/response shapes | OpenAPI spec → `/v3/api-docs`, Swagger UI `/swagger-ui.html` |
> | Exact dependency versions | `api/pom.xml`, `ui/package.json` (+ README stack snapshot) |
> | Authorization matrix | controller `@PreAuthorize` expressions + [`SECURITY.md`](../SECURITY.md) |
> | DB schema / migrations | `api/src/main/resources/db/migration/` |
> | Tunable config values | `api/src/main/resources/application*.properties` |
>
> When you add a section, prefer a one-line reference over a copied table if the
> underlying fact is generated or changes per commit.

---

## Design Principles

These are the load-bearing decisions. Everything else in the document follows
from them.

### Fast local inner loop is paramount

Local development must run with **two terminals and nothing else** — no Docker,
no external services, no `.env` file. Speed comes from *removing* infrastructure,
not from loosening the build gate. The database is embedded H2 in file mode; the
API and the SPA each run from their own dev server. Anything that would slow the
inner loop (Docker requirements, slow tests, heavyweight infra) is treated as a
regression and flagged. ([ADR-0002](./ADR/0002-minimal-docker-free-stack.md))

### The build gate is strict and is the safety net

With a deliberately small test surface, the gate — Checkstyle
(`failOnViolation=true`), unit + integration tests, JaCoCo on `verify`, ESLint +
`tsc --noEmit`, and CircleCI — is the main thing keeping `main` releasable. The
rule is *verify before every commit*; the gate is never relaxed for speed. See
[Quality & Security Gate](#quality--security-gate). ([ADR-0008](./ADR/0008-strict-quality-security-gate.md))

### Portability over vendor lock-in

H2 is the *local* database, not the architecture. Flyway migrations are written
in **portable ANSI SQL**; the datasource and Hibernate dialect are
**profile-driven**, never hardcoded into shared config. Entity and column names
are explicit and unquoted (`PhysicalNamingStrategyStandardImpl`,
`globally_quoted_identifiers=false`) so the same schema runs on PostgreSQL when a
`prod` profile arrives. The PostgreSQL swap is meant to be a non-event.
([ADR-0003](./ADR/0003-embedded-h2-postgres-later.md))

### Security is configured correctly, in the correct layer — and demo relaxations are explicit

Authentication, authorization, CORS, CSRF, session/cookie flags, and security
headers are configured completely and **in the right layer** (e.g. CORS lives in
the security filter chain ahead of authorization, not in the MVC layer — see
[`WebConfiguration`](../api/src/main/java/sk/foundation/techdemo/infrastructure/web/WebConfiguration.java)).
Where the demo deliberately weakens something for local speed, the relaxation is
scoped to an explicit profile and documented in both the source and
[`SECURITY.md`](../SECURITY.md). The known `admin`/`admin` account exists **only**
under the explicit `local` and integration-test (`it`) profiles.
([ADR-0004](./ADR/0004-spring-security-session-auth.md))

### Conventions are demonstrated, not just documented

The repository is itself the example. Clean layering, constructor injection,
the repository pattern, OpenAPI-generated clients, and the ADR habit are all
present so a new project can be cloned from a working baseline rather than a
checklist.

### No supported deployment by design

There is intentionally **no** container or cloud deployment definition. The
stale Docker / Compose / ECR / Elastic Beanstalk scaffolding was removed because
a real deployment must start from a documented threat model, a `prod` profile,
external secret management, TLS, and an explicit identity-provisioning design —
none of which a template should pretend to ship. See
[Deferred Production Path](#deferred-production-path).
([ADR-0007](./ADR/0007-no-supported-deployment.md))

---

## Tech Stack

The **architectural choices** (major lines) are below; they are decisions, not
moving targets. **Exact pinned versions are deliberately not restated here** —
they change with every Dependabot bump. Read them from `api/pom.xml`,
`ui/package.json`, and the stack snapshot in [`README.md`](../README.md).

| Layer | Choice | Why it's here |
| ----- | ------ | ------------- |
| Backend language | Java 21 LTS | Pinned in [`api/.sdkmanrc`](../api/.sdkmanrc) |
| Framework | Spring Boot 4 (MVC, Security, Data JPA) | Stay on the 4.0 line |
| ORM | Hibernate + HikariCP | Portable naming strategy |
| Migrations | Flyway, portable ANSI SQL | H2 today, PostgreSQL later ([ADR-0003](./ADR/0003-embedded-h2-postgres-later.md)) |
| Database | Embedded H2 (local) → PostgreSQL (`prod`, deferred) | No Docker locally |
| PDF | OpenPDF | `GET /api/persons/{id}/pdf` |
| API docs | springdoc OpenAPI + Swagger UI | The endpoint contract ([see below](#http-surface)) |
| Observability | Actuator + Micrometer (Prometheus registry) | Health-only public; full stack deferred ([ADR-0009](./ADR/0009-observability-actuator-micrometer.md)) |
| Frontend | React 19 + TypeScript (strict) SPA | ([ADR-0005](./ADR/0005-react-typescript-spa.md)) |
| Routing / forms / state | React Router, Formik + Yup, React context | No global store at this size |
| Styling / i18n | Bootstrap + SCSS (BEM), i18next (Slovak) | — |
| HTTP client | Axios + OpenAPI-generated client | ([ADR-0006](./ADR/0006-rest-openapi-generated-client.md)) |
| Bundler | webpack 5 | — |
| Crypto | Bouncy Castle | Explicit CVE pins |

---

## System Overview

The application is a **modular monolith**: a React single-page application talks
to a Spring Boot JSON API over a session cookie. During development the two are
separate dev servers (UI → API). The backend also carries an SPA fallback
resource handler, so a future packaged build can serve the static assets and
client-side routes from the one application.

```mermaid
flowchart TD
    subgraph Browser
        SPA[React SPA<br/>webpack dev server]
        SPA --> Router[React Router<br/>protected routes]
        Router --> AuthCtx[AuthProvider +<br/>session-aware Axios]
        AuthCtx --> Client[generated TS API client]
    end

    Client -->|"credentialed HTTPS<br/>cookie + X-XSRF-TOKEN"| Chain

    subgraph API["Spring Boot API"]
        Chain[Security filter chain<br/>CORS → CSRF → authz]
        Chain --> Ctrl[REST controllers<br/>persons / records / gdpr / audit / agency / me]
        Ctrl --> ApiSvc[API services<br/>DTO mapping, @PreAuthorize, access log]
        ApiSvc --> DomSvc[domain services +<br/>JPA repositories]
        DomSvc --> ORM[Hibernate / HikariCP]
        ORM --> DB[(H2 file DB)]
    end
```

The decisive split is **API services vs. domain services**: API-facing classes
(`*ApiService`) own DTO projection, paging, authorization purpose, access-logging
and not-found/conflict semantics; domain classes (`BitemporalGuard`,
`AuditService`, `PersonMergeService`, JPA `*Repository`) own the invariants,
entity mutation and persistence. The controller never touches an entity directly.

---

## Repository Layout

```text
tech-demo-test/
├── api/                     # Spring Boot backend (Maven)
│   ├── .sdkmanrc            # pins Java 21 LTS
│   ├── checkstyle/          # checkstyle.xml (hard gate) + IDE formatter rules
│   └── src/main/java/sk/foundation/techdemo/
│       ├── auth/            # Spring Security session login + six registry roles
│       ├── registry/        # person aggregate: bitemporal detail/address/religion,
│       │                   #   BitemporalGuard, merge, agency view, PDF, /me
│       ├── records/         # append-only vital records (births/marriages/deaths)
│       ├── gdpr/            # DSR, consent, disclosure register, Art. 15 export
│       ├── auditlog/        # SHA-256 audit hash chain, access log, security events
│       ├── codelist/        # reference codelists (read-only)
│       └── infrastructure/  # cross-cutting: api/ (RFC 7807), db/, crypto/, web/, logging/
├── ui/                      # React SPA (webpack/npm)
│   └── src/
│       ├── api/             # generated OpenAPI client + shared Axios instance
│       ├── common/          # auth, axiosClient, i18n, persistence, helpers
│       ├── components/      # router, layout, sidebar, form, shared UI primitives, modals
│       ├── pages/           # login, home, persons, records, gdpr, audit, me
│       └── stories/         # Storybook stories
├── docs/
│   ├── ARCHITECTURE.md      # this document
│   ├── ADR/                 # MADR architecture decision records
│   ├── UPGRADE_PLAN.md      # Boot 4 / Java 21 history + deferred phases
│   └── QUALITY_REPORT.md
├── scripts/security_scan.sh # Semgrep (metrics off) + Trivy + npm audit
├── doctor                   # build + lint + tests + FOSSA health check
├── README.md  PROJECT_OVERVIEW.md  SECURITY.md  CLAUDE.md  # overview file is a retired pointer
└── .github/workflows/fossa.yml  +  .circleci/config.yml
```

Both sides are organized **by feature** (`registry`, `records`, `gdpr`,
`auditlog`, `codelist`, `auth`) with a shared `infrastructure` / `common` layer
for cross-cutting concerns — the layout a new project should copy.

---

## Backend

Backend source lives under
[`api/src/main/java/sk/foundation/techdemo`](../api/src/main/java/sk/foundation/techdemo).

### Application bootstrap

- [`TechDemoApplication`](../api/src/main/java/sk/foundation/techdemo/TechDemoApplication.java)
  is the Spring Boot entry point;
  [`TechDemoApplicationConfiguration`](../api/src/main/java/sk/foundation/techdemo/TechDemoApplicationConfiguration.java)
  holds shared beans.
- Profiles drive everything environment-specific: default (no known account),
  `local` / `it` (creates `admin`/`admin`), `prod` (Secure cookie, Swagger off,
  origins from an env var), and `e2e` (disposable in-memory DB). The exact
  property values per profile live in `application*.properties`.

### Auth feature (`auth/`)

Session-based Spring Security. The
[`SecurityConfig`](../api/src/main/java/sk/foundation/techdemo/auth/SecurityConfig.java)
filter chain is the authority for the whole request lifecycle:

- **CORS** registered in the chain (`.cors(...)`) — credentialed, exact-origin
  only, so preflight is handled *before* authorization. `WebConfiguration`
  deliberately does **not** also define CORS.
- **CSRF** via a cookie/header token — the SPA fetches a token from
  `GET /api/auth/csrf` and echoes it in `X-XSRF-TOKEN`.
- **Session fixation** defeated by session-ID rotation on login.
- **Authorization** enforced by request matchers (the matrix is authoritative in
  `SecurityConfig` and documented in [`SECURITY.md`](../SECURITY.md) — not copied
  here, since it changes with the matchers).
- **Security headers**: a defense-in-depth CSP on API-origin responses
  (`script-src 'self'`, no `unsafe-eval`), `X-Frame-Options: DENY`,
  `Referrer-Policy`, and a `Permissions-Policy`.
- **401 entry point** — the API never redirects to a login page.

Supporting classes:
[`AuthController`](../api/src/main/java/sk/foundation/techdemo/auth/AuthController.java)
(JSON login, current-user, logout, CSRF),
[`AppUserDetailsService`](../api/src/main/java/sk/foundation/techdemo/auth/AppUserDetailsService.java)
+ [`UserRepository`](../api/src/main/java/sk/foundation/techdemo/auth/UserRepository.java)
(DB-backed users, BCrypt),
[`LocalAdminInitializer`](../api/src/main/java/sk/foundation/techdemo/auth/LocalAdminInitializer.java)
(recreates the seeded registry accounts — `admin`, `user`, `dpo`, `auditor`,
`agency`, `citizen` — **only** under `local`/`it`), and
[`SecurityEventListener`](../api/src/main/java/sk/foundation/techdemo/auth/SecurityEventListener.java)
(persists authentication-failure events to `AUDIT.SECURITY_EVENT`; best-effort, so
an audit-write hiccup never changes the authentication outcome — NIS2 story).

> **Authorization model (stable part):** authorities are `ROLE_<name>` for the
> **six registry roles** (`CLERK`, `SUPERVISOR`, `AGENCY_READER`, `DPO_OFFICER`,
> `SECURITY_AUDITOR`, `CITIZEN`). Authorization is **per-operation via
> `@PreAuthorize`** on the controllers (not URL matchers), with service-layer
> office scoping for clerks; personal-data reads additionally require the
> `X-Access-Purpose` header and are access-logged. The frontend mirrors the role
> gating for UX only and is never the enforcement point. The exact per-endpoint
> matrix lives in the controllers and [`SECURITY.md`](../SECURITY.md#authorization-matrix);
> change the `@PreAuthorize` expressions and the negative tests in the API
> integration tests together.

### Registry domain (`registry/`, `records/`, `gdpr/`, `auditlog/`, `codelist/`)

The reference layering is **controller → API service → domain services + JPA
repositories**; class names below are illustrative entry points, not an
inventory. The domain follows the ported design ([ADR-0011](./ADR/0011-civil-registry-schema-port.md)),
with PostgreSQL-only mechanisms (RLS, triggers, GiST exclusion) re-expressed as
application-layer services.

| Area | Representative classes | Responsibility |
| ---- | ---------------------- | -------------- |
| Person aggregate | `PersonController`, `RegistryPersonApiService` | create + composite/current reads, search, bitemporal writes of detail/address/religion, PDF, agency view |
| Bitemporal invariant | `BitemporalGuard` | at most one current-belief version per key per point in valid time (app-layer GiST-exclusion replacement) |
| Identity merge | `PersonMergeService` | duplicate→primary merge under pessimistic person-row locks; single-hop resolution |
| Vital records | `VitalRecordsApiService` | append-only births/marriages/deaths, amendments (supervisor + reason), dissolution, Art. 9 medical |
| GDPR | `GdprApiService` | DSR lifecycle, consent + withdrawal, disclosure register, full Art. 15 export |
| Audit | `AuditService`, `AccessLogService` | SHA-256 hash chain (append + `verifyChain` with a chain-head tail check), read-access log |
| Codelists | `CodelistService` | whitelisted read-only reference lists |
| Agency | `AgencyController` (`agencyView`) | minimal current-state projection for `AGENCY_READER` |
| Citizen portal | `MeController` | own data + own access log, portal-eligible only |

**Bitemporal write flow (`RegistryPersonApiService`).** Each detail/address/
religion write is *supersede-and-close* (design §4): under the person-row lock, an
open-ended insert that starts after the current open row re-records the old belief
as a bounded copy and transaction-time supersedes the original; `BitemporalGuard`
then rejects any residual valid-time overlap before the new row is saved. `search`
uses a valid-time-filtered `searchCurrent` query so stale history copies never
surface. Every write appends to the audit chain; every personal-data read appends
to the access log.

Request/response DTOs keep the wire contract decoupled from the JPA entities;
their exact fields are part of the [OpenAPI contract](#http-surface).

### Infrastructure (`infrastructure/`)

- `api/` — `ApiExceptionHandler` centralizes application + persistence failures
  as **RFC 7807 `application/problem+json`** ([ADR-0012](./ADR/0012-rfc7807-error-contract.md)):
  `ConflictException`→409, `EntityNotFound`→404, `DataIntegrityViolationException`→400
  (generic, no SQL internals), auth→401/403.
- `db/` — the two entity bases every JPA entity extends: `IdentifiableEntity`
  (surrogate id) and `NaturalKeyEntity` (natural-key tables).
- `crypto/` — `NationalIdCryptoService` (AES-GCM encryption + HMAC lookup key,
  Bouncy Castle; dev keys under `local`/`it`, see [`SECURITY.md`](../SECURITY.md)).
- `web/` — `WebConfiguration` (SPA history fallback resource handler; CORS is
  *not* here — see above).
- `logging/` — `LoggingConfiguration` + `logback-spring.xml`.

### Configuration & data model

`application.properties` (and the `prod` / `e2e` overlays) own the tunables —
H2 file-mode datasource, HikariCP pool, JPA (`open-in-view=false`,
`ddl-auto=validate` so **Flyway owns the schema**), Flyway, health-only Actuator,
and session-cookie security. Specific values are not restated here; read the
properties file.

The schema is defined entirely by **Flyway migrations** in
[`db/migration/`](../api/src/main/resources/db/migration) (`V1`..`V8`) — the
migration set is the authoritative, evolving record, so it is not transcribed
here. The architecturally relevant invariants: natural keys as designed;
bitemporal `valid_to > valid_from` and one-current-version constraints; the audit
hash chain; migrations carry no H2-specific SQL (portability,
[ADR-0003](./ADR/0003-embedded-h2-postgres-later.md)), with each dropped
PostgreSQL mechanism marked `-- PG-delta:` in `V5`; and no known account is seeded
into the schema — the registry accounts exist only at runtime via
`LocalAdminInitializer` under `local`/`it`. See [`db/DESIGN.md` §12](../db/DESIGN.md)
for the enforcement mapping and [`SECURITY.md`](../SECURITY.md) for provisioning.

---

## Frontend

Frontend source lives under [`ui/src`](../ui/src).

### Bootstrap & routing

[`index.tsx`](../ui/src/index.tsx) mounts the React root and installs
`BrowserRouter`, the auth context, the modals context, Bootstrap, and global
SCSS. [`components/router/routes.tsx`](../ui/src/components/router/routes.tsx)
defines every route statically (login, root/home, person search/create/detail,
vital-record registration, GDPR, audit, and the citizen `/me` portal) and wraps
the protected ones with `withRequireAuth`; a persistent collapsible sidebar
filters menu entries by the authenticated role.

```mermaid
flowchart LR
    A[index.tsx] --> B[BrowserRouter]
    B --> C[ContextProviders<br/>Auth + Modals]
    C --> D{routes.tsx}
    D -->|/login| E[LoginPage]
    D -->|protected| F[RequireAuth]
    F -->|authenticated| G[Home / Persons pages]
    F -->|not authed| E
```

### Auth flow

[`AuthProvider`](../ui/src/common/auth/AuthProvider.tsx) restores persisted user
state by validating the live backend session against the current-user endpoint on
load — the SPA trusts the server session, not localStorage. `RequireAuth`
redirects unauthenticated users to `/login`. `useAuth`, a reducer, and typed
`Role` / `User` models complete the feature.

### API client & cross-cutting

- [`common/axiosClient.ts`](../ui/src/common/axiosClient.ts) is the shared Axios
  instance: `withCredentials`, strips empty query params, serializes dates, and
  carries the CSRF header.
- [`api/generated/`](../ui/src/api/generated) holds the OpenAPI-generated Axios
  client (regenerated via `npm run generate-openapi-services`); `api/api.ts`
  injects the shared instance into it. **Generated code is not hand-edited.**
- `common/` also holds i18n, persistence, Yup config, and validation helpers.

### UI composition

- `components/layout/`, `header/`, `footer/`, `menu/` — the app shell.
- `components/form/` + `components/shared/` — reusable form fields, a generic
  `Table` (with its own context + reducer + `useFndtTable` hook), modals, and
  buttons. These are the components exercised by Storybook (`stories/`).
- `pages/` — the registry feature pages: `persons/` (search/create/detail),
  `records/` (vital-record registration), `gdpr/`, `audit/`, and `me/` — each
  binding the shared `Table` / Formik+Yup `Form` primitives.

Styling is Bootstrap + SCSS with BEM naming; Slovak strings live in
`assets/translations/sk.json`.

---

## HTTP Surface

The API contract — every endpoint, its parameters, and its request/response
shapes — is **generated from the source** and published as OpenAPI. It is not
duplicated here, because a hand-copied endpoint table goes stale the moment a
controller changes:

- **OpenAPI JSON:** `/v3/api-docs`
- **Swagger UI:** `/swagger-ui.html` (both disabled under the `prod` profile)
- **Source of truth:** the springdoc-annotated controllers under
  [`auth/`](../api/src/main/java/sk/foundation/techdemo/auth) and the feature
  `api/` packages (`registry/api`, `records/api`, `gdpr/api`, `auditlog/api`,
  `codelist`)
- The frontend's typed client is generated from this spec
  (`npm run generate-openapi-services`, [ADR-0006](./ADR/0006-rest-openapi-generated-client.md))

Architecturally, the surface is the session lifecycle root `/api/auth/*` (CSRF,
login, current-user, logout) plus the registry feature roots — `/api/persons/*`
(bitemporal person aggregate + PDF), `/api/{births,marriages,deaths}/*` (vital
records), `/api/gdpr/*`, `/api/audit/*`, `/api/codelists/*`, `/api/agency/persons/*`
(minimal agency view), and `/api/me/*` (citizen portal) — plus health-only
`/actuator/health`. Authorization per endpoint is governed by `@PreAuthorize` on
the controllers (see [Auth feature](#auth-feature-auth)).

---

## Data Flow

### Login handshake

```mermaid
sequenceDiagram
    participant U as SPA
    participant A as API (SecurityConfig chain)
    U->>A: GET CSRF token
    A-->>U: XSRF-TOKEN cookie + token body
    U->>A: POST login (creds + X-XSRF-TOKEN)
    A->>A: authenticate (BCrypt) → rotate session ID
    A-->>U: 200 + user + session cookie
    U->>A: GET current-user (cookie)
    A-->>U: 200 current user → AuthProvider populates context
```

### Person read (search / composite)

Search/detail page → generated client → `PersonController` (`@PreAuthorize`,
`X-Access-Purpose` required) → `RegistryPersonApiService` (`searchCurrent` /
current-in-valid-time composite projection) → paged/composite DTO. The read
appends one `ACCESS_LOG` row per subject; no entity crosses the controller
boundary.

### Person write (bitemporal versioning)

`Form` (Formik+Yup) → generated client (role-gated, CSRF header) →
`RegistryPersonApiService`: acquire the person-row pessimistic lock →
supersede-and-close the open version → `BitemporalGuard` rejects any residual
valid-time overlap → save the new version → append an `AUDIT` chain entry. Errors
funnel through `ApiExceptionHandler` as RFC 7807 problem+json.

### Vital-record amendment

`VitalRecordsApiService` rejects amending an already-superseded version (409) and
re-checks registry-number uniqueness excluding the amended record's own chain,
then supersedes the prior version and appends the new one with a mandatory reason
and supervisor approval.

---

## Quality & Security Gate

The gate is the point of the template. The **authoritative, current behaviour**
(scanners, where each runs, and whether it blocks) lives in
[`SECURITY.md`](../SECURITY.md#security-tooling-in-use) and the gate tables in
[`README.md`](../README.md) — not copied here. Architecturally:

- **Blocking** (keeps `main` releasable): CircleCI (`mvn verify` + frontend lint
  & build), Checkstyle, JaCoCo coverage check inside `mvn verify`, ESLint +
  `tsc --noEmit`, GitGuardian secret scan.
- **Advisory / report-only**: FOSSA, CodeQL, Dependabot, Trivy, Semgrep,
  SonarQube. ([ADR-0008](./ADR/0008-strict-quality-security-gate.md))

The **test harness** is layered ([ADR-0010](./ADR/0010-test-harness-playwright-e2e.md)):
backend unit tests (Surefire) and integration tests on in-memory H2 (Failsafe)
run inside `mvn verify`; Playwright provides a backend-free app smoke, a
Storybook smoke, and a **full E2E suite** (`ui/tests/test_*.spec.ts`) that
auto-starts both app servers and covers login/logout, main/person pages,
birth-record office scoping, GDPR DSR, audit-chain verification, citizen `/me`,
agency authorization, accessibility smoke, and OpenAPI drift. Every Playwright
test saves a screenshot (pass or fail) plus console/trace captures. `./doctor`
executes the whole harness — build, lint,
`mvn verify`, all Playwright layers, security scans, SonarQube (skipped with a
warning when unavailable), FOSSA — and is the single "everything passed"
command; per-layer commands for the fast inner loop are in
[`CLAUDE.md`](../CLAUDE.md#verifying-code-changes).

Two gotchas worth keeping in this doc because they have bitten before:

- **Integration tests run on in-memory H2 only — no Docker / Testcontainers.**
  If anything tries to start a container during `verify`, that is a regression.
- **Trivy needs an anonymous OCI pull.** Without Docker Desktop the
  `docker-credential-desktop` helper is missing and the DB download FATAL-errors,
  but `--output` leaves the *previous* report in place — a stale file looks like
  a fresh pass. Neutralise the Docker config first
  (`export DOCKER_CONFIG=$(mktemp -d)`); `security_scan.sh` already does this.

Verification depth is chosen by net lines changed — see
[`CLAUDE.md`](../CLAUDE.md#verifying-code-changes). Touching `ui/src/common/auth/*`,
`axiosClient.ts`, `ui/src/api/*`, or backend `auth/` / `infrastructure/` always
escalates to the full suite regardless of size.

---

## Architecture Decision Records

Decisions are captured as **MADR** records under [`docs/ADR/`](./ADR/) — see
[ADR-0000](./ADR/0000-use-markdown-any-decision-records.md) for why that format,
and [`adr-template.md`](./ADR/adr-template.md) to start a new one. Every
load-bearing decision in this document is backed by an ADR:

| ADR | Decision | Drives section |
| --- | -------- | -------------- |
| [0000](./ADR/0000-use-markdown-any-decision-records.md) | Use MADR for decision records | — |
| [0001](./ADR/0001-using-source-repo.md) | Source repository: GitHub | [Quality & Security Gate](#quality--security-gate) |
| [0002](./ADR/0002-minimal-docker-free-stack.md) | Minimal, Docker-free local stack | [Design Principles](#design-principles), [System Overview](#system-overview) |
| [0003](./ADR/0003-embedded-h2-postgres-later.md) | Embedded H2 now, PostgreSQL later | [Configuration & data model](#configuration--data-model) |
| [0004](./ADR/0004-spring-security-session-auth.md) | Spring Security session login | [Auth feature](#auth-feature-auth) |
| [0005](./ADR/0005-react-typescript-spa.md) | React + TypeScript SPA | [Frontend](#frontend) |
| [0006](./ADR/0006-rest-openapi-generated-client.md) | REST + code-first OpenAPI client | [API client](#api-client--cross-cutting) |
| [0007](./ADR/0007-no-supported-deployment.md) | No supported deployment by design | [Deferred Production Path](#deferred-production-path) |
| [0008](./ADR/0008-strict-quality-security-gate.md) | Strict quality & security gate | [Quality & Security Gate](#quality--security-gate) |
| [0009](./ADR/0009-observability-actuator-micrometer.md) | Actuator + Micrometer observability | [Tech Stack](#tech-stack) |
| [0010](./ADR/0010-test-harness-playwright-e2e.md) | Layered test harness; `./doctor` as entry point | [Quality & Security Gate](#quality--security-gate) |
| [0011](./ADR/0011-civil-registry-schema-port.md) | Civil-registry schema port with app-layer enforcement | [System Overview](#system-overview), [Backend](#backend) |
| [0012](./ADR/0012-rfc7807-error-contract.md) | RFC 7807 API error contract | [Infrastructure](#infrastructure-infrastructure) |

When you make a decision worth defending later (a stack pin, a security
relaxation, a deferred phase), record it here rather than only in a commit
message or a chat. Changing an accepted decision means a *new* superseding ADR,
not an edit to the old one.

### Source-gate precedence

Where this document, an ADR, or any prose doc disagrees with the running source
or configuration, **the source / config wins** (consistent with
[`SECURITY.md`](../SECURITY.md)). The prose is then corrected, or the config is
fixed if it is the defect — the conflict is never left open. Conflicts found and
resolved while writing these ADRs are listed in
[ADR-0008 → Validation](./ADR/0008-strict-quality-security-gate.md); both have
since been resolved in favour of the source (the CircleCI `npm ci` command is
correct — the lockfile now carries all `@parcel/watcher` platform entries — and
`CLAUDE.md` was corrected to match).

---

## Deferred Production Path

This template stops where a real deployment must begin. The documented next
steps (full detail in [`docs/UPGRADE_PLAN.md`](./UPGRADE_PLAN.md) and
[`SECURITY.md`](../SECURITY.md#future-hardening-deferred--see-docsupgrade_planmd)):

1. **DB → PostgreSQL** via a `prod` profile. Migrations are already portable ANSI
   SQL, so the swap is meant to be a non-event.
2. **MFA** via Spring Security 7's built-in support (one-time tokens, passkeys /
   WebAuthn, TOTP) — added in-app, no external IdP required. An external IdP
   (Keycloak / OIDC) is optional, for SSO / federation / centralized lifecycle.
3. **Platform controls** — TLS termination, login throttling / rate limiting,
   audit logging, and secret management (Vault / cloud KMS; never `.env`).
4. **Observability → OpenTelemetry + Prometheus + Loki + Jaeger.** Today only
   Actuator + Micrometer are wired.
5. **Packaging** and a real deployment definition — only once a threat model and
   target platform exist.
6. **Coverage ratchet** — the backend minimum is already enforced by
   `jacoco:check`; lift the floor toward the Sonar target as the business surface
   grows.

Known open residual gaps, especially login throttling, are tracked in
[`SECURITY.md`](../SECURITY.md#known-residual-gaps), not only in dated remediation
reports.

---

## Further Reading

- [`README.md`](../README.md) — setup, commands, stack snapshot.
- [`CLAUDE.md`](../CLAUDE.md) — development guidelines and verification recipe.
- [`SECURITY.md`](../SECURITY.md) — demo caveats, controls, hardening checklist.
- [`docs/UPGRADE_PLAN.md`](./UPGRADE_PLAN.md) — migration history and deferred phases.
- [`docs/ADR/`](./ADR/) — architecture decision records.
