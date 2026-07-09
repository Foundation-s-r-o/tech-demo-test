# Tasks

Implement in this order. Keep each item focused on the goal and desired outcome;
do not preserve historical notes here. Git is the history.

1. [done] Verify and clean `docs/UPGRADE_PLAN.md`.
   - Goal: make the document reflect only current state and remaining deferred work.
   - Clarification: phases 0-7 appeared complete and were replaced with a current baseline plus deferred work. Keep genuinely deferred work clearly labelled as deferred, not unfinished.

2. [done] Review `docs/QUALITY_REPORT.md`.
   - Goal: decide whether it is still useful as an active report.
   - Clarification: removed because the content duplicated `doctor`, compact Sonar JSON, `SECURITY.md`, and current ADRs.

3. [done] Review `SECURITY.md` open and unfinished issues.
   - Goal: produce a concrete implementation plan for remaining real security gaps.
   - Clarification: current template gaps are now separated from future production-only hardening. Explicitly deferred production items are not immediate work unless the repo goal changes.

4. Add login throttling.
   - Goal: protect login against account and source-address brute-force attempts before the template is exposed beyond local/demo use.
   - Clarification: this is a `SECURITY.md` known residual gap. Keep the control compatible with the current session-login model and document only the intended behavior.
   - How: don't hand-roll counters. Listen for Spring Security's `AuthenticationFailureBadCredentialsEvent`/`AuthenticationSuccessEvent` and gate an `AuthenticationProvider`/`UserDetailsService` on a per-username+IP failure count backed by a Caffeine cache (swap to Redis only when clustered) with exponential backoff. Add Bucket4j (token-bucket) as a filter ahead of the auth entry point for per-source-address limiting, and record lockouts to `AUDIT.SECURITY_EVENT` via the existing `SecurityEventListener`.

5. [done] Review `docs/REACT19_UPGRADE_PLAN.md`.
   - Goal: decide whether it is still useful beside `docs/UPGRADE_PLAN.md`.
   - Clarification: removed because it was an executed React 19 migration plan from 2026-05-27. Current React/TypeScript baseline belongs in `README.md` and `docs/UPGRADE_PLAN.md`.

6. [done] Remove unused observability stack files.
   - Goal: keep only the supported observability surface in source.
   - Clarification: removed `otel-config.yml`, `grafana/`, `loki/`, and `prometheus/` because they were unsupported reference inputs. ADRs and architecture now state that full-stack runtime configs stay out until a deployment target owns them.

7. [done] Review `scripts/security_scan.sh`.
   - Goal: confirm whether this remains the right security scan entry point.
   - Clarification: kept it as the repo-local security scan entry point because `doctor`, `CLAUDE.md`, ADRs, and `SECURITY.md` already depend on it. README now points to it without duplicating scanner details.

8. [done] Rewrite `README.md`.
   - Goal: make it a short developer entry point.
   - Clarification: README now contains intro, quick start, daily verification commands, links to authoritative docs, and deployment status only.

9. Verify logging implementation.
   - Goal: understand how application logging works today and whether it matches the template goal.
   - Clarification: check backend logging configuration, security/audit logging, frontend console handling, and test expectations. Document only the intended behavior and required controls.
   - How: backend is SLF4J over Spring Boot's Logback (`logback-spring.xml`); set per-profile levels via `logging.level.*` and enable Boot 4 structured JSON logs for `prod` (`logging.structured.format.console=ecs`) with trace/span IDs via Micrometer MDC. Keep auth/audit events on `SecurityEventListener` -> `AUDIT.SECURITY_EVENT` (no passwords/PII). Frontend: strip `console.*` from prod builds (Terser `drop_console`) and enforce the ESLint `no-console` rule behind a small logger wrapper. Deliverable is a short "intended logging + never-log" note.

10. Verify observability endpoints in the test harness.
   - Goal: make the test/doctor output explicitly name the observability endpoints it verifies.
   - Clarification: this is a `SECURITY.md` known residual gap. Current supported surface should be Actuator health plus any authenticated operational endpoint that is intentionally exposed. Keep full OTel/Prometheus/Grafana/Loki out unless reintroduced as supported runtime.
   - How: source of truth is Spring Boot Actuator's `management.endpoints.web.exposure.include`. Add a test (Playwright `test_observability.spec.ts` or a MockMvc slice) asserting `GET /actuator/health` is public/UP and `/actuator/**` returns 401 anonymous / 200 for SUPERVISOR, and make `doctor` echo the endpoint list it verifies.

11. Reset the database/Flyway baseline.
   - Goal: make the local database easy to wipe and recreate from a clean migration baseline.
   - Clarification: this is a high-blast-radius change. Do it in its own batch, preserve required seed/demo data behavior, and verify a clean database can migrate from `V1__*`.
   - How: squash V1-V8 into a single `V1__baseline.sql` (H2 is disposable/gitignored, so no `flyway:baseline` on live data is needed); move demo seeding out of migrations into the profile-gated `LocalAdminInitializer` so the V3-seed/V4-remove and V1-create/V8-drop churn disappears. Keep ANSI-portable SQL and verify a fresh DB migrates end-to-end via `./mvnw verify`.

