# Plan — fix the REVIEW-LOG.md punch list (registry-port completion)

Status: ✅ done (2026-07-08) · Created: 2026-07-08 · Owner: Claude Code

All work packages (WP1–WP7) from the original punch list were implemented,
verified, and committed (`35587bd`..`8d47f5d`). Closure was recorded in
[REVIEW-LOG.md](REVIEW-LOG.md) ("Closing entry — punch list resolved").

## Context

The registry port ([PLAN-registry-port.md](PLAN-registry-port.md), Stages 1–6) is functionally
complete and green on lint/verify/E2E, but the final review in [REVIEW-LOG.md](REVIEW-LOG.md)
closes with a 12-item punch list: 3 defects, 5 contract/design gaps, 3 doc items claimed done
but not, and the un-run release checklist. At plan creation, all items had been re-verified in the
working tree on 2026-07-08 and **every one was still open** — including ADR-0011 asserting an
`openapi.yaml` deviations note that did not exist, and presenting `BitemporalGuard` as wired while
it was dead code.

Decisions taken by Jozef (2026-07-08):

- **Bitemporal**: rework person writes to the design's full bitemporal semantics (not
  adopt-simplified).
- **Agency**: build the minimal AGENCY_READER endpoint over `V_PERSON_CURRENT` semantics
  (API only, no UI page).
- **Art. 15 export**: extend to full scope (religion, history, vital records, access log).
- **Commits**: after green gates, commit in reviewable chunks (no push, no Co-Authored-By
  trailer).

Goal: close the punch list so the port can honestly be called done — code fixed, docs truthful,
release checklist run, work committed.

## Follow-up documentation sync (2026-07-08)

Progress legend: ✅ done · 🔲 not started

| Item | Status |
| --- | --- |
| Retire stale `PROJECT_OVERVIEW.md` into a pointer to canonical docs | ✅ done |
| Patch `docs/ARCHITECTURE.md` gate classification, Playwright scope, ADR map, and residual-gap wording | ✅ done |
| Patch `SECURITY.md` business-authorization wording | ✅ done |
| Patch ADR-0004 title and ADR index to match the Spring Security 7 MFA direction | ✅ done |
| Patch ADR-0010 to remove the stale persons-edit E2E claim and list current E2E scope | ✅ done |
| Add the missing Playwright vertical slices identified in `docs/reviews/documentation_playwright_sync_review_2026-07-08_150019.md` | 🔲 not started |
| Move/archive generated scanner and legal artifacts out of the normal agent context path | 🔲 not started |

---

## Work packages (in execution order)

### WP1 — Small backend defects

1. **Vital-records amend path** — `api/src/main/java/sk/foundation/techdemo/records/api/VitalRecordsApiService.java`
   - In `amendBirth` (L72), `amendMarriage` (L110), `amendDeath` (L167): reject with
     `ConflictException` (→ 409) when `previous.getSupersededAt() != null` (prevents history
     falsification + forked chains).
   - Call `assertRegistryNumberFree(...)` in all three amend paths, excluding the amended
     record's own chain (walk `supersedesId` ancestry or exclude by matching registry number of
     `previous`).
   - ITs in `RegistryRecordsApiIT`: amend-of-superseded → 409; amend onto another record's
     registry number → 409; legitimate amend keeping same number → 200.
2. **`AuditService.verifyChain()`** — `api/src/main/java/sk/foundation/techdemo/auditlog/AuditService.java` (L72–88)
   - After the loop, load `CHAIN_HEAD` (repository already injected) and fail verification if
     `expectedPrev` ≠ `head.getLastHash()` (detects tail truncation/rewrite).
   - IT: delete/modify the newest audit row directly via repository → verification reports
     invalid.
3. **`PersonMergeService.merge()` race** — `api/src/main/java/sk/foundation/techdemo/registry/PersonMergeService.java` (L22–45)
   - Acquire `personRepository.lockByPersonUid(duplicateUid)` (and primary) before the
     uniqueness checks, same pattern as `RegistryPersonApiService.lockPerson` (L310).
4. **DB CHECK violations → 400** — `api/src/main/java/sk/foundation/techdemo/infrastructure/api/ApiExceptionHandler.java`
   - Add `@ExceptionHandler(DataIntegrityViolationException.class)` → 400 `ProblemDetail`
     (generic message, no SQL internals). IT: death record with no date/found/court basis → 400.

