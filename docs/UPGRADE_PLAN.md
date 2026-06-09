# Backend Upgrade Plan — tech-demo

**Status:** Draft for review
**Author:** Jozef Antony (with Claude Code)
**Date:** 2026-05-27
**Scope:** `api/` (Spring Boot backend) only. Frontend (`ui/`) touched only where the auth contract changes.

---

## 0. Status — COMPLETE (2026-05-27)

Phases 0–7 executed and green. Backend now runs **Spring Boot 4.0.6 on Java 21 LTS** (project default; also verified green on Java 25), embedded **H2** (no Docker), **Spring Security** session login (`admin/admin`), and **OpenPDF**. Final gate: `./mvnw clean verify` → **20 tests pass** (7 unit + 13 integration), 0 Checkstyle violations; Trivy → **0 HIGH/CRITICAL** (Tomcat bumped 11.0.21→11.0.22); UI lint + app smoke green.

| Phase | Result |
|---|---|
| 0 Test net | ✅ characterization tests added; green baseline |
| 1 Remove Redis/OpenAI/Sleuth | ✅ removed; Actuator+Micrometer only |
| 2 H2 local DB | ✅ Docker-free ITs; portable migration |
| 3 Spring Security login | ✅ admin/admin seeded; session auth E2E |
| 4 OpenPDF | ✅ `/api/persons/{id}/pdf` |
| 5 Java 21 | ✅ |
| 6 Boot 4.0.6 + Java 25 | ✅ (see migration notes below) |
| 7 Consolidation | ✅ docs + security scan |
| 8 OTel+Prometheus | ⏸ deferred (post-jump, when needed) |
| 9 PostgreSQL | ⏸ deferred (production) |

**Build requirement:** Java 21 LTS — project default pinned in `api/.sdkmanrc` (`sdk env`). Boot 4 also builds on Java 25; bump `<java.version>` to switch. Global SDKMAN default left unchanged (workspace has Java 8/11 projects).

**Boot 4 migration breakages hit & fixed (reference for other projects):**
- `@EntityScan` → `org.springframework.boot.persistence.autoconfigure`
- Hibernate 7 removed `CamelCaseToUnderscoresNamingStrategy.isCaseInsensitive` → use `PhysicalNamingStrategyStandardImpl`
- Test slices split into module starters (`spring-boot-starter-webmvc-test`, `-data-jpa-test`)
- Legacy `hibernate-types-60` incompatible with Hibernate 7 → removed; `HibernateRepository.persist()` → `JpaRepository.save()`
- `hibernate-jpamodelgen` groupId `org.hibernate` → `org.hibernate.orm`
- Flyway autoconfig modularized → `spring-boot-starter-flyway`
- **Jackson 3**: `com.fasterxml.jackson` → `tools.jackson`; needs `spring-boot-starter-json`
- `@MockBean` → `@MockitoBean`; MockMvc security needs explicit `springSecurity()`
- Latent DTO bug exposed: `@Max` on String → `@Size`; `@NotNull` on nullable column removed
- JaCoCo 0.8.12 → 0.8.14 (Java 25 class file 69); Tomcat 11.0.21 → 11.0.22 (CVEs)

## 1. TL;DR

We are taking the backend from **Spring Boot 3.4.13 / Java 17 / MariaDB / mock-auth** to
**Spring Boot 4.0 / Java 25 LTS / local H2 / real DB-backed login + PDF generation**.

The codebase has **almost no automated safety net** (one trivial unit test) and several
**hard blockers** for Spring Boot 4 (dead `spring-cloud-sleuth-otel`, removed `@MockBean`).
So the strategy is **stabilize first, jump last**:

1. Build a real test net on the *current* stack and get it green.
2. Make the small, reversible changes (DB → H2, real auth, PDF) on **Boot 3.4**.
3. Do the risky **Java 25 + Boot 4** platform jump **last**, against a codebase that is
   already simple and well-tested.

