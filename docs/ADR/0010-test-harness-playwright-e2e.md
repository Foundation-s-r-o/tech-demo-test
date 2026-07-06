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
scans, static analysis). How should the test harness be layered, and how is it
executed repeatably?

## Decision Drivers

* Fast local inner loop — no Docker, no heavyweight infra ([ADR-0002](0002-minimal-docker-free-stack.md)).
* With few tests, each layer must earn its place; avoid over-engineering.
* One command should run everything, so "the harness passed" is unambiguous.
* Browser evidence (screenshots, traces, console logs) should be captured
  automatically for review.

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
| Full E2E | Playwright `test_*.spec.ts`; auto-starts API `:8082` (profiles `local,e2e`, in-mem DB) + UI `:8080` | login, failed login, logout, main page, persons list/create/edit, client-side validation |
| Security scans | `scripts/security_scan.sh` (Semgrep, Trivy, npm audit) | repo-wide |
| Static analysis | SonarQube (skipped with a warning when token/host unavailable) | both modules |

`./doctor` runs all layers in that order and fails fast. E2E specifics:

* Tests are **self-contained** — each creates its own data through the UI as
  the `local`-profile admin; the `e2e` profile uses a fresh in-memory H2, so no
  seed fixtures or cleanup steps exist.
* `webServer` manages both app servers (`reuseExistingServer: false`);
  `./doctor` pre-checks that `:8080`/`:8082` are free.
* `screenshot: 'on'` in all Playwright configs — every test, passed or failed,
  leaves a screenshot in `test-results/` and the HTML report (gitignored).
* Stable `id` selectors (`#login_*`, `#PersonEditForm_*`, `#error_dialog_close`)
  instead of text/CSS-utility locators.

Considered and deliberately **deferred** (recorded so they are conscious calls,
not omissions): JaCoCo minimum-coverage gate (tracked as an open gap in
[`SECURITY.md`](../../SECURITY.md)), authz-negative E2E with a second USER-role
test account, accessibility smoke (`@axe-core/playwright`), OpenAPI
client-drift check in CI. Rejected as over-engineering for a template:
mutation testing, performance budgets, visual-regression snapshots.

### Consequences

* Good — one command (`./doctor`) proves the whole harness; CI (CircleCI) and
  the GitHub scanners cover the push path independently.
* Good — E2E now covers the primary user journeys, not just auth.
* Bad — `./doctor` is slow (full E2E starts both servers); the fast inner loop
  remains the per-layer commands in CLAUDE.md's "Verifying Code Changes".
* Bad — no coverage minimum yet; the JaCoCo gate stays an open gap.

## Validation

`./doctor` exits 0 with all layers green; the Playwright HTML reports and
`docs/smoke-*.log` console captures are produced per run. Executable
definitions live in `doctor`, `ui/playwright*.config.ts`, `ui/tests/`, and
`api/pom.xml` (Surefire/Failsafe/JaCoCo) — per the source-gate precedence rule
([ADR-0008](0008-strict-quality-security-gate.md)), those files are the
authority over this prose.
