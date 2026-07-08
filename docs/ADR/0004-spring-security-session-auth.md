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
* **Authorization** is per-operation via `@PreAuthorize` (`@EnableMethodSecurity`)
  over six domain roles — `CLERK`, `SUPERVISOR`, `AGENCY_READER`, `DPO_OFFICER`,
  `SECURITY_AUDITOR`, `CITIZEN` (`auth/Role.java`) — with service-layer office
  scoping for clerks and an own-data predicate for the citizen portal (the
  app-layer stand-in for PostgreSQL RLS; see
  [ADR-0011](0011-civil-registry-schema-port.md)). `USERS` is the single
  credential store; its `OFFICE_CODE` / `PERSON_UID` columns (migration `V7`)
  carry clerk scope and citizen identity. Authentication failures are recorded to
  `AUDIT.SECURITY_EVENT` (`auth/SecurityEventListener`). The full role matrix
  lives in [`SECURITY.md`](../../SECURITY.md).

**MFA does not require an external IdP.** Spring Security 7 (the line Spring Boot
4 ships) provides built-in multi-factor authentication — one-time tokens,
WebAuthn / passkeys, and TOTP, modelled as progressive granting of authorities
via `@EnableMultiFactorAuthentication` — so production MFA is added *in this
application*, not a reason to adopt Keycloak. The **OIDC boundary is still kept
open**: authorization lives in the app, so a later switch to an external IdP for
SSO, federation, or centralized user lifecycle would not require rewriting the
authorization logic — but it is now optional, not the MFA path.

The known **`admin`/`admin`** account is created **only** under the explicit
`local` and `it` profiles (`LocalAdminInitializer`, `@Profile("local | it")`).
Migration `V4` removes the historical seeded account from every database; the
default and `prod` profiles provision no known user.

### Consequences

* Good — one fewer stateful service; a single source of truth for users.
* Good — correct, complete controls demonstrated at the right layer.
* Good — production MFA is a native Spring Security 7 feature added in-app (no
  external IdP needed); adopting Keycloak/OIDC later for SSO / federation /
  lifecycle stays a config-level upgrade, not a rewrite.
* Bad — the app owns password reset / lockout surface that an IdP would provide;
  mitigated by using only built-in Spring Security mechanisms.
* Bad — **login throttling / rate limiting is not yet implemented** (open gap in
  [`SECURITY.md`](../../SECURITY.md)); must be closed before login is exposed
  beyond local/demo.

## Validation

Enforced and tested in source — `SecurityConfig` request matchers, the six-role
`@PreAuthorize` matrix, plus the negative/authorization integration tests. Where
this ADR and any prose doc disagree, the source (`SecurityConfig.java` +
`@PreAuthorize` annotations) is authoritative.

## More Information

A production deployment would enable MFA (e.g. for regulated data) with Spring
Security 7's built-in support — see
[Multi-Factor Authentication (Spring Security reference)](https://docs.spring.io/spring-security/reference/servlet/authentication/mfa.html)
and the [Spring Security 7 MFA announcement](https://spring.io/blog/2025/10/21/multi-factor-authentication-in-spring-security-7/).
An external IdP (optional — SSO / federation / lifecycle) is tracked in
[`docs/UPGRADE_PLAN.md`](../UPGRADE_PLAN.md) and [`SECURITY.md`](../../SECURITY.md).
