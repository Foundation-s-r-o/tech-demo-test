# Plan: Evolve tech-demo DB schema + app to the civil-registry design (db/)

## Context

`db/` contains a finished **civil-registry design**: `DESIGN.md` (design + rules),
`schema.sql` (PostgreSQL DDL, 38 tables / 4 schemas, verified by `test_schema.sql` T1–T14),
`seed_codelists.sql` (6,187 official MetaIS items), `openapi.yaml` (33-operation API
contract), `import_codelists.py`. The application (`api/` + `ui/`) still runs the old demo:
flat `PERSON` table (Flyway V1–V4), simple CRUD API/UI.

**Goal:** the app's H2/Flyway schema becomes the **single** schema — evolved to the registry
design — and the app implements the **full registry API from `db/openapi.yaml`** with a
**full UI** for all areas. Afterwards `db/schema.sql`, `db/test_schema.sql`,
`db/seed_codelists.sql` are retired (superseded); `DESIGN.md`, `openapi.yaml`,
`import_codelists.py` stay (updated). `MVSR_RFO_v01.rtf` is out of scope (user removes it);
no DESIGN.md §8.1 extensions.

## User-confirmed decisions

| Decision | Choice |
|---|---|
| Target | App Flyway/H2 schema; one schema, not two |
| API surface | Implement the full registry contract now (replaces demo Person API) |
| PG-only mechanisms | App-layer equivalents; DDL kept PostgreSQL-ready with documented re-hardening delta |
| Codelists | Full 6,187-item seed as Flyway migration |
| db/ folder | Retire SQL files; keep + update docs and importer |
| Demo PERSON data | Drop, no carry-over (disposable) |
| PK strategy | **Natural PKs as designed** (person_uid varchar PK, codelist code PKs, varchar/uuid FKs per schema.sql) |
| UI depth | Full UI: persons, vital records, GDPR, audit, citizen portal |

## PostgreSQL → H2 + Spring adaptation rules

| schema.sql mechanism | App equivalent |
|---|---|
| 45 RLS policies + 7 DB roles + GUCs (`app.user_id`, `app.person_uid`…) | Spring Security roles + `@PreAuthorize` + service-layer scoping (clerk office scope, citizen own-data predicate) |
| Audit triggers + pgcrypto SHA-256 hash chain | `AuditService` (Java-computed chain; chain-head row `SELECT … FOR UPDATE`) |
| GiST `EXCLUDE USING` (bitemporal no-overlap) | Service-layer overlap checks inside write txn + pessimistic lock on person row |
| Partial unique indexes (unique among `superseded_at IS NULL`) | Service-layer uniqueness checks (same locked txn) |
| `generate_person_uid()`, check digit, `digest()`, `hmac()` | Java: `PersonUidService` (Crockford base32 + mod-32 check char), `NationalIdCryptoService` (AES-GCM + HMAC-SHA256, Bouncy Castle 1.84 already a dep; keys from env vars, fixed dev keys only in `local`/`it`/`e2e` profiles — SECURITY.md caveat) |
| PL/pgSQL `citizen_is_eligible`, `citizen_portal_uid_accessible`, `resolve_person_uid`, merge guard, no-delete trigger | Java domain services (`PortalEligibilityService`, `PersonMergeService`); hard delete simply not exposed by repositories/services |
| Type conversions | `timestamptz`→`timestamp`, `jsonb`→`clob` (Jackson 3), `uuid`→`varchar(36)` app-generated, `inet`→`varchar(45)`, `text[]`→child table/CSV varchar, `daterange @>`→explicit from/to comparisons |
| `PARTITION BY` on audit tables | Plain tables |
| Keep in DDL | 4 schemas (`CODELIST`,`REGISTRY`,`GDPR`,`AUDIT` — H2 supports `CREATE SCHEMA`), PKs/FKs/portable CHECKs, NOT NULLs, plain indexes, `V_PERSON_CURRENT` view (ANSI), audit table shapes incl. `prev_hash`/`row_hash` |

