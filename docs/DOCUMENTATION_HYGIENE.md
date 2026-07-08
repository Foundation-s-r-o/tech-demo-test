# Documentation Hygiene

## Goal

Keep repository documentation accurate, small, and useful as working context for
humans and agents. Documentation should describe the current intended state of
the project, not preserve a record of how the project got there. Git is the
history; files should not keep their own change history unless the file is
explicitly an audit log, review record, ADR, or active plan.

Good documentation in this repository should:

- provide one clear source of truth for each durable fact
- link to canonical sources instead of repeating large sections
- distinguish active guidance from historical evidence
- avoid generated output in normal reading paths
- remove stale duplicates instead of partially maintaining them
- keep agent-facing context compact enough to be useful

## Canonical Sources

Use these as the authority hierarchy:

| Subject | Canonical source |
| --- | --- |
| Setup and common commands | `README.md` |
| Agent rules and verification decision tree | `CLAUDE.md` |
| Stable architecture, layout, data flows, ADR map | `docs/ARCHITECTURE.md` |
| Security controls, demo caveats, local accounts, hardening path | `SECURITY.md` |
| Accepted decisions and consequences | `docs/ADR/*.md` and `docs/ADR/README.md` |
| Registry domain/schema design | `db/DESIGN.md` |
| Active work status | current `db/PLAN-*.md` and `db/REVIEW-LOG.md` |
| Dated audits/reviews | `docs/reviews/*.md` |

Generated scan artifacts, coverage output, smoke logs, and legal attribution
exports are evidence, not canonical prose. Keep them out of the normal
documentation path unless a current workflow explicitly requires them in git.

## Maintenance Standard

When touching documentation, make the repository easier to understand from its
current state. Do not add timeline summaries, cleanup recaps, or "previously we
did X" sections to living documents. If historical context matters, prefer an
ADR, a dated review under `docs/reviews/`, or the existing plan/review log.

Each documentation change should preserve or improve these properties:

- **Current:** the file describes the project as it is meant to work now.
- **Canonical:** durable facts live in one primary place.
- **Linked:** supporting files point to the canonical source instead of copying
  its content.
- **Scoped:** agent instructions, architecture notes, security notes, ADRs,
  plans, and reviews keep separate responsibilities.
- **Compact:** generated artifacts and verbose reports do not inflate routine
  context.
- **Actionable:** active plans show what remains to do; completed work is marked
  complete or removed from active guidance.

## What To Do

Prioritize these actions during documentation maintenance:

- Align active docs with the current code, schema, scripts, and project layout.
- Remove stale duplicate files or replace them with a short pointer to the
  canonical source.
- Fold durable security, architecture, setup, or verification guidance into the
  appropriate canonical document.
- Mark historical plans or reviews clearly so they are not mistaken for active
  work.
- Keep ADRs focused on accepted decisions and consequences.
- Keep `CLAUDE.md` focused on agent rules, repository-specific commands, and
  verification policy; avoid copying architecture or security prose into it.
- Remove generated reports from tracked documentation unless they are required
  evidence for an active workflow.
- Prefer deletion over maintaining outdated summaries with disclaimers.

## What To Avoid

Do not use living documentation for:

- cleanup history
- commit summaries
- dated progress narratives
- duplicated architecture/security/setup sections
- generated scanner output
- stale reports kept only because they once existed
- detailed instructions for how an agent must perform the hygiene pass

The documentation should state the goal and the desired repository standard. The
agent performing the work decides the exact commands, searches, and verification
needed for the change at hand.

## Classification

Use these categories when deciding whether a document should stay, move, or be
removed:

| Category | Standard |
| --- | --- |
| Canonical | Current source of truth; keep accurate and link to it. |
| Runbook | Current operational guidance; concise and executable. |
| ADR | Accepted decision and consequences; not a migration journal. |
| Plan/review log | Active or explicitly historical work tracking. |
| Dated report | Evidence from a point in time; keep only when still useful. |
| Generated artifact | Prefer ignored output or external archive. |
| Stale duplicate | Delete or replace with a pointer. |

## Completion Criteria

A documentation hygiene pass is complete when:

- current guidance has one clear canonical home
- stale duplicates, outdated reports, and unnecessary generated artifacts are
  removed or moved out of the normal context path
- historical files are clearly historical
- active plans contain only active or intentionally deferred work
- final notes describe what changed and any remaining follow-up, without adding
  history sections to living documentation
