---
status: accepted
date: 2026-07-07
deciders: Foundation engineering + CTO
---
# Port the civil-registry design into the app as its single schema (app-layer enforcement)

## Context and Problem Statement

`db/` held a finished civil-registry design (`DESIGN.md`, PostgreSQL `schema.sql` — 38 tables /
4 schemas, bitemporal history, tamper-evident audit, RLS, GDPR/NIS2 structures) plus a 33-operation
API contract (`openapi.yaml`) and a 6 187-item MetaIS codelist seed. The running app, however, was
still the demo flat `PERSON` CRUD. How do we evolve the app to *be* the registry design while
keeping the Docker-free H2 inner loop ([ADR-0002](0002-minimal-docker-free-stack.md),
[ADR-0003](0003-embedded-h2-postgres-later.md)) and the strict gate
([ADR-0008](0008-strict-quality-security-gate.md)) green?

## Decision Drivers

* One authoritative schema — the app's Flyway/H2 schema *is* the design, not a second copy.
* Keep the fast, Docker-free inner loop; migrations portable ANSI so the PostgreSQL switch stays a
  config change.
* The design's PostgreSQL-only mechanisms (RLS, roles, triggers, pgcrypto, GiST exclusion,
  partitioning) are not expressible in portable H2 DDL.
* Security must not silently weaken — where a DB control is dropped, an app-layer control replaces
  it, and the gap is documented.

## Considered Options

* **Port to the app's Flyway/H2 schema, enforce PostgreSQL-only mechanisms in the application
  layer**, keep DDL PostgreSQL-ready with a documented re-hardening delta.
* Run two schemas (keep `schema.sql` for PostgreSQL, a separate H2 one for the app) — divergence
  risk, rejected.
* Require Docker + PostgreSQL locally to keep RLS/triggers — breaks the inner-loop driver, rejected.

## Decision Outcome

Chosen option: **port to the app's Flyway/H2 schema with application-layer enforcement**.

* **Schema**: migrations `V5` (schemas + 35 tables + `V_PERSON_CURRENT` + semantic seeds), `V6`
  (6 167 codelist items from `import_codelists.py`), `V7` (extend `USERS`), `V8` (drop demo
  `PERSON`). Natural keys as designed; portable ANSI types; each dropped PostgreSQL mechanism marked
  `-- PG-delta:` in `V5`.
* **Domain + services**: `PersonUidService`, `NationalIdCryptoService` (AES-GCM + HMAC, Bouncy
  Castle), `AuditService` (Java SHA-256 hash chain), `AccessLogService`, `BitemporalGuard`,
  `PortalEligibilityService`, `PersonMergeService`.
* **API**: the full registry contract — persons + bitemporal details/addresses/religion, vital
  records (register/amend/dissolution/medical), codelists, GDPR (DSR/consent/disclosure/Art. 15
  export), audit (events/access-log/chain verification), citizen `/me`. Per-operation `@PreAuthorize`
  (six registry roles) + service-layer office scoping; personal reads require `X-Access-Purpose`.
* **Enforcement mapping and the PostgreSQL re-hardening delta** are documented in
  [`db/DESIGN.md` §12](../../db/DESIGN.md); demo-mode caveats (app-layer enforcement, dev crypto
  keys, seeded accounts) in [`SECURITY.md`](../../SECURITY.md).
* The demo `persons/` feature was removed together with 3a (it owned `/api/persons`); `schema.sql`,
  `test_schema.sql`, and `seed_codelists.sql` are retired.

### Consequences

* **Positive**: single source of truth; inner loop stays Docker-free; the design is now executable
  and tested — the `test_schema.sql` semantics T1–T13 (including bitemporal-overlap rejection,
  point-in-time selection, and the minimal AGENCY_READER view) are ported to integration tests.
  T14 (representative portal access) is **not** ported — an accepted deviation (`/me` is
  own-data-only), recorded in `db/openapi.yaml`.
* **Negative / accepted**: app-layer enforcement is weaker than in-DB RLS — a non-app database
  client bypasses scoping and tamper-evidence. This is explicitly documented with the re-hardening
  path; deviations from the contract (session auth vs OAuth2 scopes, `/api` base path, the retained
  PDF endpoint) are recorded in `db/openapi.yaml`.
* Session auth ([ADR-0004](0004-spring-security-session-auth.md)) and the generated OpenAPI client
  ([ADR-0006](0006-rest-openapi-generated-client.md)) are unchanged; the contract just grows.
