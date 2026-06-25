---
status: accepted
date: 2026-06-25
deciders: Foundation engineering + CTO
---
# No supported deployment artifact by design

## Context and Problem Statement

The repository previously carried Docker, Compose, ECR, and Elastic Beanstalk
scaffolding plus reference observability configs. As a *template*, shipping a
deployment definition is actively harmful: it would be copied verbatim into real
projects without a threat model, a `prod` profile, secret management, TLS, or an
identity-provisioning design — turning convenience scaffolding into an unowned
production path. A project with a known target environment can make a concrete
deployment call (e.g. fat JAR + systemd + reverse proxy); a template has no such
target, so any deployment definition it shipped would be a guess.

## Decision Drivers

* A template must not imply a production posture it has not designed.
* Security is never traded for convenience (CLAUDE.md,
  [`SECURITY.md`](../../SECURITY.md)).
* Minimal moving parts ([ADR-0002](0002-minimal-docker-free-stack.md)).

## Considered Options

* **No supported deployment definition; document the path instead.**
* **Keep generic Docker/Compose/cloud scaffolding** as a starting point.
* **Ship an opinionated reference deployment** (e.g. fat JAR + systemd).

## Decision Outcome

Chosen option: **no supported deployment definition.** The stale Docker /
Compose / ECR / Elastic Beanstalk scaffolding was removed.
`run_application.sh` is local-only; the `grafana/`, `loki/`, `prometheus/`, and
`otel-config.yml` files are reference inputs, not a supported stack.

A real deployment must start from a documented threat model, the `prod` profile,
external secret management, TLS termination, and an explicit
identity-provisioning design (see [`SECURITY.md`](../../SECURITY.md) →
"Local test account and production provisioning"). The repository intentionally
ships **no** generic production bootstrap script, because such a script becomes a
reusable privileged-credential path without a defined platform.

### Consequences

* Good — no copy-paste production trap; downstream projects must consciously
  design their deployment.
* Good — the repo stays small and honest about what it is (a rolling demo on
  `main`).
* Bad — there is no one-command deploy; accepted, because a template's job is to
  be cloned and then hardened, not deployed as-is. The hardening checklist and
  deferred phases live in [`SECURITY.md`](../../SECURITY.md) and
  [`docs/UPGRADE_PLAN.md`](../UPGRADE_PLAN.md).

## More Information

CI builds and *verifies* the artifact (CircleCI `mvn verify` + webpack build,
see [ADR-0008](0008-strict-quality-security-gate.md)) but does not deploy it.
Source of truth: `README.md` → "Deployment status", and the absence of any
deployment workflow in `.github/workflows/`.
