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
| **JaCoCo** | Java coverage | Maven `verify` phase | Reports only (no minimum yet) |
| **ESLint + `tsc --noEmit`** | TypeScript types + style | `npm run lint` (local + CI) | Hard gate on errors |
| **Trivy (`fs`)** | OS / dep CVEs | On-demand local + ad-hoc CI | HIGH/CRITICAL = 0 maintained on `main` |
| **SonarQube** (internal, `10.16.35.93:9000`) | Static analysis (api + ui) | On-demand from dev machines | Dashboard only; api gate currently in `ERROR` on two conditions — new-code coverage (33.7% < 80%) and one new violation (> 0) |
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

### Authorization matrix

Enforced by request-matcher rules in `SecurityConfig`; the frontend mirrors these only for UX and
is never the enforcement point. Authorities are `ROLE_ADMIN` / `ROLE_USER`. The seeded local/it
account is `ADMIN`; no `USER` is provisioned by default.

| Operation | Endpoint | USER | ADMIN |
| --------- | -------- | :--: | :---: |
| List persons | `GET /api/persons` | ✅ | ✅ |
| Person detail | `GET /api/persons/{id}` | ✅ | ✅ |
| Create / update / delete | `POST/PUT/DELETE /api/persons/**` | ❌ | ✅ |
| PDF export | `GET /api/persons/{id}/pdf` | ❌ | ✅ |
| Operational endpoints | `/actuator/**` (non-health) | ❌ | ✅ |
| Health | `GET /actuator/health` | public | public |
| Session/identity | `/api/auth/me`, `/api/auth/logout` | ✅ | ✅ |

Reads are open to any authenticated role; all writes, PDF export, and operational endpoints are
ADMIN-only. Adjust the matchers and the negative tests in `PersonControllerIT` together if this
default changes.

## Local test account and production provisioning

`admin/admin` is strictly local test data. Migration `V4` deletes the historical migration seed
from every database. `LocalAdminInitializer` recreates it only when the `local` or `it` profile is
explicitly active. Never activate `local` in a deployed environment.

For production, prefer an external identity provider such as Keycloak/OIDC and provision
administrators in that system with MFA and auditable role assignment. If database-backed login
must be used temporarily:

1. Generate a unique random password in the deployment secret manager.
2. Run a one-time, access-controlled provisioning job inside the trusted environment.
3. Hash the password with BCrypt (cost 12 or higher) inside that job and insert the administrator;
   do not pass the plaintext through an image build argument, migration, repository file, or log.
4. Disable or remove the provisioning job after success and require password rotation/MFA at the
   identity layer as soon as available.
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

The 2026-06-12 review closed the critical dependency finding and all eight high-severity
findings (see `security_remediation_2026-06-12_094357.md`). The medium/low items are tracked
below. The remaining **open** items must be closed before this template is used as a
production-ready service; track them here, not only in the dated remediation report.

| Gap | Severity | Status | Note |
| --- | -------- | ------ | ---- |
| **Login throttling** | Medium | Open | No per-account or per-source rate limit / progressive delay. Add before exposing login beyond local/demo. |
| **Backend coverage gate** | Medium | Open | JaCoCo reports only; no minimum threshold. New-code coverage ~33.7% vs the 80% Sonar target. See "Future hardening" below. |
| **Role-based business authorization** | Medium | **Closed** | ADMIN/USER matrix enforced in `SecurityConfig` with negative tests in `PersonControllerIT`. See "Authorization matrix" above. |
| **Content Security Policy** | Medium | **Closed** | CSP set on API-origin responses (`script-src 'self'`, no `unsafe-eval`); Referrer-Policy and Permissions-Policy also present. HSTS still deferred to the TLS proxy. |
| **Email semantic validation** | Low | **Closed** | `@Email` enforced on `PersonModifyRequestDTO.email`; invalid format returns 400. |
| **Playwright storage-state hygiene** | Low | **Closed** | `ui/storageStateFresh.json` untracked and `ui/.gitignore` now covers `storageState*.json` at the `ui` root as well as `tests/`. |

## Future hardening (deferred — see `docs/UPGRADE_PLAN.md`)

- **Auth → Keycloak** (OIDC) for production identity, MFA, lifecycle, and audit controls.
- **DB → PostgreSQL** via a `prod` Spring profile. Flyway migrations are already
  portable ANSI SQL so the swap is meant to be a non-event.
- **Observability → OpenTelemetry + Prometheus + Loki + Jaeger.** Today only
  Actuator + Micrometer are wired.
- **Secrets** in a vault (HashiCorp Vault / cloud KMS) or SOPS-encrypted files.
  Never in `.env`, never inlined into shell `KEY=value` prefixes.
- **TLS termination**; the `prod` profile already marks the session cookie Secure.
- **Backend test coverage** — current new-code coverage is ~33%; lift toward the
  80% Sonar threshold as features grow.

## Out of scope for this repo

- Anything in `node_modules/` or `~/.m2/repository/` — those are caches, not source.
- Forks and downstream uses of this template — their security is their own.
- Local developer machines (shell snapshots, `~/.zshrc` exports, IDE plugins).
