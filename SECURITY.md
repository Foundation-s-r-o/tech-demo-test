# Security Policy

## About this repository

`tech-demo-test` is the Foundation s.r.o. reference template for modern backend +
frontend projects. It is a **rolling demo**, not a released product: there is one
supported branch (`main`), no LTS line, and parts of the stack are deliberately
weakened so the inner loop stays fast (see "Demo-mode caveats" below).

If you are forking this repo as a starting point for a real service, treat every
caveat below as a hardening checklist, not a guideline.

## Reporting a Vulnerability

**Use GitHub's Private Vulnerability Reporting.**

Open the repository's **Security** tab → **Report a vulnerability**. This sends a
private advisory to the maintainers with a built-in discussion thread and CVE
coordination path. Direct link:

[github.com/Foundation-s-r-o/tech-demo-test/security/advisories/new](https://github.com/Foundation-s-r-o/tech-demo-test/security/advisories/new)

Please include, at minimum:

- A short description of the issue and its impact.
- Affected file paths, endpoints, commit SHA, or version.
- Reproduction steps (proof of concept welcome but not required).
- Any suggested remediation if you have one.

**What to expect** (best effort, this is a template repo, not an enterprise product):

- Acknowledgement of receipt within **5 working days**.
- Initial triage (accepted / declined / need-more-info) within **10 working days**.
- For accepted reports, a fix or mitigation merged to `main` as soon as feasible,
  followed by an advisory credit if you wish.

We do not run a bug bounty.

## Supported Versions

Only the current `main` branch is supported.

| Branch / tag | Supported |
| ------------ | --------- |
| `main` (latest) | yes |
| Anything else (older commits, archived branches, forks) | no |

This repo does not cut versioned releases. Downstream projects forked from this
template are responsible for their own version policy and security posture.

## Security tooling in use

| Scanner | Scope | Where it runs | Gate behaviour |
| ------- | ----- | ------------- | -------------- |
| **GitGuardian** | Secrets in commits | GitHub PR check | Blocks PR on detected secrets |
| **Dependabot** | Maven + npm dep bumps | Weekly schedule | Opens PRs; humans review/merge |
| **FOSSA** | Dependency licences + vulns | GitHub Actions `fossa-scan` on push/PR to `main` | Non-blocking scan (no `run-tests`) — false positives from a stale upstream patent-rights policy are kept out of the gate |
| **Checkstyle** | Java style + structure | Maven `validate` phase | Hard gate (`failOnViolation=true`) |
| **JaCoCo** | Java coverage | Maven `verify` phase | Hard gate: merged unit+IT line coverage ≥ 70% (`jacoco.line-coverage.minimum` in `api/pom.xml`; ratchet toward 80%) |
| **ESLint + `tsc --noEmit`** | TypeScript types + style | `npm run lint` (local + CI) | Hard gate on errors |
| **Trivy (`fs`)** | OS / dep CVEs | On-demand local + ad-hoc CI | HIGH/CRITICAL = 0 maintained on `main` |
| **SonarQube** (internal, `10.16.35.113:9000`) | Static analysis (api + ui) | On-demand from dev machines | Dashboard only; api gate currently in `ERROR` on two conditions — new-code coverage (33.7% < 80%) and one new violation (> 0) |
| **CircleCI** | Backend `mvn verify` + frontend webpack build | On every push | Hard gate |

Notes:

- The Dependabot npm runs require a public-registry lockfile. The `ui/package-lock.json`
  is regenerated against `https://registry.npmjs.org/` (no internal feed URLs in
  `resolved` entries) — keep it that way.
- The FOSSA workflow runs scan-only because three packages (`react-bootstrap`,
  `prop-types-extra`, `uncontrollable`) are flagged under FOSSA's legacy
  `facebook-patent-rights-2` policy even though they have been MIT-licensed since
  September 2017. Re-enable `run-tests: true` once the FOSSA project policy is
  corrected upstream.

## Implemented application controls

- CSRF uses a cookie/header token flow; the SPA obtains a token from `/api/auth/csrf`.
- Credentialed CORS accepts only exact origins from `app.security.allowed-origins`.
- Login rotates an existing session ID.
- Only Actuator health is exposed publicly; operational endpoints require authentication and are
  not exposed by the endpoint configuration.
- Role-based authorization is enforced server-side in `SecurityConfig` (see the matrix below).
- A Content-Security-Policy is set on API-origin responses, alongside Referrer-Policy,
  Permissions-Policy, and `X-Frame-Options: DENY`.
- `email` fields are validated for format (`@Email`), not only length.
- Session cookies are HttpOnly with explicit SameSite; the `prod` profile also requires Secure.
- H2 console is disabled in every profile.
- Swagger/OpenAPI are disabled in the `prod` profile.
- Semgrep repository scans use `SEMGREP_SEND_METRICS=off`, `--metrics off`, and `--no-trace`
  through `scripts/security_scan.sh` and `doctor`.
- **Application-layer access control (civil-registry port).** The registry schema's PostgreSQL
  controls — row-level security, DB group roles/grants, audit + no-delete triggers, the pgcrypto
  hash chain, and GiST exclusion / partial-unique constraints — are **not** in the H2 schema; they
  are enforced in the application layer instead (Spring Security `@PreAuthorize`, service-layer
  office scoping, `BitemporalGuard`, `AuditService` hash chain, `PersonMergeService`). A non-app
  database client therefore bypasses scoping and tamper-evidence. Re-add the in-DB controls on the
  PostgreSQL switch — see the **PostgreSQL re-hardening delta** in [`db/DESIGN.md`](db/DESIGN.md).
- **National-id encryption keys.** The national identifier is stored AES-256-GCM-encrypted with a
  keyed HMAC for lookup (`NationalIdCryptoService`). Keys are read from configuration
  (`registry.crypto.national-id.enc-key` / `.hmac-key`); **fixed dev keys ship only with the
  `local`/`it`/`e2e` profiles** (in the profile property files). In every other profile a blank key
  fails fast at startup — production must supply real keys via environment/secret manager.

### Authorization matrix

Registry authorization is enforced **server-side per operation** by method-level `@PreAuthorize`
on the controllers (mirroring the contract's `x-required-roles`), plus service-layer office scoping
for clerks; `SecurityConfig` handles only the public paths, `/actuator/**` (SUPERVISOR), and the
"authenticated" default. The frontend mirrors this only for UX and is never the enforcement point.
Authorities are `ROLE_<name>` for the six registry roles.

| Area | Endpoints | Roles |
| ---- | --------- | ----- |
| Persons (write) | `POST /api/persons`, `…/details`, `…/addresses`, `…/religion` | CLERK, SUPERVISOR |
| Persons (read) | `GET /api/persons`, `/{uid}`, `…/details/addresses/religion`, `/{uid}/pdf` | CLERK, SUPERVISOR, DPO_OFFICER |
| Vital records (register) | `POST /api/{births,marriages,deaths}` | CLERK (own office), SUPERVISOR |
| Vital records (amend) | `POST /api/{…}/{id}/amendments` (reason mandatory) | SUPERVISOR |
| Vital records (read) | `GET /api/{births,marriages,deaths}/{id}` | CLERK (own office), SUPERVISOR, DPO_OFFICER |
| Death medical (Art. 9) | `POST/GET /api/deaths/{id}/medical` | CLERK, SUPERVISOR, DPO_OFFICER (read) |
| GDPR workflow | `/api/gdpr/requests`, `/disclosures`, `/persons/{uid}/export` | DPO_OFFICER |
| Consents | `/api/gdpr/consents`, `…/withdrawal` | CLERK, SUPERVISOR, DPO_OFFICER |
| Audit trail | `/api/audit/events`, `/access-log`, `/chain/verification` | DPO_OFFICER, SECURITY_AUDITOR |
| Codelists | `GET /api/codelists`, `…/items` | any authenticated |
| Agency (minimal view) | `GET /api/agency/persons/{uid}` | AGENCY_READER (current-state projection only — no Art. 9 data, history, or vital records) |
| Citizen portal | `GET /api/me`, `/me/access-log` | CITIZEN (own data, portal-eligible) |
| Health | `GET /actuator/health` | public |

Personal-data reads additionally require an `X-Access-Purpose` header (missing → 400 problem+json)
and are recorded in the read-access log. Clerks are scoped to their own registry office for vital
records; supervisors work nationwide.

Authentication failures are recorded to `AUDIT.SECURITY_EVENT` by `SecurityEventListener`
(event type `login_failure`, attempted username + source address, **no password or other PII** —
NIS2 monitoring story). The write is best-effort: a failure to persist the security event is logged
and swallowed so it can never change the authentication outcome.

## Local test accounts and production provisioning

Six demo accounts are seeded **only** under the `local` / `it` profiles (`LocalAdminInitializer`),
one per role, and are strictly local test data:

| Account | Role | Note |
| ------- | ---- | ---- |
| `admin/admin` | SUPERVISOR | central authority |
| `user/user` | CLERK | scoped to Bratislava office `2957` |
| `dpo/dpo` | DPO_OFFICER | GDPR workflow |
| `auditor/auditor` | SECURITY_AUDITOR | audit/security logs |
| `agency/agency` | AGENCY_READER | minimal current-state view |
| `citizen/citizen` | CITIZEN | linked to a seeded portal-eligible person |

Migration `V4` deletes the historical migration seed from every database; `LocalAdminInitializer`
recreates these accounts only when the `local` or `it` profile is explicitly active. Never activate
`local` in a deployed environment.

For production, enable **MFA using Spring Security 7's built-in support** (one-time tokens,
WebAuthn / passkeys, TOTP — no external identity provider required). An external IdP
(Keycloak/OIDC) is optional, for SSO, federation, and centralized user lifecycle. If
database-backed login must be used temporarily:

1. Generate a unique random password in the deployment secret manager.
2. Run a one-time, access-controlled provisioning job inside the trusted environment.
3. Hash the password with BCrypt (cost 12 or higher) inside that job and insert the administrator;
   do not pass the plaintext through an image build argument, migration, repository file, or log.
4. Disable or remove the provisioning job after success and require password rotation plus MFA
   (native Spring Security 7 MFA, or at the external IdP if one is adopted) as soon as feasible.
5. Start with the `prod` profile and set `APP_ALLOWED_ORIGINS` to exact HTTPS origins.

The repository intentionally does not provide a generic production bootstrap script because such
a script would become a reusable privileged credential path without a defined deployment platform.

## Stack baseline

| Layer | Version | Notes |
| ----- | ------- | ----- |
| Java | **21 LTS** | Pinned in `api/.sdkmanrc`; Boot 4 also builds on 25 |
| Spring Boot | **4.0.6** | Do not chase 4.1+ until pinned-stable |
| Spring Security | session login + BCrypt + CSRF | Local test user only under `local` / `it` |
| Hibernate ORM | 7.x | naming strategy: `PhysicalNamingStrategyStandardImpl` (portable) |
| Jackson | **3.x** (`tools.jackson`) | Boot 4 default |
| Flyway | portable ANSI SQL migrations | H2 today, PostgreSQL tomorrow |
| H2 | file-mode (`./data/`, gitignored) | local development DB |
| React | **19.2.x** | classic JSX transform |
| Node | **>= 22.11.0** | enforced via `ui/package.json` `engines` |
| Bundler | webpack 5 | sass-loader 17, modern Sass API |

## Known residual gaps

This table is the single home for the medium/low hardening items. (The initial baseline review's
critical dependency finding and all eight high-severity findings were closed during security
hardening.) The remaining **open** items must be closed before this template is used as a
production-ready service.

| Gap | Severity | Status | Note |
| --- | -------- | ------ | ---- |
| **Login throttling** | Medium | Open | No per-account or per-source rate limit / progressive delay. Add before exposing login beyond local/demo. |
| **Backend coverage gate** | Medium | **Closed** | `jacoco:check` on `verify` enforces merged unit+IT line coverage ≥ 70% (77.9% measured at introduction). Ratchet `jacoco.line-coverage.minimum` toward the 80% Sonar target. |
| **Role-based business authorization** | Medium | **Closed** | ADMIN/USER matrix enforced in `SecurityConfig` with negative tests in `PersonControllerIT`. See "Authorization matrix" above. |
| **Content Security Policy** | Medium | **Closed** | CSP set on API-origin responses (`script-src 'self'`, no `unsafe-eval`); Referrer-Policy and Permissions-Policy also present. HSTS still deferred to the TLS proxy. |
| **Email semantic validation** | Low | **Closed** | `@Email` enforced on `PersonModifyRequestDTO.email`; invalid format returns 400. |
| **Playwright storage-state hygiene** | Low | **Closed** | `ui/storageStateFresh.json` untracked and `ui/.gitignore` now covers `storageState*.json` at the `ui` root as well as `tests/`. |

## Future hardening (deferred — see `docs/UPGRADE_PLAN.md`)

- **MFA** via Spring Security 7's built-in support (OTT / passkeys / TOTP) — added in-app, no
  external IdP required. An external IdP (Keycloak / OIDC) is optional, for SSO / federation /
  lifecycle / audit.
- **DB → PostgreSQL** via a `prod` Spring profile. Flyway migrations are already
  portable ANSI SQL so the swap is meant to be a non-event.
- **Observability → OpenTelemetry + Prometheus + Loki + Jaeger.** Today only
  Actuator + Micrometer are wired.
- **Secrets** in a vault (HashiCorp Vault / cloud KMS) or SOPS-encrypted files.
  Never in `.env`, never inlined into shell `KEY=value` prefixes.
- **TLS termination**; the `prod` profile already marks the session cookie Secure.
- **Backend test coverage** — merged unit+IT line coverage is gated at ≥ 70%
  (`jacoco:check` on `verify`); lift the floor toward the 80% Sonar threshold as
  features grow.

## Out of scope for this repo

- Anything in `node_modules/` or `~/.m2/repository/` — those are caches, not source.
- Forks and downstream uses of this template — their security is their own.
- Local developer machines (shell snapshots, `~/.zshrc` exports, IDE plugins).
