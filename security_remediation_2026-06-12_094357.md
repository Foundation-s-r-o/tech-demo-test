# Security Remediation Report - Tech Demo Test

**Review date:** 2026-06-12 09:43 CEST  
**Repository:** `/Users/antony/fndt/demo/tech-demo-test`  
**Scope:** remediation of the critical and high findings from `security_review_2026-06-12_082510.md`  
**Overall result:** **HIGH-SEVERITY REMEDIATION PASSED WITH DOCUMENTED VERIFICATION LIMITATIONS**

## Status Legend

| Status | Meaning |
|---|---|
| PASSED | The control was implemented and its relevant local check passed |
| REMOVED | The unused vulnerable deployment surface was deleted |
| PARTIAL | The implementation is present, but one intended verification could not complete |
| ISSUES FOUND | A material issue remains |
| NOT APPLICABLE | The reviewed deployment mechanism is no longer used or present |

## Critical and High Findings

| ID | Original severity | Checked | Result | Remediation and evidence |
|---|---|---|---|---|
| SEC-01 | Critical | Yes | **PASSED** | Forced `shell-quote` to `^1.8.4`, regenerated the npm lockfile, and confirmed `npm audit` reports 0 vulnerabilities. Trivy reports 0 HIGH/CRITICAL dependency findings. |
| SEC-02 | High | Yes | **PASSED** | Actuator exposure is restricted to `health`; integration tests confirm health is public and `/actuator/loggers` is unauthorized and not exposed. |
| SEC-03 | High | Yes | **PASSED** | Enabled cookie/header CSRF protection, added the SPA CSRF bootstrap endpoint, configured Axios XSRF handling, and replaced wildcard credentialed CORS with exact configured origins. Tests confirm a missing CSRF token is rejected and an untrusted origin is forbidden. |
| SEC-04 | High | Yes | **PASSED** | Login invokes `SessionAuthenticationStrategy`; an integration test confirms an existing session ID changes after authentication. |
| SEC-05 | High | Yes | **PASSED** | Migration `V4` deletes the historical seeded administrator. `LocalAdminInitializer` creates `admin/admin` only under explicit `local` or `it` profiles. Migration and login tests passed. Production guidance is documented in `SECURITY.md`. |
| SEC-06 | High | Yes | **REMOVED** | Removed unused AWS/ECR deployment workflows, Docker build files, compose files, sample environment file, and the Docker UI runner. The remaining FOSSA workflow uses least-privilege permissions and immutable action SHAs. |
| SEC-07 | High | Yes | **REMOVED** | Removed the unused privileged observability compose service together with all compose configuration. Repository search found no remaining `privileged: true` deployment configuration. |
| SEC-08 | High | Yes | **NOT APPLICABLE** | Removed the unused root-running and stale Docker image definition. Repository search found no Dockerfile or compose manifest. |
| SEC-09 | High | Yes | **PASSED FOR RELEASE-BLOCKING RISK** | CircleCI now uses reproducible `npm ci`, runs blocking `npm audit --audit-level=high`, frontend lint/type checks, production build, and Maven verification. `npm ci --dry-run` passed. A local Semgrep/Trivy wrapper was added; automatic SAST execution in hosted CI remains a medium-priority improvement. |

## Local Test Administrator

`admin/admin` is **strictly local and integration-test data**. It is not created by the normal
migration result and must never be used in a deployed environment. Never activate the `local`
profile in production.

Recommended production design:

1. Prefer Keycloak or another OIDC provider for administrator identity, MFA, lifecycle control,
   role assignment, and authentication audit records.
2. If database-backed authentication is temporarily required, generate a unique password in a
   deployment secret manager and provision the first administrator through a one-time,
   access-controlled job.
3. Hash the secret inside the trusted runtime using BCrypt with cost 12 or higher. Never put the
   plaintext or hash in source control, Flyway migrations, image build arguments, or logs.
4. Disable the provisioning job after success, rotate the credential, and migrate to MFA-backed
   external identity as soon as practical.
5. Start deployed instances with the `prod` profile and set `APP_ALLOWED_ORIGINS` to exact HTTPS
   frontend origins.

## Other Security Controls Checked