Every phase ends with a **verification gate** (build + tests must be green) and a commit,
so any phase can be reverted in isolation.

---

## 2. Current State (verified, not assumed)

| Area | Reality today | Implication |
|---|---|---|
| Framework | Spring Boot **3.4.13**, Java **17** | 2 major moving parts to change |
| DB | **MariaDB** — `flyway-mysql`, `MariaDBDialect`, V1 migration uses backticks + `engine=InnoDB` + `auto_increment`; `application.properties` full of MySQL-only Hikari tuning | DB swap touches migration, dialect, and ~40 lines of properties |
| Auth | **Fake mock** — `AuthServiceImpl` hardcodes `Admin/Admin/admin` in an in-memory `ConcurrentLinkedQueue`. No Spring Security, no users table. `logout()` has a bug (removes queue head, not the matching user) | "Users in DB" is a **new feature**, not a migration |
| Caching | Redis/Redisson, **disabled by default** (`caching.enabled=false`) | Safe to drop for a local demo |
| Observability | `spring-cloud-sleuth-otel` 1.1.4 + OTel exporter + Loki + Prometheus | **Sleuth is discontinued** — incompatible with Boot 4 / Spring Framework 7. Hard blocker. |
| AI | OpenAI Assistants v1 (`gpt-3.5-turbo`) in `ai/` package | Legacy/dead API; out of scope but flag as cleanup |
| PDF | **None** | New feature |
| Tests | **1** unit test (`PersonControllerTest`, asserts a 400). 3 integration tests exist but need **Docker + Testcontainers MariaDB** → don't run in plain `mvn test` | No safety net for refactoring |
| Build tooling | Lombok 1.18.42, Hibernate 6.6.39, Checkstyle 10.16, JaCoCo 0.8.12 | All need version checks for Java 25 / Boot 4 |

### Spring Boot 4 hard blockers found in code
- `@MockBean` in `PersonControllerTest` → **removed** in Boot 4 (use `@MockitoBean`).
- `spring-cloud-sleuth-otel` → not portable to Spring Framework 7.
- `application.properties`: `hibernate.current_session_context_class=...hibernate5.SpringSessionContext` (a Hibernate-5-era setting), `management.metrics.export.prometheus.enabled` (renamed in newer Boot), `spring.sleuth.*` (Sleuth-only).

---

## 3. Critique of the Proposed Stack

Decisions confirmed with you: **H2 file-mode**, **Spring Security + session login**, **OpenPDF**, **stabilize-first sequencing**.

| Proposed | Verdict | Notes / Risk |
|---|---|---|
| **Spring Boot 4.x** | OK, but newest-major risk | Boot **4.0.0 GA was 2025-11-20** (Spring Framework 7). Baseline Java 17, tested through Java 25. Ecosystem is young — Redisson, Loki appender, springdoc, and some plugins may lag. We mitigate by **removing** the fragile deps (Sleuth, Redisson) *before* the jump. **Decided: pin the latest 4.0.x patch — we do not need the latest minor (4.1+).** Stay on the stable 4.0 line. |
| **Java 25 LTS** | Good choice | **GA 2025-09-16, LTS.** Fully supported by Boot 4. Watch: Lombok (1.18.42 is borderline — bump to latest), `hibernate-jpamodelgen`, Checkstyle, JaCoCo, and any bytecode-manipulating plugin must support class file 69. **Recommendation: step through Java 21 first** (build + test on 21) to isolate JDK issues from framework issues. |
| **Local DB → H2 file-mode** | Good for the stated goal | Best Flyway/Hibernate support of the embedded options, persists across restarts, and — big win — lets the **integration tests run without Docker**. Cost: rewrite V1 migration to portable SQL, change dialect to `H2Dialect`, strip MySQL-only properties. Keep `flyway-core`; drop `flyway-mysql`. ⚠️ H2 is a *dev* DB — do not present it as production-grade. |
| **Simple login, users in DB** | Sound; decided | Spring Security + JPA `UserDetailsService` + BCrypt + Flyway `users` table. **Decided: seed one bootstrap user `admin` / `admin` via a Flyway migration (BCrypt hash of `admin`, documented).** **Auth moves to Keycloak later (not now)** — keep this layer dead-simple and swappable (session login now → OAuth2 resource server later). No premature OAuth2 abstraction. |
| **Flyway** | Correct, already present | Keep it. Just retarget to H2 and make migrations portable. Add migrations for `users` and any PDF-related tables. |
| **PDF → OpenPDF** | Good fit | Pure-Java, no native deps, LGPL+MPL, simple programmatic API, fine on Java 25 / Boot 4. If documents become layout-heavy later, revisit `openhtmltopdf + Thymeleaf`. |

