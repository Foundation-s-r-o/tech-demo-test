---
status: accepted
date: 2026-06-25
deciders: Foundation engineering + CTO
---
# REST API with code-first OpenAPI and a generated TypeScript client

## Context and Problem Statement

The React SPA ([ADR-0005](0005-react-typescript-spa.md)) talks to the Spring Boot
API exclusively over HTTP. The contract between them must stay in sync as both
sides evolve, and hand-maintaining either a spec or a client is a drift risk.

## Decision Drivers

* One source of truth for the API contract; no FE/BE drift.
* Minimal ceremony (KISS) — avoid maintaining a hand-written spec by hand.
* Type safety on the client without manual transcription.

## Considered Options

* **REST + code-first OpenAPI** (spec generated from Spring annotations) → generated TS client.
* **Contract-first OpenAPI** (hand-written spec → generated server and client).
* **GraphQL.**

## Decision Outcome

Chosen option: **REST with code-first OpenAPI**. The contract is generated from
Spring annotations (springdoc) and published at `/v3/api-docs` (Swagger UI at
`/swagger-ui.html`, both disabled under `prod`). The frontend regenerates a
typed **Axios** client from that spec via `npm run generate-openapi-services`
(`openapi-generator-cli`, `typescript-axios`), into `ui/src/api/generated/`.

Rules:

* **Generated code is not hand-edited.** The shared Axios instance
  (`common/axiosClient.ts` — credentialed, CSRF header, date/empty-param
  handling) is injected via `api/api.ts`.
* Authentication against the API is by **session cookie**
  ([ADR-0004](0004-spring-security-session-auth.md)), not a browser-held token.

### Consequences

* Good — single contract source; the generated client removes a class of manual
  errors and keeps the SPA in lockstep with the API.
* Bad — code-first means the contract follows the implementation; mitigated by
  reviewing the OpenAPI diff whenever an endpoint changes.

## More Information

Source: `PersonController` / `AuthController` annotations, `ui/src/api/`, and the
`generate-openapi-services` script.
