---
status: accepted
date: 2026-07-06
deciders: Foundation engineering + CTO
---
# Layered test harness with Playwright E2E and `./doctor` as the single entry point

## Context and Problem Statement

The gate ([ADR-0008](0008-strict-quality-security-gate.md)) defines *which*
controls block a change, but the test surface itself was thin and fragmented:
the E2E suite covered only authentication, and no single command executed the
whole harness (build, lint, unit/integration tests, browser tests, security
scans, static analysis). How should the harness be layered, and how is it run
repeatably?

## Decision Drivers

* Fast local inner loop — no Docker, no heavyweight infra ([ADR-0002](0002-minimal-docker-free-stack.md)).
* With few tests, each layer must earn its place; avoid over-engineering.
* One command should run everything, so "the harness passed" is unambiguous.
* Browser evidence (screenshots, traces, console logs) captured automatically.

## Considered Options

* **Layered pyramid + `./doctor` orchestrator** (unit → integration → smoke →
  E2E → scans → static analysis in one script).
* Separate documented commands per layer, no orchestrator.
* CI-only harness (rely on CircleCI/GitHub scanners, nothing locally).

## Decision Outcome

Chosen option: **layered pyramid + `./doctor` orchestrator**.

| Layer | Tooling | Scope |
| ----- | ------- | ----- |
| Backend unit | JUnit 5 via Surefire (`*Test`) | services, controllers, PDF |
| Backend integration | Failsafe (`*IT`), full Spring context on **in-memory H2**, profile `it` | auth flow, persons CRUD + authz matrix, repository, Flyway |
| App smoke | Playwright, backend-free, dev server on `:8090` | login shell renders, zero console errors |
| Storybook smoke | Playwright, Storybook on `:6007` | shared components render clean |
| Full E2E | Playwright `test_*.spec.ts`; auto-starts API `:8082` (profiles `local,e2e`, in-mem DB) + UI `:8080` | login, failed login, logout, main page, persons list/create/detail, client-side validation, birth-record office scoping, GDPR DSR, audit chain verification, citizen `/me`, agency authorization checks, accessibility (axe-core, serious/critical gate), live-spec contract check |
| Coverage gate | `jacoco:check` on `verify`, merged unit+IT data | line coverage ≥ `jacoco.line-coverage.minimum` (`api/pom.xml`; ratchet toward 0.80) |
| Contract drift | E2E live-spec test + `./doctor` client regeneration diff | backend ↔ `ui/api-docs.json` ↔ generated TS client |
| Security scans | `scripts/security_scan.sh` (Semgrep, Trivy, npm audit) | repo-wide |
| Static analysis | SonarQube (skipped with a warning when token/host unavailable) | both modules |

`./doctor` runs all layers in that order and fails fast. E2E specifics:

* Tests are **self-contained** — each creates its own data through the UI as the
  `local`-profile admin; the `e2e` profile uses a fresh in-memory H2, so no seed
  fixtures or cleanup steps exist.
* `webServer` manages both app servers (`reuseExistingServer: false`);
  `./doctor` pre-checks that `:8080`/`:8082` are free.
* `screenshot: 'on'` in all Playwright configs — every test, passed or failed,
  leaves a screenshot in `test-results/` and the HTML report (gitignored).
* Stable `id` selectors (`#login_*`, `#PersonCreateForm_*`, `#error_dialog_close`)
  instead of text/CSS-utility locators.

Beyond the basics the harness also gates JaCoCo minimum coverage (merged
unit+IT), authz negatives via the seeded `user/user` `ROLE_USER` account,
accessibility (`@axe-core/playwright`, serious/critical), and OpenAPI contract
drift (live spec vs `ui/api-docs.json` in E2E; spec vs generated client in
`./doctor` — springdoc pinned to `openapi_3_0` output, generator CLI to 6.0.1).
Rejected as over-engineering for a template: mutation testing, performance
budgets, visual-regression snapshots.

### Consequences

* Good — one command (`./doctor`) proves the whole harness; CI (CircleCI) and the
  GitHub scanners cover the push path independently.
* Good — E2E now covers several primary user journeys, not just auth.
* Bad — `./doctor` is slow (full E2E starts both servers); the fast inner loop
  remains the per-layer commands in CLAUDE.md's "Verifying Code Changes".

## Validation

`./doctor` exits 0 with all layers green; the Playwright HTML reports and
`docs/smoke-*.log` console captures are produced per run. Executable definitions
live in `doctor`, `ui/playwright*.config.ts`, `ui/tests/`, and `api/pom.xml`
(Surefire/Failsafe/JaCoCo) — per source-gate precedence
([ADR-0008](0008-strict-quality-security-gate.md)), those files are the authority
over this prose.