| Area | Severity | Result | Evidence / residual issue |
|---|---|---|---|
| Production session cookie | Medium | **PASSED** | `prod` requires `Secure`; all profiles use HttpOnly and explicit SameSite. |
| H2 console | Low | **PASSED** | Disabled in all profiles. |
| Swagger/OpenAPI | Medium | **PASSED** | Disabled in `prod`. |
| Production source maps/eval | Medium | **PASSED** | Production webpack disables source maps; generated bundle search found no `eval` or inline source maps. |
| Frontend stale auth state | Low | **PASSED** | Failed session initialization clears persisted user data. |
| Error stack traces | Medium | **PASSED** | Server error stack trace inclusion is disabled. |
| Semgrep telemetry | Privacy | **PASSED - CONFIGURATION** | `SEMGREP_SEND_METRICS=off`, `--metrics off`, and `--no-trace` are enforced by `scripts/security_scan.sh`; `doctor` exports the environment override. |
| Role-based business authorization | Medium | **ISSUES FOUND** | Business endpoints still distinguish authenticated from anonymous users, but no documented ADMIN/USER operation matrix is enforced. |
| Login throttling | Medium | **ISSUES FOUND** | No account/source rate limit or progressive delay has been implemented. |
| Content Security Policy | Medium | **ISSUES FOUND** | Eval was removed from production output, but an application-specific CSP is not yet enforced. |
| Coverage security gate | Medium | **ISSUES FOUND** | Security behavior tests were expanded, but JaCoCo still has no minimum threshold and integration coverage reporting needs review. |
| Playwright storage-state hygiene | Low | **ISSUES FOUND** | The tracked `ui/storageStateFresh.json` should be removed and all generated storage-state paths ignored. |
| Email semantic validation | Low | **ISSUES FOUND** | Length/null validation exists; semantic email validation remains absent. |

## Verification Executed

| Command / check | Result |
|---|---|
| `./mvnw clean verify` | **PASSED:** 7 unit + 19 integration tests, 0 failures/errors, 0 Checkstyle violations |
| `npm audit` | **PASSED:** 0 vulnerabilities |
| `npm run lint` | **PASSED:** ESLint and TypeScript checks |
| `npm run build` | **PASSED:** production build; 24 existing Sass/performance warnings |
| `npm ci --legacy-peer-deps --ignore-scripts --no-audit --no-fund --dry-run` | **PASSED:** lockfile supports reproducible CI installation, including cross-platform optional packages |
| Trivy filesystem scan for vulnerabilities, secrets, and misconfiguration | **PASSED:** 0 HIGH/CRITICAL findings and no secret findings |
| Repository scan for disabled CSRF, wildcard CORS, Docker/compose, privileged mode, and AWS access-key workflow variables | **PASSED:** no active insecure configuration found |
| `git diff --check` | **PASSED** |
| Semgrep security/secrets/OWASP rule execution | **PARTIAL:** telemetry-off invocation reached rule download; sandbox DNS blocked `semgrep.dev`, so the updated rules were not executed locally |
| Final Playwright authentication rerun | **PARTIAL:** the secured login flow reached the authenticated home page before the final display-name-only fixture correction; a final clean rerun was blocked by restricted local port binding |

## Recommended Next Tests

1. Add authorization tests from a documented ADMIN/USER endpoint matrix before creating normal
   production users.
2. Add login throttling tests for per-account and per-source limits, progressive delay, recovery,
   and denial-of-service resistance.
3. Add production-profile startup tests that fail when `APP_ALLOWED_ORIGINS` is absent or contains
   a non-HTTPS or wildcard origin.
4. Add browser assertions for CSRF login/logout/write operations and production response headers,
   including CSP and HSTS behind the real TLS proxy.
5. Run `./scripts/security_scan.sh` in a network-enabled CI job with pinned Semgrep and Trivy
   versions, then retain the scan artifacts.
6. Run the complete Playwright suite in CI where ports `8080` and `8082` can be bound.

## Conclusion

The original critical dependency and all eight high-severity findings have been remediated or
removed from the repository. The remaining confirmed issues are medium or low severity and should
be addressed before treating this demo template as a production-ready service.
