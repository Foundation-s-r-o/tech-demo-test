# Tasks

Implement in this order. Keep each item focused on the goal and desired outcome;
do not preserve historical notes here. Git is the history.

New tasks are derived by applying the discovery rules at the bottom of this file
to `docs/ARCHITECTURE.md`, the ADRs, and `SECURITY.md`.

1. Run the Playwright harness in CI.
   - Goal: make the E2E suite (auth, authz, GDPR, audit chain, a11y, OpenAPI drift) a push-time
     gate instead of a laptop-only check.
   - Clarification: today CircleCI gates only `mvnw verify` + UI lint/build; every Playwright
     spec runs only via local `doctor`. This is the largest gap between "gate is the safety net"
     and what the gate actually covers.
   - How: add a CircleCI job (Node 22 image + `npx playwright install --with-deps chromium`)
     that runs smoke + the full E2E suite; the suite already self-starts both servers on in-mem
     H2, so no Docker is needed. Store the HTML report + screenshots as build artifacts.

2. Add login throttling.
   - Goal: protect login against account and source-address brute-force attempts before the template is exposed beyond local/demo use.
   - Clarification: this is a `SECURITY.md` known residual gap. Keep the control compatible with the current session-login model and document only the intended behavior.
   - How: don't hand-roll counters. Listen for Spring Security's `AuthenticationFailureBadCredentialsEvent`/`AuthenticationSuccessEvent` and gate an `AuthenticationProvider`/`UserDetailsService` on a per-username+IP failure count backed by a Caffeine cache (swap to Redis only when clustered) with exponential backoff. Add Bucket4j (token-bucket) as a filter ahead of the auth entry point for per-source-address limiting, and record lockouts to `AUDIT.SECURITY_EVENT` via the existing `SecurityEventListener`.

3. Verify logging implementation.
   - Goal: understand how application logging works today and whether it matches the template goal.
   - Clarification: check backend logging configuration, security/audit logging, frontend console handling, and test expectations. Document only the intended behavior and required controls.
   - How: backend is SLF4J over Spring Boot's Logback (`logback-spring.xml`); set per-profile levels via `logging.level.*` and enable Boot 4 structured JSON logs for `prod` (`logging.structured.format.console=ecs`) with trace/span IDs via Micrometer MDC. Keep auth/audit events on `SecurityEventListener` -> `AUDIT.SECURITY_EVENT` (no passwords/PII). Frontend: strip `console.*` from prod builds (Terser `drop_console`) and enforce the ESLint `no-console` rule behind a small logger wrapper. Deliverable is a short "intended logging + never-log" note.

4. Verify observability endpoints in the test harness.
   - Goal: make the test/doctor output explicitly name the observability endpoints it verifies.
   - Clarification: this is a `SECURITY.md` known residual gap. Current supported surface should be Actuator health plus any authenticated operational endpoint that is intentionally exposed. Keep full OTel/Prometheus/Grafana/Loki out unless reintroduced as supported runtime.
   - How: source of truth is Spring Boot Actuator's `management.endpoints.web.exposure.include`. Add a test (Playwright `test_observability.spec.ts` or a MockMvc slice) asserting `GET /actuator/health` is public/UP and `/actuator/**` returns 401 anonymous / 200 for SUPERVISOR, and make `doctor` echo the endpoint list it verifies.

5. Reset the database/Flyway baseline.
   - Goal: make the local database easy to wipe and recreate from a clean migration baseline.
   - Clarification: this is a high-blast-radius change. Do it in its own batch, preserve required seed/demo data behavior, and verify a clean database can migrate from `V1__*`.
   - How: squash V1-V8 into a single `V1__baseline.sql` (H2 is disposable/gitignored, so no `flyway:baseline` on live data is needed); move demo seeding out of migrations into the profile-gated `LocalAdminInitializer` so the V3-seed/V4-remove and V1-create/V8-drop churn disappears. Keep ANSI-portable SQL and verify a fresh DB migrates end-to-end via `./mvnw verify`.

6. Add a PostgreSQL production profile.
   - Goal: provide a real production database path while preserving the fast embedded-H2 local loop.
   - Clarification: source comments currently mark this as deferred. This remains production-hardening work until the repo has a supported deployment target.
   - How: keep datasource + Hibernate dialect profile-driven (never hardcode a dialect). Add `application-prod.properties` with a PostgreSQL `DataSource` (HikariCP is the Boot default pool) and let Hibernate auto-detect the dialect. Keep Flyway migrations ANSI-portable and reintroduce the PG-only controls (RLS, triggers, GiST exclusion) via the `-- PG-delta:` markers. Test with a Testcontainers PostgreSQL profile in a dedicated `verify` path so the default local loop stays Docker-free.

