---
status: accepted
date: 2026-06-25
deciders: Foundation engineering + CTO
---
# Identity via Spring Security session login; Keycloak/OIDC deferred

## Context and Problem Statement

The template needs authentication and authorization for a single SPA talking to
a single API. A full identity provider (Keycloak) would add a stateful service
to run, back up, and upgrade — at odds with the minimal-stack premise
([ADR-0002](0002-minimal-docker-free-stack.md)) — and would split the user model
across two systems. But the template must still demonstrate authentication,
role-based authorization, CSRF, and session hygiene done correctly.

## Decision Drivers

* Minimal moving parts; no extra stateful service for one app
  ([ADR-0002](0002-minimal-docker-free-stack.md)).
* Security configured completely and **in the correct layer** (CLAUDE.md,
  [`SECURITY.md`](../../SECURITY.md)).
* A clean upgrade path to a real IdP for production.
* Demo relaxations must be explicit and profile-scoped.

## Considered Options

* **Spring Security session login, users in DB.**
* **Keycloak / external OIDC provider** from day one.
* **Stateless JWT bearer tokens** held in the browser.

## Decision Outcome

Chosen option: **Spring Security session login with DB-backed users**, realised
in `auth/SecurityConfig.java`:

* Passwords are **BCrypt**-hashed; users load from the DB via
  `AppUserDetailsService` / `UserRepository`.
* **Session cookie** auth (not browser-held JWT). Login rotates the session ID
  (`ChangeSessionIdAuthenticationStrategy`).
* **CSRF** via a cookie/header token (`CookieCsrfTokenRepository`); the SPA reads
  the token from `GET /api/auth/csrf` and echoes it in `X-XSRF-TOKEN`.
* **CORS** is configured in the security filter chain (`.cors(...)`), exact-origin
  and credentialed, so preflight is handled *before* authorization —
  not in the MVC layer (`WebConfiguration` deliberately omits it).
* **Security headers**: CSP (`script-src 'self'`, no `unsafe-eval`),
  `X-Frame-Options: DENY`, `Referrer-Policy`, `Permissions-Policy`.
* A 401 entry point — the API never redirects to a login page.

The **OIDC boundary is kept open**: authorization lives in the app, so a future
switch to Keycloak/OIDC for production identity, MFA, and lifecycle does not
require rewriting the authorization logic.

The known **`admin`/`admin`** account is created **only** under the explicit
`local` and `it` profiles (`LocalAdminInitializer`, `@Profile("local | it")`).
Migration `V4` removes the historical seeded account from every database; the
default and `prod` profiles provision no known user.

### Consequences

* Good — one fewer stateful service; a single source of truth for users.
* Good — correct, complete controls demonstrated at the right layer.
* Good — production identity (Keycloak/OIDC, MFA, audit) is a config-level
  upgrade, not a rewrite.
* Bad — the app owns password reset / lockout surface that an IdP would provide;
  mitigated by using only built-in Spring Security mechanisms.
* Bad — **login throttling / rate limiting is not yet implemented** (open gap in
  [`SECURITY.md`](../../SECURITY.md)); must be closed before login is exposed
  beyond local/demo.

## Validation

Enforced and tested in source — `SecurityConfig` request matchers plus the
negative tests in `PersonControllerIT`. Where this ADR and any prose doc
disagree, the source (`SecurityConfig.java`) is authoritative.

## More Information

A production deployment would add mandatory MFA at the identity layer (e.g. for
regulated data). Deferred production identity is tracked in
[`docs/UPGRADE_PLAN.md`](../UPGRADE_PLAN.md) and
[`SECURITY.md`](../../SECURITY.md).
