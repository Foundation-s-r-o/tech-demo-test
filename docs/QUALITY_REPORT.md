# Quality & Security Scan Report — tech-demo-test

Run: 2026-05-27 · SonarQube host: `http://10.16.35.93:9000` (matrika local instance)

Tools: ESLint + tsc (ui), Checkstyle + JaCoCo (api), Trivy (fs vuln), SonarQube (api + ui).
Raw artifacts in this folder: `eslint-report.txt`, `trivy-report.{txt,json}`, `sonar-metrics-*.json`, `sonar-issues-*.json`.

## Summary

| Tool | api | ui |
|---|---|---|
| Build / compile | ✅ pass (1 unit test, 0 checkstyle violations) | ✅ tsc pass |
| ESLint | n/a | ❌ **11 errors** |
| Sonar bugs | 0 | 3 |
| Sonar vulnerabilities | 0 | 0 |
| Sonar security hotspots | 1 (hardcoded password) | 0 |
| Sonar code smells | 8 | 72 |
| Sonar coverage | 5.7% | 0% |
| Sonar ratings (rel/sec/maint) | 1.0 / 1.0 / 1.0 | 3.0 / 1.0 / 1.0 |
| Trivy | — combined: **5 CRITICAL, 49 HIGH, 52 MEDIUM, 14 LOW** — |

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

## Sonar — notable issues

**api security hotspot:** `auth/UserResponseDTO.java:15` — hardcoded `password` (the mocked `Admin/admin` auth). Expected for the demo, but review before any real use.

**api CRITICAL code smells:** tab char in `PersonRepository.java:15`; duplicated log literals "Trace Id: {}" / "Span Id: {}" in `PersonServiceImpl` (extract constants); static-access on `IdentifiableEntity_.ID` in `PersonApiRepositoryImpl.java:75`.

**ui bugs (3):** missing keyboard listeners on clickable `MenuCollapse.tsx:12` / `SubMenuRow.tsx:11` (a11y); redundant `try` in `TableWrapper.tsx:122`.

**ui code smells:** 72 (1 critical, 36 major) — triage on dashboard.

## Recommendations (priority order)

1. Fix the 11 ESLint errors (blocks `npm run lint` gate).
2. Bump tomcat to 10.1.55 — clears 3 CRITICAL + 13 HIGH at once.
3. Add npm `overrides` for form-data, basic-ftp, axios, react-router.
4. Bump spring-boot/spring-core/bcprov/netty in pom.
5. Address api CRITICAL smells (constants, tab char).
6. Raise test coverage (api 5.7%, ui 0%).