7. Add MFA or stronger identity.
   - Goal: replace the demo-grade DB-backed login posture with production-grade account protection before real users rely on the service.
   - Clarification: source comments currently mark stronger identity as deferred. Keep feature code independent of a specific identity provider until the target is chosen.
   - How: use Spring Security 7's native MFA (`@EnableMultiFactorAuthentication`) — one-time tokens, WebAuthn/passkeys, and TOTP modelled as progressive authority granting; no external IdP required. Keep an OIDC boundary open (`spring-boot-starter-oauth2-client`) so Keycloak/OIDC can be added later for SSO/federation without touching feature code. Store TOTP secrets / WebAuthn credentials via the deployment secret manager, never in the repo.

8. Make the prod profile fail fast on incomplete configuration.
   - Goal: a `prod`-profile boot with missing/invalid required config must fail at startup
     with a clear message, never limp along with defaults.
   - Clarification: crypto keys already fail fast; extend the same discipline to all
     security-relevant config (allowed origins, session policy, datasource) and document the
     complete required-env-var list in one place.
   - How: consolidate config into validated `@ConfigurationProperties` (JSR-303 +
     `@Validated`), add a boot test asserting `prod` fails without the required vars, and a
     generated/maintained "required environment" table in SECURITY.md.

9. Expose and verify readiness/liveness probes plus graceful shutdown.
   - Goal: give the app real operational lifecycle behavior (K8s-style probes, connection
     draining) ahead of any deployment target.
   - Clarification: extends task 4 — fold into it if done together.
   - How: `management.endpoint.health.probes.enabled=true`, `server.shutdown=graceful` with
     a timeout, and assert `/actuator/health/liveness|readiness` in the observability test.

10. Resolve the persisted-state parsing TODO.
    - Goal: make invalid `localStorage` values in `ui/src/common/persistService.ts` behave predictably.
    - Clarification: decide the intended user-visible and developer-visible outcome for malformed persisted JSON; avoid silent ambiguity.
    - How: wrap the `JSON.parse` in `persistService.ts` in try/catch; on a malformed value, drop the key (`localStorage.removeItem`) and fall back to the default rather than throwing. Validate the parsed shape with the existing Yup schema before use, and surface one diagnostic via the frontend logger wrapper.

11. Resolve the date-format configuration TODO.
    - Goal: make `ui/src/components/form/DatePicker.tsx` use the intended date-format policy.
    - Clarification: either keep the Slovak template default as an explicit product decision or connect it to supported application configuration.
    - How: decide the policy — keep Slovak `dd.MM.yyyy` as an explicit exported default constant, or source it from app config/i18n. Route `DatePicker.tsx` and every caller through one shared formatter helper built on the date library the UI already bundles, so there is a single format source of truth.

12. Codify the architecture rules as ArchUnit tests.
    - Goal: turn `docs/ARCHITECTURE.md` claims (feature packages over shared infrastructure,
      controller -> API service -> domain, no cross-feature imports, constructor injection,
      no vendor dialect/SQL imports) into executable tests that fail `verify`.
    - Clarification: these rules are currently enforced only by review; ArchUnit makes the
      documentation self-verifying and catches drift from generated/refactored code.
    - How: one `ArchitectureTest` class with `com.tngtech.archunit` rules per documented
      constraint; run in the normal unit-test phase (no Docker, milliseconds).

13. Add invariant tests for app-layer-enforced database rules.
    - Goal: prove the rules that PostgreSQL would enforce natively (bitemporal non-overlap,
      append-only vital records, audit hash-chain continuity, no-delete) hold under adversarial
      inputs — the application layer is currently their only enforcement point.
    - Clarification: `BitemporalGuardTest`/`AuditServiceTest` cover happy paths; the PG-delta
      controls have no DB backstop on H2, so property-style tests are the real safety net.
    - How: jqwik property tests for BitemporalGuard interval logic and hash-chain
      verification under random interleavings; concurrency test for double-registration.
      Optionally run PIT mutation testing once, advisory-only, on these packages to check the
      tests actually kill faults — do not wire PIT into the gate.

14. Add a frontend unit-test layer for pure logic.
    - Goal: catch logic regressions (persist-state parsing, date formatting, Yup schemas,
      axios interceptors) in milliseconds instead of only via the slow E2E suite.
    - Clarification: the UI currently has zero unit tests — every regression must survive to a
      Playwright run to be caught. Keep scope to pure logic; component rendering stays with
      Storybook + smoke.
    - How: Vitest + Testing Library, `npm test` wired into `doctor` and CI beside lint. Start
      with the code touched by tasks 10 and 11 so those fixes land with tests.