12. Add a PostgreSQL production profile.
   - Goal: provide a real production database path while preserving the fast embedded-H2 local loop.
   - Clarification: source comments currently mark this as deferred. This remains production-hardening work until the repo has a supported deployment target.
   - How: keep datasource + Hibernate dialect profile-driven (never hardcode a dialect). Add `application-prod.properties` with a PostgreSQL `DataSource` (HikariCP is the Boot default pool) and let Hibernate auto-detect the dialect. Keep Flyway migrations ANSI-portable and reintroduce the PG-only controls (RLS, triggers, GiST exclusion) via the `-- PG-delta:` markers. Test with a Testcontainers PostgreSQL profile in a dedicated `verify` path so the default local loop stays Docker-free.

13. Add MFA or stronger identity.
   - Goal: replace the demo-grade DB-backed login posture with production-grade account protection before real users rely on the service.
   - Clarification: source comments currently mark stronger identity as deferred. Keep feature code independent of a specific identity provider until the target is chosen.
   - How: use Spring Security 7's native MFA (`@EnableMultiFactorAuthentication`) — one-time tokens, WebAuthn/passkeys, and TOTP modelled as progressive authority granting; no external IdP required. Keep an OIDC boundary open (`spring-boot-starter-oauth2-client`) so Keycloak/OIDC can be added later for SSO/federation without touching feature code. Store TOTP secrets / WebAuthn credentials via the deployment secret manager, never in the repo.

14. Resolve the persisted-state parsing TODO.
   - Goal: make invalid `localStorage` values in `ui/src/common/persistService.ts` behave predictably.
   - Clarification: decide the intended user-visible and developer-visible outcome for malformed persisted JSON; avoid silent ambiguity.
   - How: wrap the `JSON.parse` in `persistService.ts` in try/catch; on a malformed value, drop the key (`localStorage.removeItem`) and fall back to the default rather than throwing. Validate the parsed shape with the existing Yup schema before use, and surface one diagnostic via the frontend logger wrapper.

15. Resolve the date-format configuration TODO.
   - Goal: make `ui/src/components/form/DatePicker.tsx` use the intended date-format policy.
   - Clarification: either keep the Slovak template default as an explicit product decision or connect it to supported application configuration.
   - How: decide the policy — keep Slovak `dd.MM.yyyy` as an explicit exported default constant, or source it from app config/i18n. Route `DatePicker.tsx` and every caller through one shared formatter helper built on the date library the UI already bundles, so there is a single format source of truth.

16. Review accepted OpenAPI cuts.
   - Goal: decide which `db/openapi.yaml` accepted cuts should remain intentional scope limits and which should become implemented API/UI behavior.
   - Clarification: cover dissolution amendments, DSR status governance and Art. 12 extension handling, audit-event paging, representative portal access, and codelist-backed office/municipality inputs.
   - How: triage each documented cut in `db/openapi.yaml` as "intentional scope limit" (leave, note it) or "to implement". For the latter, add the operation to the contract, regenerate the client (`npm run generate-openapi-services`), implement controller + UI, and cover with Playwright. Cross-check against items 17-18 so contract, client, and tests move together.

17. Ensure the UI uses the published API surface and tests cover those features.
    - Goal: close the gap between the published OpenAPI contract, generated UI client, implemented UI features, and Playwright coverage.
    - Clarification: the contract source in this repo is `db/openapi.yaml`; `ui/api-docs.json` and `ui/src/api/generated/` are generated/derived artifacts. Cover missing API-backed user workflows, not every low-level endpoint mechanically.
    - How: regenerate the typed client from the contract with `npm run generate-openapi-services` (openapi-generator, typescript-axios) and route all UI calls through it (no hand-rolled axios). Map each contract `x-required-roles` workflow to a Playwright spec; `test_openapi.spec.ts` already gates live-spec-vs-committed drift.

18. Ensure every role has at least two Playwright tests.
    - Goal: prove the six demo roles have meaningful positive and negative coverage.
    - Clarification: this is a `SECURITY.md` known residual gap. Current roles are `SUPERVISOR`, `CLERK`, `DPO_OFFICER`, `SECURITY_AUDITOR`, `AGENCY_READER`, and `CITIZEN`. Prefer tests that exercise real permissions and role-specific workflows over duplicate login checks.
    - How: use the seeded six accounts via the `login(page, creds)` helper and, per role, add one positive test (an allowed workflow succeeds) and one negative test (a forbidden route/endpoint returns 403 or is hidden in the UI). Extend `test_authz.spec.ts` rather than duplicating login-only checks.

19. Add a periodic API/UI coverage check.
    - Goal: prevent the UI, generated OpenAPI client, and Playwright coverage from drifting after the initial gap is closed.
    - Clarification: add this only after items 17 and 18 are complete. The periodic check should report what is missing; it should not force noisy, endpoint-by-endpoint coverage that adds no product value.
    - How: add a small Node script that parses `db/openapi.yaml` operations and diffs them against the generated client methods and the Playwright-covered operations, reporting gaps only (advisory, non-failing). Wire it into `doctor` beside the existing OpenAPI drift step. Do this after 17 and 18.


20. Keep 2 language versions for this app: English and SLovak, make sure dae formats are consistent, keep the language translations in sync.

