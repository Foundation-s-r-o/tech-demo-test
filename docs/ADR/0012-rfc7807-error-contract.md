---
status: accepted
date: 2026-07-08
deciders: Foundation engineering + CTO
---
# RFC 7807 problem+json as the API error contract

## Context and Problem Statement

The civil-registry API ([ADR-0011](0011-civil-registry-schema-port.md)) surfaces several failure
modes — authorization denials, not-found, domain conflicts (bitemporal overlap, registry-number
reuse, already-merged person), bean-validation failures, and database CHECK/constraint violations.
The design contract (`db/openapi.yaml`) already specifies `application/problem+json`. How do error
responses stay consistent, machine-readable, and free of leaking SQL / stack internals?

## Decision Drivers

* One uniform, documented error shape for the SPA and any API consumer.
* Never leak persistence internals (SQL, constraint names, stack traces) to the client.
* Map each failure to a correct HTTP status without controllers hand-rolling responses.

## Considered Options

* **RFC 7807 `ProblemDetail` via a single `@RestControllerAdvice`** (Spring's built-in support).
* Ad-hoc per-controller error DTOs — rejected: drifts, duplicates, leaks easily.

## Decision Outcome

Chosen option: **RFC 7807 `application/problem+json`**, centralized in
[`ApiExceptionHandler`](../../api/src/main/java/sk/foundation/techdemo/infrastructure/api/ApiExceptionHandler.java)
(extending `ResponseEntityExceptionHandler`, so framework/validation errors are already rendered as
`ProblemDetail`). The application mappings:

| Exception | Status | Detail |
| --------- | ------ | ------ |
| `ConflictException` | 409 | domain message (e.g. overlap, registry-number reuse, merge chain) |
| `EntityNotFoundException` / `EmptyResultDataAccessException` | 404 | not-found message |
| `DataIntegrityViolationException` | 400 | **generic** message — no SQL or constraint internals |
| `AuthenticationException` / `UnauthorizedException` | 401 | "Invalid credentials" |
| `AccessDeniedException` | 403 | "Access is denied" |

### Consequences

* Good: consistent, self-describing errors; the DB CHECK safety net (e.g. a death record with no
  date/found/court basis) becomes a clean 400 instead of a 500 with a leaked SQL statement.
* Good: controllers throw typed exceptions; no per-endpoint error handling.
* Neutral: `DataIntegrityViolationException`→400 is deliberately generic; the specific cause is
  logged server-side, never returned.

### Related decisions recorded here (single home, cross-linked from ADR-0011)

* **Application-layer race control.** The PostgreSQL GiST-exclusion / merge-guard mechanisms are
  replaced by pessimistic row locks: `RegistryPersonApiService` holds a person-row lock before the
  bitemporal `BitemporalGuard` overlap check, and `PersonMergeService.merge` locks both the
  duplicate and primary person rows (in deterministic order, to avoid deadlock) before its
  uniqueness checks.
* **Minimal audit payloads.** Audit-chain entries store table/action + a small JSON id payload, not
  full row snapshots; the SHA-256 chain plus a chain-head tail check in `AuditService.verifyChain`
  detects both retroactive edits and truncation of the newest row.
* **Delete-by-convention.** Records are retired by supersede/anonymize rather than row `DELETE`;
  there are no hard-delete endpoints (T6 nuance; also noted in `db/openapi.yaml` deviations).

## More Information

Design contract: [`db/openapi.yaml`](../../db/openapi.yaml) (`errors are RFC 7807`); enforcement
mapping and the PostgreSQL re-hardening delta in [`db/DESIGN.md` §12](../../db/DESIGN.md).
