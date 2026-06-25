---
status: accepted
date: 2026-06-25
deciders: Foundation engineering + CTO
---
# Embedded H2 file-mode now, PostgreSQL via a prod profile later

## Context and Problem Statement

The template needs a database for local development, integration tests, and an
eventual production deployment. Requiring a real PostgreSQL (in Docker or
installed) for every clone would violate the Docker-free premise
([ADR-0002](0002-minimal-docker-free-stack.md)) and slow the inner loop. At the
same time, the production target for downstream projects is PostgreSQL, and the
schema must not get locked to a single vendor.

How do we get a zero-infra local DB without painting the project into an
H2-only corner?

## Decision Drivers

* Docker-free, fast local loop ([ADR-0002](0002-minimal-docker-free-stack.md)).
* The real production target is PostgreSQL.
* Migrations and the data model must stay portable across both engines.
* Integration tests must run without Docker / Testcontainers.

## Considered Options

* **Embedded H2 (file-mode locally, in-memory for tests) + portable migrations.**
* **PostgreSQL everywhere, via Docker / Testcontainers.**
* **H2-only, accept vendor lock-in.**

## Decision Outcome

Chosen option: **embedded H2 with portability discipline**. H2 runs in file mode
for local dev (`./api/data/`, gitignored) and in-memory for tests (`e2e` profile:
`jdbc:h2:mem:e2e`). PostgreSQL is deferred to a `prod` profile.

The lock-in is avoided by hard rules, all enforced in source:

* Flyway owns the schema (`ddl-auto=validate`); migrations are **portable ANSI
  SQL** with no vendor-specific types.
* Datasource URL and Hibernate dialect are **profile-driven**, never hardcoded
  into shared config.
* `PhysicalNamingStrategyStandardImpl` + `globally_quoted_identifiers=false` keep
  identifiers unquoted and case-portable, so the same DDL runs on PostgreSQL.

The PostgreSQL swap is meant to be a *non-event*: add `prod` datasource +
dialect, run the existing migrations.

### Consequences

* Good — zero local DB infra; integration tests run on H2 with no Docker.
* Good — the PostgreSQL path is open without a schema rewrite.
* Bad — H2 and PostgreSQL differ in edge semantics (types, constraints,
  concurrency), so the swap still needs PostgreSQL-backed integration coverage
  before it is trusted. Tracked in [`docs/UPGRADE_PLAN.md`](../UPGRADE_PLAN.md).
* Bad — features that would benefit from PostgreSQL-only capabilities (e.g.
  Row-Level Security, JSONB, partial indexes) cannot be used while H2 is in the
  matrix. Accepted for a template; a downstream project needing them opens a new
  ADR (a regulated project that mandates PostgreSQL + RLS, for example, cannot
  make this trade and would choose PostgreSQL everywhere).

## More Information

Source of truth: `application.properties`, `application-prod.properties`,
`application-e2e.properties`, and the migrations under `db/migration/`. A
regulated, RLS-dependent project would deliberately make the *opposite* call
("no embedded DB, not even in tests"); this template trades production-fidelity
for iteration speed because it has no real data to protect.
