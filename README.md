# tech-demo-test — Foundation reference template

A full-stack demo application that doubles as Foundation s.r.o.'s reference
template for new projects. It is intentionally **small, dependency-light, and
Docker-free for local development**, while wired up to a real CI / quality /
security toolchain so the bar is set the same way every new project will inherit.

This is a **rolling demo** (one supported branch: `main`), not a released
product — see [SECURITY.md](./SECURITY.md) for the supported-versions and
demo-mode caveats policy.

> Tech-stack reference document (Google Drive): [Standard Tech Stack](https://docs.google.com/document/d/16zLZa3JL4DSsK9vqUzfF046CPau5ziO5cJfsdrBqhe8/edit?usp=drive_link).
> Comment there if you have feedback on direction. The repo's job is to make
> that document executable; the document's job is to record the why.

## Stack snapshot

| Layer | Version | Notes |
| ----- | ------- | ----- |
| Java | **21 LTS** | Pinned in `api/.sdkmanrc`; Boot 4 also builds on 25 |
| Spring Boot | **4.0.6** | Stable 4.0 line; do not chase 4.1+ |
| Spring Security | session login + BCrypt | Bootstrap user `admin`/`admin` seeded via Flyway `V3` (demo only) |
| Hibernate ORM | 7.x | Naming strategy: `PhysicalNamingStrategyStandardImpl` |
| Jackson | **3.x** (`tools.jackson`) | Boot 4 default |
| Flyway | portable ANSI SQL migrations | H2 today, PostgreSQL via `prod` profile later |
| DB | embedded **H2 file-mode** (`./api/data/`, gitignored) | No Docker required for local dev |
| PDF | OpenPDF 2.0.3 | `GET /api/persons/{id}/pdf` |
| React | **19.2.x** | Classic JSX transform |
| TypeScript | 5.8.x | Strict mode |
| Node | **≥ 22.11.0** | Enforced via `ui/package.json` `engines` |
| Bundler | webpack 5 | sass-loader 17, modern Sass API |

Full upgrade history and deferred items (Keycloak, PostgreSQL, OTel/Prometheus,
Docker as production target): [`docs/UPGRADE_PLAN.md`](./docs/UPGRADE_PLAN.md).

## Quick start (no Docker, no `.env` required)

The default local dev path is **two terminals**, embedded H2, no external services.

### Backend (`api/`)

```bash
cd api
sdk env                # picks up Java 21 from .sdkmanrc
./mvnw spring-boot:run
```

The API comes up on `http://localhost:8082`. Bootstrap login is **`admin` /
`admin`** (seeded via Flyway migration `V3`, BCrypt-hashed). The H2 data file
lives under `api/data/` and is gitignored.

OpenAPI / Swagger UI: `http://localhost:8082/swagger-ui.html`
H2 console: `http://localhost:8082/h2-console` (JDBC URL: `jdbc:h2:file:./data/techdemo`)

### Frontend (`ui/`)

```bash
cd ui
npm install --legacy-peer-deps     # Storybook addon pulls a React-18-peer lib, hence legacy-peer-deps
npm start
```

The dev server runs on `http://localhost:8080`. The UI reads
`APP_API_SERVER_URL=http://localhost:8082` from the root `.env` file for API calls.

Other useful UI scripts:

| Command | What it does |
| ------- | ------------ |
| `npm run storybook` | Storybook on `http://localhost:6006` for component development |
| `npm run smoke` | Playwright app smoke (login shell, console-error gate) |
| `npm run smoke:storybook` | Playwright Storybook smoke (Button + Form stories) |
| `npm run playwright` | Full Playwright E2E suite (installs browsers first) |
| `npm run build` | Production webpack build |
| `npm run lint` | `tsc --noEmit` + ESLint |

> **Internal registry note:** if you previously had a TFS-internal npm feed in
> `~/.npmrc`, this repo's `package-lock.json` is now fully public-registry
> (`registry.npmjs.org`). Don't reintroduce TFS-resolved URLs — Dependabot
> will break on the next run.

## Quality & security tooling

The full table with gate behaviour lives in [SECURITY.md](./SECURITY.md). Quick reference:

| Scanner | Where | Gate |
| ------- | ----- | ---- |
| **GitGuardian** | GitHub PR check | Blocks PR on detected secrets |
| **Dependabot** | Weekly schedule | Opens PRs; humans review/merge |
| **FOSSA** | GitHub Action `fossa-scan` on push/PR to `main` | Scan-only (non-blocking, see SECURITY.md for why) |
| **Checkstyle** | `./mvnw validate` | Hard gate (`failOnViolation=true`) |
| **JaCoCo** | `./mvnw verify` | Report only (no minimum yet) |
| **ESLint + `tsc --noEmit`** | `npm run lint` (local + CI) | Hard gate on errors |
| **Trivy (`fs`)** | On-demand | HIGH/CRITICAL maintained at 0 on `main` |
| **SonarQube** (internal `10.16.35.93:9000`) | On-demand | Dashboard only |
| **CircleCI** | Every push | Hard gate (backend `mvn verify` + frontend webpack build) |

FOSSA shield badge:

[![FOSSA Status](https://app.fossa.com/api/projects/custom%2B46919%2Fgithub.com%2FFoundation-s-r-o%2Ftech-demo-test.git.svg?type=shield)](https://app.fossa.com/projects/custom%2B46919%2Fgithub.com%2FFoundation-s-r-o%2Ftech-demo-test.git?ref=badge_shield)

## Verification commands

See [`CLAUDE.md`](./CLAUDE.md#verifying-code-changes) for the full small-vs-large-change verification recipe. Headlines:

```bash
# Small UI change
cd ui && npm run lint

# Small API change
cd api && ./mvnw checkstyle:check test

# Bigger change — run the lot
cd ui && npm run lint && npm run smoke && npm run smoke:storybook
cd api && ./mvnw clean verify
trivy fs . --scanners vuln --severity HIGH,CRITICAL
```

Comprehensive health check (build + lint + tests + FOSSA): `./doctor`.

## Editor setup

The Java code style is enforced by Checkstyle (`api/checkstyle/checkstyle.xml`)
and Maven gates the build on it. For interactive formatting in your IDE, import
the matching formatter rules from `api/checkstyle/code-formatter-rules.xml`:

- **Eclipse:** *Window → Preferences → Java → Code Style → Formatter → Import*
- **IntelliJ IDEA:** *Settings → Code Style → Java → Manage → Import* (use the Eclipse XML profile importer)

The TypeScript / React side is enforced by ESLint (`ui/.eslintrc*`) + `tsc --noEmit`
on `npm run lint`. Most editors pick up the project ESLint config automatically.

## Working in this repo

| Rule | Why |
| ---- | --- |
| Track work in [Issues](https://github.com/Foundation-s-r-o/tech-demo-test/issues) | One issue per piece of work; reference it in commits |
| Small PRs — 100s of lines is too big (exceptions must be justified) | Reviewable in one sitting; keeps blast radius small |
| Keep `main` releasable | If `./mvnw verify` + `npm run lint` + smoke don't pass, the PR isn't ready |
| Secrets via env vars only | Never in code, never in `.env` committed files — see CLAUDE.md |

## Engineering metrics

This repo is connected to [LinearB](https://app.linearb.io/dashboard?isOrgSettings=true) (GitHub SSO) — tracks DORA metrics for **internal team-level gauging only**, not individual performance.

Reference: [DORA 2023 report](https://services.google.com/fh/files/misc/2023_final_report_sodr.pdf) · [DORA Quick Check](https://dora.dev/quickcheck/).

## Reporting security issues

Use GitHub Private Vulnerability Reporting — see [SECURITY.md](./SECURITY.md).

## Deferred / stale scaffolding (not the current path)

The following files are kept in-tree as forward scaffolding for the future
`prod` profile / Keycloak / PostgreSQL phase. **They do not reflect the current
local dev path** and may be removed or rewritten when that phase lands. Do not
rely on them today:

| Path | Status | Why kept |
| ---- | ------ | -------- |
| `docker-compose.yml` | Stale | References the removed MariaDB + Redis stack |
| `run_application.sh` | Stale | Assumes `.env`-driven external services |
| `.env - Sample` | Stale | Many keys reference deferred services (OpenAI, Redis, Loki) |

Tracked under tech-debt in `docs/UPGRADE_PLAN.md`.

## Further reading

- [SECURITY.md](./SECURITY.md) — vulnerability reporting, demo-mode caveats, hardening checklist
- [CLAUDE.md](./CLAUDE.md) — development guidelines (stack rules, verification recipe, code style)
- [docs/UPGRADE_PLAN.md](./docs/UPGRADE_PLAN.md) — Spring Boot 4 / Java 21 upgrade history and deferred phases
