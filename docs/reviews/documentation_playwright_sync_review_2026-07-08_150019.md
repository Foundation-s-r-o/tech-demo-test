# Documentation, ADR, and Playwright Coverage Review

Date: 2026-07-08 15:00
Repository: `tech-demo-test`
Scope: `CLAUDE.md`, `SECURITY.md`, `PROJECT_OVERVIEW.md`, `docs/ARCHITECTURE.md`,
`docs/ADR/*.md`, current Playwright tests, and supporting source/config evidence.

## Executive summary

The ADR set is mostly coherent and short enough for agent use. The main problems are:

1. `PROJECT_OVERVIEW.md` is materially stale and conflicts with the civil-registry
   architecture now described by `docs/ARCHITECTURE.md`, `SECURITY.md`, and ADR-0011.
2. `docs/ARCHITECTURE.md` is broadly aligned with ADR-0011 and `SECURITY.md`, but its
   quality-gate section still misclassifies JaCoCo as advisory and undersells the
   current Playwright suite.
3. ADR-0010 claims Playwright coverage for "persons list/create/edit"; current tests
   cover list/create/detail/validation, not edit.
4. ADR-0011 claims a broad registry API surface. Current Playwright coverage is good
   for login, persons create/detail, birth registration, role menu, agency authz,
   GDPR DSR, audit chain, citizen portal, OpenAPI drift, and a11y smoke. It is not
   complete for marriages, deaths, amendments, death medical data, codelists, GDPR
   consent/disclosure/export, PDF, access-log reads, and several negative
   access-purpose/authorization cases.
5. Generated reports and large legal/scanner artifacts clutter the normal agent
   context path and should be moved or clearly classified as archive/generated output.

No existing files were changed by this review.

## Verification performed

Commands run:

```bash
find ui/tests -maxdepth 3 -type f -print | sort
rg -n "test\\(|describe\\(|expect\\(|persons|birth|marriage|death|gdpr|audit|agency|me|OpenAPI|authorization" ui/tests ui/playwright*.config.ts
find docs/ADR -maxdepth 1 -type f -name '*.md' -print | sort
rg -n "Playwright|E2E|persons|birth|marriage|death|gdpr|audit|codelist|agency|me|authorization" docs/ADR/*.md docs/ARCHITECTURE.md SECURITY.md PROJECT_OVERVIEW.md README.md
npx playwright test --list
```

Playwright runner inventory:

- 25 tests
- 9 spec files
- Browser project: `chromium`
- Files: `test_a11y.spec.ts`, `test_audit.spec.ts`, `test_authentication.spec.ts`,
  `test_authz.spec.ts`, `test_citizen.spec.ts`, `test_gdpr.spec.ts`,
  `test_openapi.spec.ts`, `test_persons.spec.ts`, `test_records.spec.ts`

The full Playwright suite was not executed; this review verifies claimed coverage
by test inventory and source inspection, not runtime pass/fail behavior.

## Playwright coverage against ADR claims

### ADR-0010 test-harness claims

ADR-0010 says full E2E covers:

- login, failed login, logout
- main page
- persons list/create/edit
- client-side validation
- authorization matrix
- accessibility gate
- live OpenAPI contract check

Coverage status:

| Claim | Current Playwright coverage | Status |
| --- | --- | --- |
| Login / failed login / logout | `test_authentication.spec.ts` | Covered |
| Main page | `test_persons.spec.ts` | Covered |
| Persons list | `test_persons.spec.ts` | Covered |
| Persons create | `test_persons.spec.ts`, helper `createPersonViaUi` | Covered |
| Persons edit | No edit route or test found; current route is `/persons/:uid` detail | Not covered / claim stale |
| Client-side validation | `test_persons.spec.ts` missing required fields | Covered narrowly |
| Authorization matrix | `test_authz.spec.ts` covers role menu, agency endpoint, agency create rejection | Partial |
| Accessibility | `test_a11y.spec.ts` checks login, home, persons list, persons add in light mode | Partial |
| Live OpenAPI spec | `test_openapi.spec.ts` | Covered |

Recommendation:

- Update ADR-0010 wording from "persons list/create/edit" to
  "persons list/create/detail/client-side validation" unless an edit flow is added.
- Expand the E2E scope statement to include current tests for records, GDPR, audit,
  citizen portal, and agency authz.

### ADR-0011 civil-registry surface

ADR-0011 claims this app implements:

- persons + bitemporal details/addresses/religion
- vital records: register/amend/dissolution/medical
- codelists
- GDPR: DSR/consent/disclosure/Art. 15 export
- audit: events/access-log/chain verification
- citizen `/me`
- per-operation `@PreAuthorize`
- service-layer office scoping
- personal reads require `X-Access-Purpose`

Coverage status:

