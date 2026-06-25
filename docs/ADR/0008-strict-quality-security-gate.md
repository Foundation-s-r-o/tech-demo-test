---
status: accepted
date: 2026-06-25
deciders: Foundation engineering + CTO
---
# Strict quality & security gate, with the running config as the authority

## Context and Problem Statement

With a deliberately small test surface, the automated gate is the main thing
keeping `main` releasable and modelling the bar downstream projects inherit. The
question is which controls are *blocking* versus *advisory*, and — because this
template is GitHub-hosted ([ADR-0001](0001-using-source-repo.md)) — which
GitHub-native toolchain implements them.

## Decision Drivers

* The gate is the safety net; it is never relaxed for speed (CLAUDE.md).
* Security is a continuous, automated activity, not a pre-release step.
* GitHub-native tooling, since the repo lives on GitHub.
* Keep local verification fast and proportionate to change size.

## Considered Options

* **GitHub-native gate**: CircleCI build + GitHub security scanners + local tools.
* **Internal Jenkins triplet** (SonarQube + Trivy + license) — for a project on
  an internal CI host.

## Decision Outcome

Chosen option: a **GitHub-native, layered gate**. Blocking vs advisory:

| Control | Where | Blocking? |
| ------- | ----- | --------- |
| CircleCI — `mvn verify` (BE) + npm lint/build (FE) | every push | **Yes** |
| Checkstyle (`failOnViolation=true`) | Maven `validate` | **Yes** |
| ESLint + `tsc --noEmit` | `npm run lint` | **Yes** (errors) |
| GitGuardian secret scan | GitHub PR | **Yes** |
| CodeQL default setup | push/PR/weekly | reports |
| Dependabot (Maven + npm) | weekly | opens PRs |
| FOSSA (license + vuln) | GitHub Action on `main` | scan-only (non-blocking) |
| JaCoCo coverage | Maven `verify` | report-only (no minimum yet) |
| Trivy `fs` | on-demand / `security_scan.sh` | HIGH/CRITICAL kept at 0 |
| Semgrep | `scripts/security_scan.sh` | metrics off; advisory |
| SonarQube (internal) | on-demand | dashboard only |

Integration tests run on **in-memory H2 only — no Docker / Testcontainers**
([ADR-0003](0003-embedded-h2-postgres-later.md)); a container start during
`verify` is a regression. Verification depth scales with net lines changed
(CLAUDE.md → "Verifying Code Changes").

**Authority rule (source-gate precedence).** Where prose documentation and the
running configuration disagree, **the configuration / source code is
authoritative** — consistent with [`SECURITY.md`](../../SECURITY.md), which
treats the implemented controls as the source of truth. Prose is then corrected
to match the config, or the config is fixed if it is the defect; the
disagreement is never left unresolved.

### Consequences

* Good — security is enforced continuously and automatically.
* Good — clear separation of blocking vs advisory controls.
* Bad — advisory-only items (coverage, Sonar gate, FOSSA) can regress silently;
  tracked as open gaps in [`SECURITY.md`](../../SECURITY.md).
* Bad — Trivy's `--output` leaves a stale report if its DB pull fails (missing
  `docker-credential-desktop`); neutralise the Docker config first
  (`export DOCKER_CONFIG=$(mktemp -d)`), as `security_scan.sh` does.

## Validation

Known doc-vs-config conflicts at time of writing (resolved in favour of source
per the authority rule):

1. **CircleCI npm install command.** `.circleci/config.yml` runs
   `npm ci --legacy-peer-deps --no-audit --no-fund`, but `CLAUDE.md` documents
   `npm install --legacy-peer-deps …` and explicitly warns that `npm ci` fails
   on Linux because the lockfile only pins the dev host's
   `@parcel/watcher-darwin-arm64` binary. The config is authoritative for *what
   runs today*, but this is flagged as a likely defect — either the config
   should move to `npm install`, or the lockfile must carry all platform
   `@parcel/watcher` entries. **Action: reconcile `.circleci/config.yml` and
   `CLAUDE.md`.**
2. **Auth endpoint name.** `SECURITY.md` references `/api/auth/me`; the source
   (`AuthController`) exposes `/api/auth/currentuser`. Source wins; `SECURITY.md`
   should be corrected.

## More Information

Source of truth: `.circleci/config.yml`, `.github/workflows/fossa.yml`,
`api/checkstyle/`, `scripts/security_scan.sh`, and the gate tables in
[`README.md`](../../README.md) / [`SECURITY.md`](../../SECURITY.md). A project on
an internal CI host would implement the same intent (continuous static analysis,
CVE scan, license check) with a different toolchain.