### WP2 — Bitemporal rework (person satellites)

Rework `RegistryPersonApiService` writes from blanket-supersede to the design's
supersede-and-close flow, wiring `BitemporalGuard`. **No controller/DTO change → this WP needs
no OpenAPI resync** (DTOs already carry `@NotNull validFrom`; composite/current reads keep
their contract; response DTOs already expose `validFrom/validTo/supersededAt`).

**Wire the guard** — inject `BitemporalGuard` into `RegistryPersonApiService`
(`@RequiredArgsConstructor`); call `assertDetailInsertable` / `assertAddressInsertable` /
`assertReligionInsertable` in each write **after** the supersede-and-close step, **before**
saving the new row (guard must see post-truncation state; `saveAndFlush` the closed copy so the
JPQL guard query sees it). Guard throws `ConflictException` → existing 409 mapping. All three
write methods already hold `lockByPersonUid` (guard's documented precondition); `createPerson`
needs no lock (no satellite rows yet). Update the stale class javadoc.

**Supersede-and-close algorithm** (three private per-type helpers — detail keyed by person;
address by person+addressType; religion by person; entities share no interface, don't invent
one):

```
current = repo.findByPersonUidAndSupersededAtIsNull(uid)      // addresses: + addressType
open = current where validTo == null                          // invariant: ≤ 1

if (newValidTo == null && open exists):                       // only open-ended inserts take over
    if (open.validFrom < newValidFrom):                       // DESIGN §4 canonical flow
        copy = closed copy of open (all business fields, validTo = newValidFrom,
               recordedAt = now, recordedBy = actor); repo.saveAndFlush(copy)
        open.supersededAt = now; open.supersededBy = copy.id  // copy carries the old belief
    else if (open.validFrom == newValidFrom):                 // correction: no empty [D,D) copy
        open.supersededAt = now; open.supersededBy = newRow.id (after save)
    // else open.validFrom > newValidFrom: leave — guard 409s

guard.assert*Insertable(...)                                  // rejects every remaining overlap
save new row; audit INSERT (one entry per API call, unchanged)
```

Per-method: `insertDetail` (L278, L286–290) with `closedDetailCopy` helper (all 13 business
fields); `addAddress` (L138, L150–151) — truncation candidate is the open row **of the same
addressType**, closed copy reuses the old link's `addressId`; `addReligion` (L175, L187) —
closed copy keeps `religionCode`/`legalBasis`/`consentId`, consent-validation block unchanged.
Delete dead `supersede(...)`/`supersedeReligion(...)` helpers (Checkstyle flags unused). Add an
up-front range sanity check: `validTo != null && !validTo.isAfter(validFrom)` → 409.
`supersededBy` FKs the same table, so pointing the original at its closed copy is valid.

**Read-path fallout:**

- **`search` (required fix)**: closed history copies are current-belief, so name search would
  return stale-name hits. Replace the repository finder with `searchCurrent(...)` `@Query`
  adding the valid-time filter (`validFrom <= :today and (validTo is null or validTo > :today)`);
  service passes `LocalDate.now()`.
- `getAddresses`/`getReligion`: keep returning all current-belief rows (the valid-time
  timeline); add `validFrom` sort in the service streams for deterministic output.
- `getComposite`/`getMe`/`currentDetail`: unchanged — guard now guarantees ≤1 current row
  containing today. GDPR consent withdrawal (supersede without replacement) stays valid
  transaction-time retraction. `V_PERSON_CURRENT` already correct; **no schema changes**.

**T4 decision**: no `?asOf=` API parameter (no consumer needs it; avoids OpenAPI churn) — T4 is
proven at repository level inside a MockMvc-driven IT. Adding `?asOf=` later is additive.

**ITs** (in `RegistryPersonApiIT`; `RegistryDomainIT` guard-level T3 stays as-is):

- Modify `addingDetailSupersedesPrevious`: history 2 → **3** rows (superseded original, closed
  copy with `supersededAt == null`, new open row) + `validTo`/`supersededAt` jsonPath asserts.
- Modify `permanentAddressSupersedesPrevious`: `GET /addresses` 1 → **2** rows, sorted asserts;
  composite still shows only the new address.
- Add **T3 detail**: closed-range insert overlapping the open row → 409, history unchanged.
- Add **T3 address**: overlapping same-type address → 409; overlapping *different* type → 201
  (proves per-type key).
- Add **historical insert preserves current**: past closed range, no overlap → 201; 2 current-
  belief rows; composite unchanged.
- Add **T4 point-in-time**: repository-level half-open containment pick for two `asOf` dates
  against the supersede-produced timeline.
- Add **religion supersede**: second open religion → 201, closed copy + new row (JaCoCo floor
  coverage for the helper).

Edge cases encoded: equal-`validFrom` correction; open insert starting before current open row
→ 409; future-dated open insert → allowed (truncation at future date, composite keeps old value
until then); `validTo <= validFrom` → 409.

Main risks: Hibernate flush ordering around the guard query (mitigated by `saveAndFlush`) and
forgetting the per-type filter on the address truncation candidate.

### WP3 — Contract/design gaps

5. **Agency endpoint (T9)** — new `registry/api/AgencyController` + service method:
   - `GET /api/agency/persons/{uid}` → minimal current projection (the `V_PERSON_CURRENT`
     column set: uid, name, dob, sex, existence status, permanent address), `@PreAuthorize`
     AGENCY_READER only, requires `X-Access-Purpose`, access-logged.
   - Read via the existing satellite repositories (same current-in-valid-time pick as the
     composite read) — no JPA mapping of the H2 view needed; keep the view as the SQL-level
     artifact it is.
   - IT (T9 port): agency reads minimal view; agency gets 403 on clerk endpoints (existing);
     clerk/citizen get 403 on agency endpoint. Update the E2E "agency denial-only" test to also
     exercise the allowed read (API-level; keep UI out of scope).
6. **Access-logging completeness** — `RegistryPersonApiService.search` (L235) +
   `PersonController.search` (L56): log one `ACCESS_LOG` row per returned person with the
   passed purpose. `VitalRecordsApiService`: `getMarriage` logs **both** spouse UIDs;
   `addDissolution` logs access for both spouses; `setMedical` audits `UPDATE` when the row
   already exists. ITs: search produces access-log rows; marriage read logs 2 rows.
7. **Security-event writer** — new `auth/SecurityEventListener` (`@Component`):
   - `@EventListener` on `AbstractAuthenticationFailureEvent` (login failure: username, IP if
     available, event type); persist via existing `SecurityEventRepository`. Keep minimal —
     failures first; success/privilege events only if trivially hookable.
   - No PII beyond username in the event payload (logs-no-PII rule). IT: failed login →
     `AUDIT.SECURITY_EVENT` row exists.
8. **Art. 15 export full scope** — `gdpr/api/GdprApiService.export` (L142–150):
   - Extend `ExportResponseDTO`: religion rows (current + history), detail history, address
     history, vital records where the person is subject (birth as child, marriages as spouse,
     death), own access-log entries. All repositories exist — assembly work.
   - Export continues to self-record as a disclosure and access-log the read.
   - IT: export for the seeded citizen contains all sections.
9. **Codelist whitelist expansion** — `codelist/CodelistService.java` (L22–29): add
   `registry_office`, `municipality`, `region`, `district`, `municipality_part`,
   `title_before`/`title_after` (use actual V5/V6 table names). IT: new names resolvable,
   non-whitelisted name still rejected. (UI typeahead + code→label rendering stays deferred —
   recorded in the deviations note, see WP6.)

### WP4 — UI fixes (small)

10. **DetailPage error handling** — `ui/src/pages/persons/DetailPage.tsx` (L18–21): add
    `.catch(() => showModal(<ErrorModal text="common.error.generic" />))` via `useModals()` —
    the exact convention used in `ui/src/pages/records/MarriagePage.tsx:45`.
11. **i18n deceased flag** — add `common.yes` = `"Áno"` to
    `ui/src/assets/translations/sk.json`; use it in `DetailPage.tsx:46` instead of
    `common.btn.confirm`.
12. **Citizen access-log race (cosmetic)** — in the `/me` page, sequence the access-log fetch
    after the composite fetch resolves, so the citizen sees the log entry their own read
    created.

### WP5 — OpenAPI resync

Required by WP3's contract changes (agency endpoint + export DTO); WP2 changes no contract.

- Start API (`local` profile), save `http://localhost:8082/v3/api-docs` over
  `ui/api-docs.json`, `npm run generate-openapi-services`, commit both together with the
  backend change. (E2E `test_openapi.spec.ts` + doctor drift gate enforce this.)

### WP6 — Docs made truthful

13. **`db/openapi.yaml`** — add the missing **Deviations** section (ADR-0011 already claims it
    exists): session auth vs OAuth2 scopes, `/api` base path, retained PDF endpoint, plus
    accepted cuts — dissolution amendments absent, DSR free transitions / no Art. 12(3)
    extension, `/api/audit/events` unpaged, delete-by-convention (T6 nuance), T14
    (representative portal access) not ported, UI free-text office/municipality inputs.
14. **ADR-0011 amendments** — correct the T-coverage sentence (after this work: T3/T4/T9
    ported; T14 remains an accepted deviation); the BitemporalGuard description becomes true
    after WP2 — verify wording matches the final write flow; the openapi.yaml-deviations claim
    becomes true via item 13.
15. **New short ADR-0012 — RFC 7807 as the API error contract** (review recommendation; also
    referenced from openapi.yaml). Record the app-layer race notes + minimal audit payloads +
    delete-by-convention honesty items here or in ADR-0011's consequences — one place,
    cross-linked.
16. **`db/DESIGN.md` §12** — verify the BitemporalGuard/supersede-then-insert row is accurate
    after WP2 (it should become true); adjust wording if any nuance differs.
17. **`docs/ARCHITECTURE.md` — rewrite** (last stale core doc; every section still describes
    the deleted demo Person app): business surface (civil-registry aggregate, six roles),
    repository layout (`registry/`, `records/`, `gdpr/`, `auditlog/`, `codelist/`,
    `infrastructure/crypto/`; two entity bases `IdentifiableEntity` + `NaturalKeyEntity`),
    HTTP surface (persons, vital records, gdpr, audit, codelists, `/me`, new agency endpoint),
    data flows (bitemporal write with guard, access-logged read, audit hash chain), auth
    (`@PreAuthorize` + office scoping). Keep the doc's volatile-facts rule: reference
    `db/migration/` and controllers rather than duplicating lists.
18. **`SECURITY.md`** — add the agency endpoint to the role matrix; mention the security-event
    listener (NIS2 story); dev-keys caveat unchanged.
19. **`db/REVIEW-LOG.md`** — append a closing entry recording the punch-list resolution.
20. **Stragglers**: `ruff format db/import_codelists.py`; V6 header count fix
    ("6187" → 6,167 inserts; same figure in DESIGN.md if present).

### WP7 — Release checklist + commits

- Gates: `cd api && ./mvnw clean verify`; `cd ui && npm run lint && npm run smoke`;
  full `npx playwright test` (incl. new/updated agency + persons specs).
- `./doctor` end-to-end (ports 8080/8082 free).
- `./scripts/security_scan.sh` (Semgrep metrics off, Trivy with `DOCKER_CONFIG` workaround,
  npm audit).
- Sonar both projects (`-Dsonar.token`), refresh `docs/` artifacts: Sonar JSONs, Trivy
  reports, JaCoCo.
- Playwright screenshot review: share the latest run's screenshots (gallery + key shots) —
  UI changed (DetailPage, i18n, /me sequencing).
- **Commits** (explicitly authorized for this work): stage + commit in reviewable chunks after
  all gates are green, e.g.:
  1. Flyway/schema + backend registry port (the bulk already in tree, grouped logically:
     migrations; domain/infra; records/gdpr/audit APIs; demo-person removal)
  2. Punch-list backend fixes (WP1–WP3)
  3. UI (Stage 5 UI + WP4 fixes + regenerated client + api-docs.json)
  4. E2E suite + docs (ADRs, ARCHITECTURE, SECURITY, openapi.yaml, DESIGN, REVIEW-LOG)
  No `Co-Authored-By` trailer. No push.

## Verification

- Each WP lands with its ITs; `./mvnw clean verify` green after every backend WP (JaCoCo floor
  must not dip — new code ships with tests).
- After WP5: E2E OpenAPI drift test green.
- Final: full E2E suite + `./doctor` + security scans green; Sonar shows no new
  bugs/vulnerabilities; screenshots reviewed.

## Explicitly out of scope (recorded as deviations, not fixed)

- UI typeahead for office/municipality + rendering code labels instead of raw codes (cosmetic,
  larger UI effort — deviations note).
- T14 representative portal access (`/me` stays own-data-only).
- Audit events pagination (demo scale).