**Over-engineering watch (ISTJ risk):** the current app carries a full observability + Redis + OpenAI stack for what is now a *local, fast-iteration demo*. The plan **deletes** that weight rather than migrating it. Don't re-add Jaeger/Loki/Grafana/Redis "to be safe" — that is exactly the perfectionism-before-testing trap.

**Decided dispositions (no longer open):**
- **Redis/Redisson + OpenAI (`ai/`): remove** (Phase 1).
- **Sleuth/Redisson: remove** (Phase 1).
- **Observability: keep only Actuator + Micrometer now.** OpenTelemetry + Prometheus are **needed later** and must be re-added on **Boot-4-stable versions** — in a dedicated phase **after** the Boot 4 jump (see Phase 8), not migrated through it.
- **Docker: not now.** H2 keeps the test suite and run loop Docker-free. Docker returns later (alongside Keycloak / real deploy).
- **Frontend impact:** session-cookie auth needs `withCredentials` + CORS/session config (minimal but real UI change in Phase 3).

---

## 4. Strategy & Reasoning

**Principle: change one variable at a time, behind a green test gate.**

**Fast iteration is paramount (explicit constraint).** Every phase must keep the local inner loop quick. **Flag anything that slows local development** — Docker requirements, slow tests, heavyweight infra. Concrete consequences baked into this plan: DB is embedded H2 (no Docker), the IT suite is freed from Testcontainers in Phase 2, and we do **not** carry Redis/Sleuth/OpenAI or full observability through the upgrade.

**Build gate stays strict (decided).** Checkstyle remains `failOnViolation=true` and integration tests + JaCoCo run on `verify`. With so few tests, the gate is our main safety net during a risky platform change — speed comes from removing infra (Docker/Redis), not from loosening the gate.