| Feature claim | Current Playwright coverage | Status |
| --- | --- | --- |
| Persons list/create/detail | `test_persons.spec.ts` | Covered |
| Person bitemporal details/address/religion writes | Only basic create fields are covered | Partial |
| Person PDF endpoint | No Playwright/UI test found | Missing |
| Birth registration | `test_records.spec.ts` own-office success | Covered narrowly |
| Birth office scoping | `test_records.spec.ts` other-office rejection | Covered |
| Marriage registration | `MarriagePage.tsx` exists, no Playwright test | Missing |
| Marriage dissolution | UI exists (`marriage_dissolve`), no Playwright test | Missing |
| Death registration | `DeathPage.tsx` exists, no Playwright test | Missing |
| Death medical Art. 9 data | UI exists (`death_medical_*`), no Playwright test | Missing |
| Vital-record amendments | API claimed in docs, no visible UI route/test found | Missing at UI; likely API/integration-test candidate |
| Codelists | API client exists and forms likely use data, no direct Playwright assertion | Partial / implicit only |
| GDPR DSR | `test_gdpr.spec.ts` creates and lists request | Covered narrowly |
| GDPR consent create/withdraw | UI exists (`gdpr_consent_*`), no Playwright test | Missing |
| GDPR disclosures | UI list exists (`gdpr_disclosures`), no create flow seen in UI | Missing / maybe API-only |
| GDPR Art. 15 export | UI button exists (`gdpr_export`), no Playwright test | Missing |
| Audit chain verification | `test_audit.spec.ts` | Covered |
| Audit events table | `test_audit.spec.ts` only checks table visible | Partial |
| Audit access-log read | UI exists (`audit_access_*`), no Playwright test | Missing |
| Citizen `/me` and own access log | `test_citizen.spec.ts` | Covered narrowly |
| Agency minimal view | `test_authz.spec.ts` hits API as agency reader and clerk | Partial |
| Six-role menu UX | `test_authz.spec.ts` covers clerk, DPO, auditor, citizen | Partial; admin/supervisor and agency menu not covered |
| Personal reads require `X-Access-Purpose` | Agency test sends header; no missing-header negative found | Partial |

## Suggested Playwright additions

Keep these as focused vertical slices, not an exhaustive UI automation program.
The goal is to cover claims made by ADR-0011 and `SECURITY.md` without making
the suite brittle.

| Priority | Suggested test | Why | Feasibility |
| --- | --- | --- | --- |
| P1 | Marriage registration through `/records/marriages` | ADR-0011 claims vital records beyond births; page exists | Feasible now |
| P1 | Death registration through `/records/deaths` | Same as above | Feasible now |
| P1 | Death medical save through `death_medical_*` controls | Covers Art. 9 medical claim | Feasible if death create returns id visibly |
| P1 | GDPR consent create + withdraw | UI controls exist; SECURITY matrix lists consents | Feasible now |
| P1 | GDPR export button smoke | Covers Art. 15 export path | Feasible; assert no error and lists refresh/download behavior as implemented |
| P1 | Audit access-log lookup after reading a person | SECURITY claims personal reads are logged | Feasible now |
| P1 | Missing `X-Access-Purpose` negative via `page.request` | SECURITY claims missing purpose gives 400 | Feasible now |
| P2 | Person PDF download response headers/content type | README/Project Overview mention PDF; ADR-0011 keeps retained PDF endpoint | Feasible via `page.request` |
| P2 | Codelist-backed form control smoke | ADR-0011 claims codelists and UI depends on them | Feasible if selectors are stable |
| P2 | Agency minimal view positive with real seeded/created person | Current agency test only proves auth reaches handler by expecting 404 | Feasible if a suitable UID can be created/read |
| P2 | Supervisor/admin menu and write capability | Six-role matrix includes SUPERVISOR; current menu tests omit admin/supervisor | Feasible |
| P3 | Vital-record amendment path | ADR claims amendments, but no obvious UI route found | Probably API/integration-test first unless UI is added |
| P3 | Marriage dissolution | UI control exists, but test needs reliable marriage id extraction | Feasible after marriage create exposes id |

Do not add broad end-to-end tests for every codelist item or every field. That
would slow the suite and fight ADR-0010's "each layer must earn its place" rule.

## Documentation sync findings

### Architecture vs ADR

Mostly in sync:

- `docs/ARCHITECTURE.md` describes civil registry as the business surface and links
  to ADR-0011.
- Its design principles match ADR-0002/0003/0004/0007/0008/0010.
- Its deferred production path now matches the Spring Security 7 MFA direction in
  ADR-0004 and `SECURITY.md`.

Issues:

1. `docs/ARCHITECTURE.md` quality gate says JaCoCo is advisory/report-only, while
   `SECURITY.md` and ADR-0008 say `jacoco:check` is a hard Maven `verify` gate.
   Fix: classify JaCoCo as blocking.
2. `docs/ARCHITECTURE.md` says full E2E covers login/logout, main page, and persons
   feature. That is stale/understated; current Playwright also covers records,
   GDPR, audit, citizen portal, agency authorization, OpenAPI, and a11y.
3. `docs/ARCHITECTURE.md` ADR table stops at ADR-0010. ADR-0011 and ADR-0012 exist
   and should be included because they drive the civil-registry schema and error
   contract.