Deviations from the contract to document in `db/openapi.yaml` + DESIGN.md: session-cookie
auth instead of OAuth2 scopes (gateway concern in prod); extra `GET /api/persons/{uid}/pdf`
(registry extract, retained from demo, OpenPDF); `/api` base path.

## Current-code anchors (explored)

**Backend** (`api/src/main/java/sk/foundation/techdemo/`)
- Flyway: `src/main/resources/db/migration/V1..V4`, default location, portable ANSI,
  uppercase unquoted identifiers (`PhysicalNamingStrategyStandardImpl`), `ddl-auto=validate`.
- Base entity `infrastructure/db/IdentifiableEntity.java` (bigint identity) — registry
  entities will NOT use it (natural PKs): add new `@MappedSuperclass`/`Persistable` bases.
- Security: `auth/SecurityConfig.java` (session, CSRF cookie/header, exact-origin CORS,
  matrix hardcoded to `/api/persons*`, roles ADMIN/USER), `auth/User.java` (`USERS` table),
  `auth/LocalAdminInitializer.java` (`local`/`it` profiles seed admin/admin + user/user).
- Errors: `infrastructure/api/ApiExceptionHandler.java` (404/409/401) → migrate to
  RFC 7807 `ProblemDetail` (contract requires problem+json).
