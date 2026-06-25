---
status: accepted
date: 2026-06-25
deciders: Foundation engineering + CTO
---
# Minimal, Docker-free local stack with an open upgrade path

## Context and Problem Statement

`tech-demo-test` is Foundation's reference template for new backend + frontend
projects. The dominant cost in day-to-day work is the local inner loop — build,
run, test, repeat. Heavyweight local infrastructure (Docker Compose, external
databases, message brokers, an identity server) slows that loop and raises the
onboarding cost for every project cloned from the template.

How simple should the default stack be, and how do we keep that simplicity from
blocking a future production deployment?

This ADR is the overarching premise for the template; the other ADRs
(`0003`–`0009`) are concrete choices that fill it in. The premise is *simplest
infrastructure with reversible decisions*, scoped to a template whose target is
*fast iteration*, not a production deployment.

## Decision Drivers

* Fast local iteration is paramount — two terminals, nothing else.
* KISS / YAGNI: no component until a present requirement needs it.
* Reversibility: simplicity must not block a known future upgrade.
* The template sets the bar every downstream project inherits.

## Considered Options

* **Docker-free local stack** (embedded H2, two dev servers, no external infra).
* **Docker Compose for local dev** (app + Postgres + Redis + observability).
* **Full production-shaped local stack** (Postgres, Keycloak, OTel collector).

## Decision Outcome

Chosen option: **Docker-free local stack**. Local development runs as two
processes — the Spring Boot API (`:8082`, profile `local`) and the webpack dev
server (`:8080`) — over embedded H2 in file mode, with no Docker, no `.env`, and
no external services. Speed comes from *removing* infrastructure, not from
loosening the build gate (see [ADR-0008](0008-strict-quality-security-gate.md)).

Reversibility is held open by deliberate design choices recorded in sibling
ADRs: profile-driven datasource/dialect and portable migrations
([ADR-0003](0003-embedded-h2-postgres-later.md)), an auth model that keeps an
OIDC boundary open ([ADR-0004](0004-spring-security-session-auth.md)), and a
Micrometer/Actuator surface a real observability stack can attach to later
([ADR-0009](0009-observability-actuator-micrometer.md)).

### Consequences

* Good — minimal onboarding, fast feedback, small surface for environment bugs.
* Good — every known upgrade has a clear, documented path; none is foreclosed.
* Bad — local does not mirror production topology, so some integration risk
  surfaces only later. Accepted: integration tests still run on H2, and the
  upgrade path is documented in [`docs/UPGRADE_PLAN.md`](../UPGRADE_PLAN.md).
* Bad — reversibility is not free; it requires discipline (env-var config,
  OIDC boundary, portable SQL) even while nothing exercises it yet.

## More Information

Realised by the source: `application.properties` (H2 file-mode, no Docker),
`api/.sdkmanrc` (Java 21 LTS), and the two-terminal workflow in
[`README.md`](../../README.md) / [`CLAUDE.md`](../../CLAUDE.md). This premise is
specific to a template context — a public, GitHub-hosted repo
([ADR-0001](0001-using-source-repo.md)) with no real-world data; a downstream
project with different constraints is expected to revisit it.