15. Gate the declared contract against the implementation.
    - Goal: stop `db/openapi.yaml` (the declared contract source) from drifting silently from
      the running API.
    - Clarification: current checks cover live-spec vs committed `ui/api-docs.json` and
      spec vs generated client — but nothing compares `db/openapi.yaml` itself to what
      springdoc emits. The "source of truth" is currently unverified.
    - How: add an `oasdiff` (breaking-changes mode) step to `doctor` diffing `db/openapi.yaml`
      against the live `/v3/api-docs` during the E2E phase; report additive drift as advisory,
      breaking drift as failure. Accepted cuts from task 16 get an ignore list with reasons.

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

20. Support English and Slovak UI locales.
    - Goal: full EN/SK localization with translations that cannot silently drift apart.
    - Clarification: the date-format policy must come from the same locale layer — align with
      task 11 (single formatter source of truth) rather than a separate mechanism.
    - How: introduce react-i18next (or the existing environment-strings mechanism if one is
      ported) with one JSON resource per locale; add a key-parity check (script comparing EN/SK
      key sets, wired into lint/doctor) so a missing translation fails the gate; locale-aware
      date formatting flows through the task-11 helper; add a Playwright test toggling locale.

21. Write a lightweight threat model.
    - Goal: unblock the deferred TLS/packaging/secret-management items, which SECURITY.md and
      UPGRADE_PLAN.md explicitly gate on "after a threat model exists" — no task creates one.
    - Clarification: one short STRIDE-lite doc over the registry surface (actors, trust
      boundaries, top abuse cases), not an enterprise artifact.
    - How: `docs/THREAT_MODEL.md` with a data-flow diagram, per-boundary threats, and a
      mapping to existing controls / residual-gap tasks. ASCII Slovak per docs convention.

22. Schedule audit-chain self-verification.
    - Goal: tamper-evidence that is checked continuously, not only when someone calls the
      verification endpoint.
    - Clarification: low priority; reuses the existing chain-verification service.
    - How: a `@Scheduled` job running the existing verification, exposing a Micrometer
      gauge/counter (`audit.chain.valid`) and logging a security event on failure.

23. Add a docs link-and-reference check to doctor.
    - Goal: broken relative links and references to deleted files get caught mechanically —
      documentation cleanups keep removing link targets.
    - How: `lychee` (offline mode, internal links only) over `*.md` as an advisory doctor step.

24. Decide the fate of performancetest/TechDemo.jmx.
    - Goal: no undocumented, unwired assets in the repo.
    - How: either document how/when to run it (README or an ADR note) and verify it works
      against the current API, or delete it.

## Task discovery rules

Abstract rules for deriving new tasks from `docs/ARCHITECTURE.md`, the ADRs, and
`SECURITY.md`. Production readiness is usually not "replace X with a more robust
Y" — it is a capability that does not exist yet. A task exists wherever a rule
below finds no answer in the repo.

1. **Claim-enforcer rule.** Every normative claim in architecture docs, ADRs, or
   SECURITY.md ("X is enforced", "never Y", "Z is the only path") must have a
   test or gate that fails when the claim breaks. Unenforced claim -> task.
2. **Deferral-trigger rule.** Every deferred item (`TODO:`, `-- PG-delta:`,
   accepted cut, "future hardening") must name the condition that activates it
   and appear in TASKS.md or `docs/UPGRADE_PLAN.md`. Orphan deferral -> task.
3. **ADR-assumption rule.** Each ADR was decided under constraints (stack
   version, no deployment target, demo scope). When a constraint changes,
   re-validate the decision and its consequences. Stale assumption -> task.
4. **Surface-coverage rule.** Every runtime surface — endpoint, Spring profile,
   scheduled job, error path, script — must be exercised by the harness at least
   once. A profile no test ever boots is an untested surface. Uncovered -> task.
5. **Lifecycle rule.** Every stateful asset (data category, crypto key, session,
   account, audit log, schema) needs a defined full lifecycle: created -> used ->
   rotated/archived -> purged/restored. Missing stage -> task.
6. **Failure-mode rule.** For every dependency (database, disk, clock, session
   store, downstream consumer), the intended behavior when it is unavailable,
   slow, or corrupt must be defined and ideally tested. Undefined -> task.
7. **Human-procedure rule.** Every action a human must perform in production
   (provision a user, rotate a key, restore a backup, handle an incident, meet a
   DSR deadline) needs a written, rehearsable procedure. Missing runbook -> task.
8. **Automation rule.** Every gate that relies on someone remembering to run it
   must also run automatically (CI or schedule). Memory-dependent gate -> task.
9. **Duality rule.** Every control implies a dual: write-audit <-> read-audit,
   backup <-> restore, deploy <-> rollback, provision <-> deprovision,
   lock <-> unlock. Implemented control with a missing dual -> task.
10. **Legal-obligation rule.** For a public-sector registry, every applicable
    obligation (GDPR articles, accessibility act, NIS2, statistical duties) maps
    to an implemented control, a task, or an explicit accepted cut with a
    reason. Unmapped obligation -> task.