Why *stabilize first, jump last* (your choice — here's the justification):
- Refactoring without tests is how you "break something without noticing." Tests come first, full stop.
- The DB swap to H2 **removes the Docker dependency from the test suite**, which makes every later phase faster and safer to verify.
- Removing Sleuth/Redisson **before** Boot 4 shrinks the migration surface — fewer incompatible libraries to fight while also fighting the framework.
- The Java 25 + Boot 4 jump is the highest-risk step; doing it **last**, against a small + tested codebase, means a failure there is easy to localize (it's the framework, not your new auth code).

Why **not** platform-first: you'd be migrating the framework on top of MariaDB/Docker, dead Sleuth, and mock auth, with one trivial test — maximum surface, minimum safety. Rejected.

---

## 5. Phased Plan

Each phase: **make change → run gate → commit**. Do not start phase N+1 until phase N's gate is green. Gates use the project's own verification commands (see `CLAUDE.md` → "Verifying Code Changes").

### Phase 0 — Baseline & test net (no behavior change)
**Goal:** prove the current app works and lock its behavior before touching anything.

- [ ] Confirm a clean baseline: `cd api && ./mvnw clean test` is green on Boot 3.4 / Java 17.
- [ ] Add **characterization (golden-master) unit tests** for current behavior:
  - `AuthService`: login success (`Admin/Admin`), login failure (bad creds → `UnauthorizedException`), `currentUser` before/after login, logout. *Document the logout bug in the test as known behavior — do not fix it yet.*
  - `PersonServiceImpl`: persist / update / deleteById against a mocked repository.
  - `PersonController` / `PersonApiService`: validation + happy-path with `@WebMvcTest` (+ `@MockBean` for now; this becomes `@MockitoBean` in Phase 6).
  - `PersonApiRepositoryImpl` filter/sort logic if it contains query building.
- [ ] Make the integration tests runnable on demand and **flag** that they need Docker today (they'll be freed in Phase 2).
- [ ] Record baseline coverage (`./mvnw jacoco:report`) as the floor we don't drop below.

**Gate:** `./mvnw clean test` green; new tests meaningfully cover auth + person CRUD.
**Reasoning:** This is the safety net everything else leans on. The golden-master pins *current* behavior so later refactors are provably behavior-preserving.

---

### Phase 1 — Decommission dead weight (still Boot 3.4 / Java 17)
**Goal:** delete what we won't carry forward, shrinking the Boot 4 surface early.

- [ ] Remove `spring-cloud-sleuth-otel` + `spring-cloud-starter-sleuth` + `spring-cloud-dependencies` import. Replace tracing usage in `TracingConfiguration`/`PersonServiceImpl` with **Micrometer Tracing** (or strip tracing to plain logging for the local demo).
- [ ] **Remove Redis/Redisson** (`spring-data-redis`, `redisson`, `RedissonCacheConfig`, `@CacheEvict` usages — swap to a simple in-memory `ConcurrentMapCacheManager` or drop caching entirely for the local demo).
- [ ] **Remove the `ai/` (OpenAI) package** and its config (`OpenAIRestTemplateConfig`, `openai.*` properties) — dead Assistants v1 surface.
- [ ] Keep **only Actuator + Micrometer** for observability. Remove OTel exporter + Loki appender for now (OTel + Prometheus return in Phase 8 on Boot-4-stable versions).
- [ ] Trim `application.properties`: drop `spring.sleuth.*`, Redis props, `openai.*`, Loki/OTel props, MySQL-only Hikari tuning that no longer applies.

**Gate:** `./mvnw clean test` green; app still boots (`./mvnw spring-boot:run`).
**Reasoning:** Every dependency removed here is one fewer Boot-4 incompatibility to debug in Phase 6.

---

### Phase 2 — Switch DB to local H2 file-mode (still Boot 3.4 / Java 17)
**Goal:** fast local iteration + Docker-free tests.

- [ ] Swap `mariadb-java-client` → `com.h2database:h2`. Drop `flyway-mysql` (keep `flyway-core`).
- [ ] Rewrite `V1__create_person_table.sql` to **portable SQL** (no backticks, no `engine=InnoDB`; `bigint generated by default as identity` or `auto_increment` in H2 MySQL-compat mode).
- [ ] `application.properties`: `spring.datasource.url=jdbc:h2:file:./data/techdemo;...`, `H2Dialect`, remove MySQL data-source-properties, drop the `hibernate5.SpringSessionContext` line, set `spring.flyway.validate-on-migrate=true` (we now control the SQL).
- [ ] Re-point integration tests off Testcontainers MariaDB onto H2 (`BaseIT`, `TestContainersConfig`, `FlywayMigrationIT`, `PersonRepositoryIT`). **This removes the Docker requirement.**
- [ ] Optionally enable the H2 console under a `dev` profile.

**Gate:** `./mvnw clean verify` green **without Docker**; manual smoke: app starts, Flyway applies V1, a person can be created/listed via Swagger UI.
**Reasoning:** Doing this on Boot 3.4 isolates DB-dialect problems from framework problems. Freeing the IT suite from Docker pays off in every later gate.

---

### Phase 3 — Real DB-backed login (still Boot 3.4 / Java 17)
**Goal:** replace the mock with Spring Security + JPA users + BCrypt.

- [ ] Add `spring-boot-starter-security`.
- [ ] Flyway `V2__create_users_table.sql` (id, username unique, password_hash, enabled, roles).
- [ ] `V3__seed_admin_user.sql` — seed one bootstrap user **`admin` / `admin`**, password stored as a **BCrypt hash** of `admin` (never plaintext, never logged). This is a deliberate local-demo credential; it goes away when Keycloak lands.
- [ ] `User` JPA entity (extends `IdentifiableEntity`), `UserRepository`, `JpaUserDetailsService`, `SecurityConfig` (form/basic login, BCrypt `PasswordEncoder`, session management, CSRF + CORS config matching the SPA).
- [ ] Rework `AuthService`/`AuthController` to delegate to Spring Security (keep the `/api/auth/login|logout|currentuser` contract and `UserResponseDTO` shape so the UI keeps working). **Fix the logout bug here** (now backed by real session invalidation).
- [ ] Update characterization tests → behavior tests for real auth. Add `@WithMockUser` slice tests.
- [ ] **Frontend:** ensure `axiosClient` sends credentials (`withCredentials: true`); verify protected routes + login flow against the new endpoints. Run `cd ui && npm run lint` and the Playwright app smoke.

**Gate:** backend `./mvnw clean verify` green; UI lint + `npm run smoke` green; manual login with the seeded admin works end-to-end.
**Reasoning:** Auth touches `ui/src/common/auth/*` and `axiosClient.ts` — the CLAUDE.md "escalate to full suite" list. Treat it as a big change regardless of line count.

---

### Phase 4 — PDF generation with OpenPDF (still Boot 3.4 / Java 17)
**Goal:** add the new capability on a stable platform.

- [ ] Add `com.github.librepdf:openpdf`.
- [ ] One vertical slice: e.g. `GET /api/persons/{id}/pdf` → `PdfService` renders a person detail PDF (`application/pdf`, `Content-Disposition`). Keep it small (KISS/YAGNI — one document type, no template engine yet).
- [ ] Unit test `PdfService` (non-empty stream, `%PDF` magic bytes, contains expected field values). Controller slice test for content-type + headers + auth.

**Gate:** `./mvnw clean verify` green; manual download of a valid PDF.
**Reasoning:** A pure-Java library with no framework coupling — lowest-risk new feature, safe to land before the big jump. It also gives Phase 6 one more behavior to regression-check.

---

### Phase 5 — Java 17 → 21 (intermediate JDK step)
**Goal:** flush out JDK-level issues before stacking the framework jump on top.

- [ ] `sdk use java 21.0.10-zulu`; set `<java.version>21</java.version>`.
- [ ] Bump Lombok to latest, `hibernate-jpamodelgen` to match Hibernate, Checkstyle/JaCoCo as needed for class file 65.
- [ ] Full suite.

**Gate:** `./mvnw clean verify` green on Java 21; UI smoke green.
**Reasoning:** If something breaks here, it's the JDK/toolchain — not Spring. Isolation makes diagnosis trivial. (Skip-merge possible: if 21 is clean you can fold 21→25 into Phase 6, but the explicit stop is cheap insurance.)

---

### Phase 6 — Spring Boot 3.4 → 4.0 **and** Java 21 → 25 (the big jump)
**Goal:** land the target platform against a small, well-tested codebase.

- [ ] Bump parent to `spring-boot-starter-parent` **4.0.x (latest patch)**; `<java.version>25</java.version>`; `sdk use java 25...`.
- [ ] Apply known Boot 4 / Spring Framework 7 breaking changes:
  - `@MockBean` → `@MockitoBean` (already isolated to test code).
  - Renamed properties (e.g. Actuator/Prometheus metrics export keys); run with `spring-boot-properties-migrator` temporarily to surface them.
  - Any removed Spring MVC / `WebMvcConfigurer` APIs used in `TracingConfiguration`/`WebConfiguration`.
  - `org.springframework.lang.@Nullable` → JSpecify `@Nullable` (Spring 7 adopts JSpecify) in `IdentifiableEntity`.
  - Verify springdoc-openapi has a Boot-4-compatible release; bump it.
- [ ] Re-run the full gate **per sub-step** (do Boot bump and JDK bump as two commits if anything is shaky).
- [ ] Update `pom.xml` security pins (Tomcat/Netty/etc.) — many may be unnecessary once Boot 4 manages newer versions; remove redundant explicit pins.

**Gate:** `cd api && ./mvnw clean verify` green; `cd ui && npm run lint && npm run smoke && npm run smoke:storybook` green; Trivy scan (`docs/trivy-report.*`) reviewed — CRITICAL/HIGH fixed or flagged; manual end-to-end: login → CRUD → PDF.
**Reasoning:** Last and riskiest. Because Phases 1–5 already removed Sleuth/Redis/MariaDB and built a test net, a failure here is almost certainly framework-localized and bisectable.

---

### Phase 7 — Consolidation & docs
- [ ] Update `CLAUDE.md`, `README.md`, `run_application.sh`, `docker-compose.yml`, `doctor` to the new reality (no MariaDB/Redis/Sleuth; H2; Java 25; Boot 4).
- [ ] Re-run quality tooling (Sonar, Trivy, JaCoCo) and refresh `docs/`.
- [ ] Final coverage check vs. the Phase 0 floor.

---

### Phase 8 — Re-introduce OpenTelemetry + Prometheus (post-jump, optional/when needed)
**Goal:** restore the observability we stripped, on **Boot-4-stable** versions, without having fought it during the migration.

- [ ] Add **Micrometer Tracing + OTel bridge** (`micrometer-tracing-bridge-otel`) + `opentelemetry-exporter-otlp` — versions managed by the Boot 4 BOM (do **not** pin loose versions; verify they resolve from the Boot 4 dependency management).
- [ ] Re-add `micrometer-registry-prometheus` (already present) and confirm the Actuator `prometheus` endpoint.
- [ ] Re-introduce log shipping (Loki appender) only if needed, on a Boot-4-compatible release.
- [ ] This is **deferred until actually needed** (real deployment / when Docker + Keycloak come). Local fast-iteration runs fine on Actuator + Micrometer alone.

**Gate:** `./mvnw clean verify` green; `/actuator/prometheus` serves metrics; traces export when an OTLP collector is present.
**Reasoning:** You want OTel + Prometheus and you want them **stable**. The only way to guarantee stable versions is to add them *after* we're on the target platform, letting the Boot 4 BOM pick compatible versions — instead of dragging Boot-2/3-era tracing libs through the jump.

---

### Phase 9 — PostgreSQL for production (FUTURE / deferred — not now)
**Goal:** when we go to production, add PostgreSQL alongside (or replacing) local H2. **We are NOT doing this now** — H2 stays for fast local iteration. This phase exists only to record the target and to keep the earlier phases from painting us into a corner.

**Are there blockers to adding it later? No hard blockers — *if* we keep a few things portable now.** The H2→Postgres switch is low-risk provided Phases 2–4 follow these rules (cheap to honour now, expensive to retrofit):

- [ ] **Portable migration SQL only.** Plain ANSI DDL — no MySQL/H2-isms. Use `bigint generated by default as identity` (works in both H2 and Postgres), plain `varchar`, `timestamp`, `boolean`, `numeric`. No backticks, no `engine=InnoDB`, no H2-only functions/types. (This is already required by Phase 2.)
- [ ] **Datasource + Hibernate dialect are profile-driven, never hardcoded in shared config.** H2 settings live in a `local`/`dev` profile; a future `prod` profile supplies the Postgres URL/driver/dialect. Don't bake `H2Dialect` into the base `application.properties`.
- [ ] **Watch identifier casing.** The current `hibernate.globally_quoted_identifiers=true` + uppercase column names means Postgres will create case-sensitive quoted columns (`"FIRST_NAME"`). That's fine if consistent, but it's the one classic gotcha — decide a single casing convention in Phase 2 and keep migrations and entities aligned to it.
- [ ] **Stick to standard JPA / data types.** No vendor-specific column types (avoid raw JSON/array types unless wrapped portably). `GenerationType.IDENTITY` is fine in both (note: it disables Hibernate JDBC insert batching — acceptable).
- [ ] **Keep `flyway-core` (DB-agnostic).** Add `org.postgresql:postgresql` + (optionally) `flyway-database-postgresql` only in this phase. If a future migration ever needs DB-specific DDL, use Flyway vendor-specific locations (`db/migration/{vendor}`) rather than branching SQL.

**When this phase actually runs (later):** add the Postgres driver + prod profile, point ITs at **Testcontainers PostgreSQL** (Docker returns here, alongside Keycloak), run the full migration set against a clean Postgres instance, and verify the schema + a CRUD round-trip.

**Decision: deferred. Proceed with the plan as-is (H2 local).** No work in this phase now — we only commit to *not* introducing H2-specific SQL or hardcoded dialect that would block it.

---

## 6. Verification Gate (used at every phase)

| Layer | Command | Pass criterion |
|---|---|---|
| Backend style + unit | `cd api && ./mvnw clean test` | Checkstyle + unit tests green |
| Backend integration | `cd api && ./mvnw clean verify` | + integration tests (Docker-free from Phase 2) |
| Frontend gate | `cd ui && npm run lint` | tsc + ESLint clean |
| App smoke | `cd ui && npm run smoke` | no console/page errors |
| Storybook smoke | `cd ui && npm run smoke:storybook` | stories render clean |
| Security | `trivy fs . --scanners vuln` (repo root) | CRITICAL/HIGH fixed or flagged |

Rule: **fix errors, flag warnings.** Never edit a test to make a real failure pass.

---

## 7. Rollback

- One Git branch per phase (e.g. `upgrade/phase-2-h2`), squash-merged after its gate is green. A bad phase is reverted by dropping its branch — earlier phases are untouched.
- Tag the green baseline (`pre-upgrade-baseline`) at the end of Phase 0.
- H2 file DB lives under `./data/` (gitignored); delete it to reset state during iteration.

---

## 8. Decisions (all resolved)

| # | Decision | Resolution |
|---|---|---|
| 1 | Bootstrap user | **`admin` / `admin`**, BCrypt-hashed, seeded via Flyway (Phase 3) |
| 2 | `ai/` OpenAI package | **Remove** (Phase 1) |
| 3 | Redis / Redisson | **Remove** (Phase 1) |
| 4 | Sleuth | **Remove** (Phase 1) |
| 5 | Observability now | **Actuator + Micrometer only**; OTel + Prometheus re-added **after** Boot 4 on stable versions (Phase 8) |
| 6 | Tracing logs in `PersonServiceImpl` | **Remove** for local demo (re-add with observability in Phase 8) |
| 7 | Auth → Keycloak | **Later, not now.** Dead-simple session login now; swap to OAuth2 resource server later. No premature abstraction. |
| 8 | Docker | **Not now.** H2 keeps everything Docker-free; Docker returns with Keycloak / deploy |
| 9 | Spring Boot version | **Latest 4.0.x patch** (stable line); not 4.1+ |
| 10 | Build gate strictness | **Keep strict** (Checkstyle fail-on-violation, ITs on verify) |
| 11 | Session vs token | **Session** (confirmed); UI owns CORS/credentials wiring (Phase 3) |
| 12 | PostgreSQL for production | **Deferred** to future Phase 9 — H2 local for now. No hard blockers *if* migrations stay portable and dialect/datasource are profile-driven (see Phase 9). Proceed as planned. |

---

## 9. Risks

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| Boot 4 ecosystem immaturity (springdoc, etc.) | Medium | Medium | Removed fragile deps early; pin latest 4.0.x; `properties-migrator` |
| H2 SQL dialect drift from MariaDB | Medium | Low | Rewrite migration portable; `validate-on-migrate=true`; IT covers it |
| Auth change breaks the SPA login flow | Medium | High | Keep `/api/auth` contract; UI smoke + manual E2E in Phase 3 |
| Java 25 toolchain gaps (Lombok/plugins) | Low–Med | Medium | Intermediate Java 21 stop; bump toolchain versions |
| Scope creep / re-adding infra "to be safe" | Medium | Medium | Explicit YAGNI rule; infra returns only on real deploy need |

---

## 10. Dependabot follow-ups (deferred majors — opened 2026-06-08)

Batch reviewed 2026-06-08. Merged: jacoco `0.8.15` (patch), `@types/react-bootstrap` `1.1.0`;
removed orphaned direct deps `ora` + `wrap-ansi`. The two below are **held** — each needs
its own focused change, not a deps bump.

### 10.1 TypeScript 5.9 → 6.0 — ✅ DONE (PR #213, 2026-06-09)
Adopted as a full migration (not the `ignoreDeprecations` escape hatch). The real driver
was TS6's `strict: true` default (we only had `noImplicitAny`), which surfaced **54** errors
across 26 files — fixed all with no `any`/`@ts-ignore`/strict-downgrade.
- `tsconfig.json` modernized: `target es5 → es2018`, `moduleResolution node → "bundler"`,
  dropped `baseUrl` (`paths` now `./src/*`-relative; `tsconfig-paths-webpack-plugin` v4 and
  the main build resolve them fine). `strict: true` explicit.
- Ambient `*.scss`/`*.css` decls for TS6 `noUncheckedSideEffectImports`; typed `dotenv` shim
  (its package `exports` hide types under `bundler`).
- Storybook docgen `react-docgen-typescript → react-docgen` (the TS-compiler variant crashes
  on `baseUrl`-less `paths`).
- **ES2018 set as the browser baseline** (`package.json` `browserslist`: chrome ≥64 / edge ≥79 /
  firefox ≥58 / safari ≥12) so babel/autoprefixer align with the es2018 tsc target. Consistent
  with React 19 (no legacy-browser support).
- Verified: `tsc` 0 errors, eslint, webpack build, app + storybook smoke all green.
- Not run: full Playwright E2E (needs backend `:8082`); auth edits are type-level/behavior-preserving.

### 10.2 Remove `@types/react-bootstrap` entirely (follow-up to PR #207)
Bumped to `1.1.0`, which npm flags as a **stub**: *"react-bootstrap provides its own type
definitions, so you do not need this installed."* We use react-bootstrap v2 (ships its own
types), so this `@types` package is vestigial. Next step: delete the `@types/react-bootstrap`
dependency, `npm install --legacy-peer-deps`, and confirm `npm run lint` (tsc) still passes
on the 12 `from 'react-bootstrap'` import sites.

### 10.3 react-i18next 15 → 17 + i18next 24 → 26 — ✅ DONE (replaces PR #209, 2026-06-09)
Done as one coupled major upgrade now that TS6 is in (react-i18next 17 peers `typescript ^5||^6`):
`i18next ^24.2.2 → ^26.3.1` + `react-i18next ^15.4.1 → ^17.0.8`. `storybook-react-i18next@10.1.2`
already allows i18next ^26 + react-i18next ^17 — no addon bump needed. Verified: lint (TS6
type-check against the new i18n types), webpack build, app smoke, storybook smoke (Form story
exercises the addon) all green. PR #209 closed as superseded.
