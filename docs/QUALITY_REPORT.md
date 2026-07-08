# Quality & Security Scan Report — tech-demo-test

Run: 2026-05-27 · SonarQube host: `http://10.16.35.93:9000` (internal instance)

Tools: ESLint + tsc (ui), Checkstyle + JaCoCo (api), Trivy (fs vuln), SonarQube (api + ui).
Raw artifacts in this folder: `trivy-report.txt`, `sonar-metrics-*.json`, and compact `sonar-issues-*.json` snapshots filtered to active BLOCKER/CRITICAL legacy severity or BLOCKER/HIGH impact severity.

## Remediation results (2026-05-27)

Items 1–5 of the recommendations were executed and re-verified. Before → after:

| Metric | Before | After |
|---|---|---|
| ESLint errors (ui) | 11 | **0** |
| Trivy CRITICAL | 5 | **0** |
| Trivy HIGH | 49 | **5** (no fix available — see below) |
| Trivy MEDIUM / LOW | 52 / 14 | 16 / 4 |
| Sonar api CRITICAL smells | 4 | **0** |
| Sonar api code smells | 8 | 4 |
| Sonar ui code smells | 72 | 69 |

**5 remaining HIGH have no fix within the agreed constraints (stay on Spring Boot 3.4.x, minimal bumps):**
- `spring-boot` / `spring-boot-starter-actuator` 3.4.13 — CVE-2026-40973, CVE-2026-22731, CVE-2026-22733 only fixed in 3.5.x / 4.0.x. Requires a major-line upgrade → out of scope.
- `lodash` / `lodash-es` 4.17.21 — CVE-2026-4800; the listed fix `4.18.0` is **not released** (latest is 4.17.21). No fix available.

**Verification:** `npm run lint` (0 errors), `npm run build` (compiles, pre-existing SCSS/bundle warnings only), `mvn clean test` (1 unit test pass, 0 checkstyle violations, integration-test sources compile). ⚠️ **Integration tests were NOT run — Docker/Testcontainers unavailable in this environment.** Trivy + SonarQube re-run; artifacts in this folder refreshed.

## Summary (post-remediation)

| Tool | api | ui |
|---|---|---|
| Build / compile | ✅ pass (1 unit test, 0 checkstyle violations) | ✅ tsc + build pass |
| ESLint | n/a | ✅ **0 errors** |
| Sonar bugs | 0 | 3 |
| Sonar vulnerabilities | 0 | 0 |
| Sonar security hotspots | 1 (hardcoded password) | 0 |
| Sonar code smells | 4 | 69 |
| Sonar coverage | 5.7% | 0% |
| Sonar ratings (rel/sec/maint) | 1.0 / 1.0 / 1.0 | 3.0 / 1.0 / 1.0 |
| Trivy | — combined: **0 CRITICAL, 5 HIGH, 16 MEDIUM, 4 LOW** — |

Sonar dashboards: [tech-demo-api](http://10.16.35.93:9000/dashboard?id=tech-demo-api) · [tech-demo-ui](http://10.16.35.93:9000/dashboard?id=tech-demo-ui)

## ESLint (ui) — 11 errors, must fix

| File:line | Rule |
|---|---|
| `common/auth/AuthProvider.tsx:77` | no-unused-vars (`err`) |
| `components/form/types.tsx:40,41,48,91` (×8) | no-explicit-any |
| `components/shared/table/TableWrapper.tsx:158` | no-unused-vars (`e`) |
| `pages/login/LoginForm.tsx:30` | no-unused-vars (`error`) |

Quick wins: rename/remove the 3 unused catch vars; type the `any`s in `form/types.tsx`.

## Trivy — HIGH/CRITICAL highlights

Backend (pom):
- **tomcat-embed-core 10.1.35** — 3 CRITICAL + 13 HIGH. The pom pins `tomcat.version=10.1.35`; bump to **10.1.55**.
- spring-boot 3.4.3 → 3.4.5; spring-core 6.2.3 → 6.2.11; spring-security-crypto 6.4.3 → 6.4.4; bcprov-jdk18on 1.80 → 1.84; netty-codec 4.1.118 → 4.1.133.

Frontend (npm):
- **form-data 4.0.0** (CRITICAL, transitive) → needs override to ≥4.0.4.
- **basic-ftp 5.0.5** (CRITICAL, transitive) → ≥5.2.0.
- axios 1.8.1 → ≥1.12 (multiple HIGH); react-router 7.2.0 → ≥7.12; lodash 4.17.21 → 4.18; minimatch, path-to-regexp also flagged.

Many npm findings are transitive — pin via `"overrides"` in `ui/package.json` (per fndt/CLAUDE.md guidance), then `npm install --legacy-peer-deps`.

## Sonar — active critical/high issues

The checked-in `sonar-issues-*.json` snapshots are intentionally compact. They
include only active issues with legacy severity BLOCKER/CRITICAL or impact
severity BLOCKER/HIGH.

- api: no active critical/high issues in the current checked-in snapshot.
- ui: 3 active HIGH-impact code smells remain; see
  `docs/sonar-issues-tech-demo-ui.json`.

## Recommendations (priority order)

> **Status:** Items 1–5 executed and verified on 2026-05-27 (see *Remediation results* above). Item 6 deferred.

| # | Recommendation | Status |
|---|---|---|
| 1 | Fix the 11 ESLint errors (blocks `npm run lint` gate) | ✅ Done — 0 errors |
| 2 | Bump tomcat to 10.1.55 | ✅ Done — cleared 3 CRITICAL + 13 HIGH |
| 3 | Add npm `overrides` for form-data, basic-ftp, axios, react-router | ✅ Done (axios/react-router-dom/path-to-regexp bumped directly; form-data, basic-ftp, minimatch, react-router via `overrides`) |
| 4 | Bump spring-boot/spring-core/bcprov/netty in pom | ✅ Done — parent 3.4.3→3.4.13, bouncycastle 1.80→1.84, netty-bom 4.1.134 pinned |
| 5 | Address api CRITICAL smells (constants, tab char) | ✅ Done — all 4 closed in Sonar |
| 6 | Raise test coverage (api 5.7%, ui 0%) | ⬜ Deferred (follow-up) |

**Accepted/remaining (no fix within constraints):** 5 Trivy HIGH (spring-boot/actuator 3.4.x, lodash) — see *Remediation results*. Current checked-in Sonar issue snapshots contain only active high-priority findings: 3 HIGH-impact UI code smells and no active api critical/high issues.
