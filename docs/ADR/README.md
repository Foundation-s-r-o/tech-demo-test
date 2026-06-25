# Decisions

This directory contains architecture decision records (ADRs) for
`tech-demo-test`. Each file captures one decision: its context, the decision
itself, the consequences, and the options considered.

For new ADRs, use [adr-template.md](adr-template.md) as the basis (MADR 3.0.0).
More information on MADR is available at <https://adr.github.io/madr/>.
General information about architecture decision records is available at
<https://adr.github.io/>.

## How we use ADRs here

- One file = one decision. Sections follow the MADR template.
- Decisions already embodied in the source are recorded as `accepted`. Changing
  an accepted decision means a new ADR that supersedes the old one; accepted ADRs
  are not rewritten in place.
- **Source-gate precedence.** Where an ADR (or any prose doc) and the running
  source / configuration disagree, the source is authoritative — see
  [`SECURITY.md`](../../SECURITY.md). The prose is then corrected to match, or
  the config is fixed if it is the defect. See
  [0008](0008-strict-quality-security-gate.md) for current known conflicts.
- The high-level architecture overview that ties these ADRs together is
  [`docs/ARCHITECTURE.md`](../ARCHITECTURE.md).

## Index

| ADR | Decision | Status |
| --- | -------- | ------ |
| [0000](0000-use-markdown-any-decision-records.md) | Use Markdown Any Decision Records (MADR) | Accepted |
| [0001](0001-using-source-repo.md) | Source repository: GitHub | Accepted |
| [0002](0002-minimal-docker-free-stack.md) | Minimal, Docker-free local stack with an open upgrade path | Accepted |
| [0003](0003-embedded-h2-postgres-later.md) | Embedded H2 file-mode now, PostgreSQL via a `prod` profile later | Accepted |
| [0004](0004-spring-security-session-auth.md) | Identity via Spring Security session login; Keycloak/OIDC deferred | Accepted |
| [0005](0005-react-typescript-spa.md) | Frontend as a React + TypeScript SPA | Accepted |
| [0006](0006-rest-openapi-generated-client.md) | REST API with code-first OpenAPI and a generated TypeScript client | Accepted |
| [0007](0007-no-supported-deployment.md) | No supported deployment artifact by design | Accepted |
| [0008](0008-strict-quality-security-gate.md) | Strict quality & security gate; running config is the authority | Accepted |
| [0009](0009-observability-actuator-micrometer.md) | Observability via Actuator + Micrometer; full stack deferred | Accepted |

## Context

This is a **template** repository: a public, GitHub-hosted demo with no
real-world data. Several decisions here are deliberately scoped to that context
(GitHub hosting, embedded H2, no supported deployment) and a downstream project
with different constraints — regulated data, an on-premise target, an internal
CI host — is expected to make different calls and record them in its own ADRs.
All of these decisions descend from the same Foundation engineering principles:
KISS / YAGNI, proportionality and reversibility, and security as a permanent
priority that is never traded for convenience.