4. `docs/ARCHITECTURE.md` residual-gaps sentence still says "login throttling,
   coverage gate"; `SECURITY.md` marks coverage gate closed. Keep only open gaps
   or say coverage ratcheting remains future hardening.

### ADR vs SECURITY.md

Mostly in sync:

- ADR-0004 and `SECURITY.md` agree on session login, CSRF, CORS in security chain,
  six roles, local/it seeded accounts, Spring Security 7 MFA direction, and open
  login throttling.
- ADR-0011 and `SECURITY.md` agree on application-layer replacement for PostgreSQL
  controls and the PostgreSQL re-hardening path.
- ADR-0010 and `SECURITY.md` agree that JaCoCo is a hard gate.

Issues:

1. ADR-0004 title still says "Keycloak/OIDC deferred"; body now says MFA is in-app
   via Spring Security 7 and external IdP is optional for SSO/federation/lifecycle.
   Fix title to avoid carrying the older Keycloak-first implication.
2. `SECURITY.md` implemented controls say role-based authorization is enforced in
   `SecurityConfig`. Later text correctly says method-level `@PreAuthorize` is the
   enforcement point and `SecurityConfig` handles only infra/default auth. Fix the
   implemented-controls bullet.
3. `SECURITY.md` residual gaps table says "ADMIN/USER matrix enforced in
   SecurityConfig with negative tests in PersonControllerIT". That is stale; the
   current model is six registry roles with controller-level method security and
   service-layer scoping.

### Architecture/ADR/SECURITY vs PROJECT_OVERVIEW.md

`PROJECT_OVERVIEW.md` is not in sync and should not be treated as canonical.

Examples:

- Says current business scope is small Person CRUD + PDF, while Architecture and
  ADR-0011 describe civil registry with vital records, GDPR, audit, agency, and `/me`.
- Says `SecurityConfig` protects every route except H2 console; current
  `SECURITY.md` says H2 console is disabled in every profile.
- Says migration `V3` seeds `admin/admin`; current docs/source say migration `V4`
  removes the historical seed and `LocalAdminInitializer` creates six accounts only
  under `local` / `it`.
- Says `WebConfiguration` configures CORS; current ADR-0004 and source put CORS in
  the Spring Security filter chain.
- Says there are 3 Playwright specs; runner lists 25 tests in 9 files.
- Planned direction says replace local session-login with Keycloak/OIDC; current
  `SECURITY.md` and ADR-0004 say Spring Security 7 MFA in-app, external IdP optional.

Recommendation: replace `PROJECT_OVERVIEW.md` with a short pointer:

- "This file is retired; read `README.md`, `docs/ARCHITECTURE.md`, `SECURITY.md`,
  and `docs/ADR/README.md`."

Or delete it after confirming no external link depends on it.

## Context-clutter findings from the earlier documentation review

These findings remain valid:

1. `PROJECT_OVERVIEW.md` is materially stale and should be replaced or retired.
2. Generated scanner artifacts under `docs/` are large and tracked:
   `docs/sonar-issues-*.json`, `docs/trivy-report.json`, metrics JSON, report TXT.
   Prefer ignored per-run output plus short human summaries.
3. Root-level `fossa_attribution.md` is a 44k-line generated legal artifact. If it
   must stay tracked, move it under `legal/` and document that agents should not
   load it during normal context gathering.
4. Dated historical reports should move under `docs/archive/` so they are not
   confused with current policy.
5. `CLAUDE.md` is useful but long for an always-loaded agent file. Move detailed
   mechanics to `docs/VERIFYING.md` and keep `CLAUDE.md` as a short verification
   decision tree plus high-risk paths.

## Recommended cleanup order

1. Retire or rewrite `PROJECT_OVERVIEW.md`.
2. Patch `docs/ARCHITECTURE.md`:
   - JaCoCo is blocking.
   - E2E coverage summary includes records/GDPR/audit/citizen/agency/OpenAPI/a11y.
   - ADR table includes ADR-0011 and ADR-0012.
   - residual gaps match `SECURITY.md`.
3. Patch `SECURITY.md` stale wording:
   - implemented-control bullet: method-level `@PreAuthorize`, not `SecurityConfig`,
     enforces business authorization.
   - residual role-auth row: six-role matrix, not ADMIN/USER.
4. Patch ADR-0004 title to match the Spring Security 7 MFA decision.
5. Patch ADR-0010 to remove the stale persons-edit claim or add an edit flow/test.
6. Add P1 Playwright tests listed above.
7. Move generated reports/archive files out of the active docs path.

## Bottom line

The active architecture/security/ADR story is converging, but `PROJECT_OVERVIEW.md`
and a few stale gate/security sentences still create conflicting guidance for
agents. Playwright coverage is meaningful and has expanded beyond the older docs,
but it does not yet cover all user-visible features claimed by ADR-0011. The next
test work should be a small set of vertical slices for marriage, death, GDPR
consent/export, audit access-log, missing access-purpose, and PDF.