- Conventions: manual DTO mapping (no MapStruct), Criteria + JPA static metamodel
  (hibernate-jpamodelgen wired), `@ParameterObject` for search params, annotation-light
  springdoc (`springdoc.api-docs.version=openapi_3_0` — keep, generator CLI 6.0.1 can't parse 3.1).
- Tests: Surefire (unit) + Failsafe (`src/integration-test/java`, `BaseIT` profile `it`,
  in-mem H2, MockMvc + security filters); JaCoCo merged floor **0.70** (`jacoco.line-coverage.minimum`);
  Checkstyle strict at `validate`.

**Frontend** (`ui/`)
- Client: `openapi-generator-cli 6.0.1` typescript-axios from committed `ui/api-docs.json`
  → `ui/src/api/generated/`; instances in `ui/src/api/api.ts`; axios + CSRF in
  `ui/src/common/axiosClient.ts`; auth context `ui/src/common/auth/` (`Role.ts` ADMIN/USER).
- Pages `ui/src/pages/{home,login,persons}/`; routes `ui/src/components/router/routes.tsx`;
  menu `ui/src/components/menu/AppMenuTree.ts`; sidebar `ui/src/components/sidebar/`;
  i18n `ui/src/assets/translations/sk.json` (full diacritics — customer-facing).
- E2E `ui/tests/test_{persons,authentication,authz,a11y,openapi}.spec.ts`,
  login helper `ui/tests/helpers/auth.ts`, Playwright webServer starts API (`local,e2e`, :8082)
  + UI (:8080). `test_openapi.spec.ts` gates live `/v3/api-docs` vs committed `api-docs.json`.

## Role & auth model

Replace ADMIN/USER with the design roles: `CLERK`, `SUPERVISOR`, `AGENCY_READER`,
`DPO_OFFICER`, `SECURITY_AUDITOR`, `CITIZEN`.

- `USERS` table absorbs `registry.staff_user` (documented deviation — one credential store):
  add `OFFICE_CODE` (FK → `CODELIST.REGISTRY_OFFICE`, for clerk scoping) and `PERSON_UID`
  (FK → `REGISTRY.PERSON`, for citizen portal identity).
- `LocalAdminInitializer`: admin/admin → SUPERVISOR (E2E login helper keeps working),
  user/user → CLERK (Bratislava office); add e2e-only seeds: agency/agency, dpo/dpo,
  auditor/auditor, citizen/citizen (linked to a seeded person).
- `SecurityConfig` matrix rebuilt from the contract's `x-required-roles`; method-level
  `@PreAuthorize` on services; Art. 9 endpoints (religion, death medical) exclude agency;
  audit endpoints for DPO/security auditor; `/me/**` citizen-only.
- Personal-data reads require `X-Access-Purpose` (else 400/problem+json) and log to
  `AUDIT.ACCESS_LOG` with `X-Request-Id` correlation (servlet filter).
- Login failures / privilege changes → `AUDIT.SECURITY_EVENT` via Spring Security event listener.

## Implementation stages

Each stage ends green (`cd api && ./mvnw clean verify` + `cd ui && npm run lint`) and is a
natural commit/PR boundary. No git commits without explicit instruction.

### Stage 1 — Flyway migrations (schema + seed)
Files: `api/src/main/resources/db/migration/V5__registry_schema.sql`,
`V6__seed_codelists.sql`, `V7__extend_users.sql`; `db/import_codelists.py`.
- V5: `CREATE SCHEMA` ×4 + all 38 tables converted per adaptation table (natural PKs as in
  schema.sql), `V_PERSON_CURRENT` view, semantic codelist seeds (sex, address_type,
  marital/existence status, family relation, religion), `GDPR.RETENTION_POLICY` statutory
  seed, `AUDIT.CHAIN_HEAD` init row. Annotate each dropped PG mechanism with a short
  `-- PG-delta:` comment referencing the re-hardening appendix.
- V6: generated by retargeted `import_codelists.py` (plain portable INSERTs, no ON CONFLICT —
  versioned migration runs once). 6,187 items.
- V7: `ALTER TABLE USERS ADD OFFICE_CODE…, PERSON_UID…` + FKs.
- `PERSON` drop deferred to Stage 4 (code still uses it).
- IT: extend `FlywayMigrationIT` (schema objects exist, FKs enforced, seeds counted).

### Stage 2 — Backend domain + infrastructure
New packages under `sk.foundation.techdemo`: `registry/` (person aggregate: person, detail,
address, religion, representation, capacity restriction, merge), `records/` (birth,
marriage+dissolution, death+medical), `codelist/`, `gdpr/`, `auditlog/`,
`infrastructure/crypto/`.
- Entity bases: `@MappedSuperclass` for natural-key entities (`Persistable<String>`),
  codelist entities with `String @Id`; JPA metamodel regenerates automatically.
- Services: `PersonUidService`, `NationalIdCryptoService`, `AuditService` (hash chain),
  `AccessLogService` + purpose filter, `BitemporalGuard` (overlap/uniqueness checks),
  `PortalEligibilityService`, `PersonMergeService`.
- `ApiExceptionHandler` → RFC 7807 `ProblemDetail` (all endpoints).
- Role model + seeds + SecurityConfig rebuild (old `/api/persons` matrix entries stay until Stage 4).
- Unit tests for UID check digit (port T1), crypto roundtrip, chain hashing; ITs for guards.

### Stage 3 — Registry API (three sub-steps, each verified)
Controllers/DTOs per contract, annotation-light, `@ParameterObject` search objects:
- 3a persons: `POST/GET /persons`, search, `/details`, `/addresses`, `/religion`,
  `/me`, `/me/access-log`.
- 3b vital records: births/marriages/deaths + `/amendments` (supervisor + mandatory reason),
  `/dissolutions`, `/medical`.
- 3c codelists (`/codelists`, `/codelists/{codelist}/items`), GDPR (DSR CRUD + statutory
  due date, consents + withdrawal → supersede religion row, disclosures, Art. 15 export —
  itself recorded as disclosure), audit (`/audit/events`, `/audit/access-log`,
  `/audit/chain/verification`).
- ITs port test_schema.sql semantics: T2 person graph, T3 bitemporal overlap rejection,
  T4 address change + point-in-time, T5 religion consent CHECK, T6 no hard delete,
  T7 chain integrity + tamper detection, T8 office scoping, T9 minimal agency view,
  T10 codelist FKs, T12 retention seed, T13/T14 citizen own-data + eligibility +
  representation, merge guards. Full `@WithMockUser` authz matrix per endpoint.

### Stage 4 — Remove demo Person feature
- Delete `persons/` package (entity, services, controller, DTOs), demo `@Sql` fixtures;
  retarget `PersonPdfService` → registry person extract (`GET /api/persons/{uid}/pdf`,
  clerk/supervisor).
- `V8__drop_person_table.sql`.
- SecurityConfig cleanup; remove obsolete unit/ITs, keep coverage ≥ 0.70.

### Stage 5 — OpenAPI sync + UI rework
- Start API (`local`), save `http://localhost:8082/v3/api-docs` → `ui/api-docs.json`,
  `npm run generate-openapi-services`, add controller instances to `ui/src/api/api.ts`.
- `Role.ts` + role-gated menu; `AppMenuTree.ts` groups: Osoby, Matričné záznamy, GDPR,
  Audit, Portál občana (citizen). Routes in `routes.tsx`.
- Pages (reuse `FndtTable`, `FndtInputSegment`, Formik+Yup, stable `id` prefixes per E2E
  convention): person search/detail (tabs: versioned details, addresses, religion),
  person create; birth/marriage/death registration + amendment + dissolution forms;
  GDPR: DSR queue + create/update, consents + withdrawal, disclosures, export download;
  audit: events table, access-log table, chain-verification status page;
  citizen portal: `/me` composite view, own access log, file DSR.
- `sk.json`: new sections (full diacritics); every new form field gets `form.input.*` keys.
- `npm run lint`, `npm run smoke`, `npm run smoke:storybook`.

### Stage 6 — E2E suite + docs + retirement
- Rewrite/extend `ui/tests/`: `test_persons` (new flows incl. version history),
  `test_records.spec.ts`, `test_gdpr.spec.ts`, `test_audit.spec.ts`,
  `test_citizen.spec.ts`; `test_authz` → full 6-role matrix (new e2e seeds);
  `test_a11y` extended to all new pages (both Nord themes); `test_openapi` unchanged (gates drift).
- Docs: rewrite `db/DESIGN.md` (H2/app-layer reality; single schema =
  `api/src/main/resources/db/migration`; **appendix: PostgreSQL re-hardening delta** —
  RLS policies, triggers, exclusion constraints, partitioning to re-add on the PG switch);
  update `db/openapi.yaml` deviations note; update `SECURITY.md` (demo caveats: dev crypto
  keys, seeded accounts, app-layer enforcement note); update project `CLAUDE.md` overview;
  new ADR `docs/ADR/0011-civil-registry-schema-port.md`.
- Delete `db/schema.sql`, `db/test_schema.sql`, `db/seed_codelists.sql`.

## Verification

- Per stage: `cd api && ./mvnw clean verify` (Checkstyle, unit+IT, JaCoCo ≥ 0.70 — add
  tests, never lower the floor) + `cd ui && npm run lint`.
- Stage 5+: `npm run smoke`, `npm run smoke:storybook`, regenerate + commit `api-docs.json`
  and generated client together.
- Final: `cd ui && npx playwright test` (ports 8080/8082 free), then `./doctor`;
  `./scripts/security_scan.sh`; Sonar both projects + refresh `docs/` artifacts
  (Sonar JSONs, Trivy, JaCoCo) per repo convention.
- Manual spot-check: register person → birth record → amendment → citizen /me → audit
  chain verification returns intact.

## Risks / flags (up-front)

- **Scope**: multi-week build (~35 endpoints, ~40 tables, full UI + E2E). Strong
  recommendation: one PR per stage; the gate stays green at each boundary. Stages 3 and 5
  are the long poles.
- **Coverage floor 0.70** over a large new codebase makes tests ~40% of the work — planned
  as first-class, not afterthought.
- App-layer enforcement is weaker than in-DB RLS (any future non-app DB client bypasses
  scoping) — explicitly documented in SECURITY.md + DESIGN.md with the PG re-hardening path.
- Local file DBs (`./data/`) need `V5+` to apply cleanly over V1–V4; demo PERSON rows are
  dropped (user-approved).
- Session-auth vs contract OAuth2 and the PDF endpoint are recorded contract deviations.
