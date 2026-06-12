# Project Overview

> Current-state overview of `tech-demo-test`, generated from the live source tree on 2026-06-09.

## Purpose

`tech-demo-test` is Foundation s.r.o.'s full-stack reference application and starter template. Its business scope is intentionally small: authenticated users can manage `Person` records and export a person detail as a PDF. The repository's broader purpose is to demonstrate the preferred application structure, dependency baseline, testing strategy, and quality controls for future projects.

This is a rolling demo on `main`, not a production-ready product or a versioned release line.

## Current Stack

| Area | Technology |
| --- | --- |
| Backend runtime | Java 21 LTS |
| Backend framework | Spring Boot 4.0.6, Spring MVC, Spring Security |
| Persistence | Spring Data JPA, Hibernate 7, HikariCP |
| Database | Embedded H2 in file mode for local development |
| Schema management | Flyway with portable ANSI SQL migrations |
| API documentation | springdoc OpenAPI and Swagger UI |
| PDF generation | OpenPDF 2.0.3 |
| Metrics | Spring Boot Actuator and Micrometer Prometheus registry |
| Frontend | React 19.2, React Router 7.12, TypeScript 6.0 |
| UI/build tooling | Webpack 5, Bootstrap 5, SCSS, Formik, Yup, i18next |
| API client | TypeScript Axios client generated from OpenAPI |
| Backend tests | JUnit 5, MockMvc, Maven Surefire/Failsafe, JaCoCo |
| Frontend tests | Playwright application and Storybook smoke tests |

Context7 was used to validate the framework terminology against the current React (`/facebook/react`) and Spring Boot (`/spring-projects/spring-boot`) documentation. The project-specific behavior below comes from this repository's source and configuration.

## Architecture

The application is a modular monolith with a React single-page application calling a Spring Boot JSON API.

```text
Browser
  -> React SPA
     -> React Router protected routes
     -> AuthProvider and session-aware Axios client
     -> generated TypeScript API client
  -> /api/auth and /api/persons
     -> Spring Security filter chain
     -> REST controllers
     -> API services and DTO mapping
     -> domain service and JPA repositories
     -> Hibernate / HikariCP
     -> H2 file database
```

The frontend and backend are separate projects during development. The backend also contains an SPA fallback resource handler so a future packaged deployment can serve frontend static assets and client-side routes from one application.

## Backend

Backend source lives under `api/src/main/java/sk/foundation/techdemo` and is organized primarily by feature.

### Authentication

- `auth/SecurityConfig.java` protects every route except login, Swagger/OpenAPI, H2 console, and Actuator endpoints.
- `AuthController` exposes JSON login, current-user, and logout operations while preserving a server-side HTTP session.
- `AppUserDetailsService` loads users from the database through `UserRepository`.
- Passwords use BCrypt.
- Flyway migration `V3` seeds the local demo account `admin` / `admin`.

### Person feature

- `PersonController` exposes CRUD, filtering, sorting, paging, and PDF download endpoints.
- `PersonApiServiceImpl` owns API-oriented orchestration, DTO projection, not-found behavior, and duplicate-email conflict handling.
- `PersonServiceImpl` owns entity mutation and persistence operations.
- `PersonApiRepositoryImpl` uses the JPA Criteria API for dynamic filters, ordering, paging, counts, and DTO projections.
- `PersonPdfService` renders a simple person-detail PDF with OpenPDF.

### Shared infrastructure

- `ApiExceptionHandler` maps application and persistence failures to consistent API responses.
- `IdentifiableEntity` provides shared entity identity behavior.
- `WebConfiguration` configures CORS and SPA history fallback.
- `application.properties` configures H2, Hibernate validation, Flyway, HikariCP, Actuator, and Prometheus metrics.

## Frontend

Frontend source lives under `ui/src`.

- `index.tsx` creates the React root and installs `BrowserRouter`, authentication context, modal context, Bootstrap, and global SCSS.
- `components/router/routes.tsx` defines login, home, person-list, person-add, and person-edit routes.
- `RequireAuth` redirects unauthenticated users to `/login`.
- `AuthProvider` restores persisted user state by validating the active backend session with `/api/auth/currentuser`.
- `common/axiosClient.ts` enables credentialed requests, removes empty query parameters, and serializes dates.
- `api/generated` contains the OpenAPI-generated Axios client; `api/api.ts` injects the shared Axios instance.
- `pages/persons` contains list, add, edit, form, table, and validation logic.
- `components/form` and `components/shared` provide reusable form, table, modal, button, and page-layout primitives.
- `assets/translations/sk.json` currently supplies Slovak translations.

## HTTP Surface

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/api/auth/login` | Authenticate and establish an HTTP session |
| `GET` | `/api/auth/currentuser` | Return the authenticated user |
| `POST` | `/api/auth/logout` | Invalidate the current session |
| `GET` | `/api/persons` | Filtered, sorted, paged person list |
| `POST` | `/api/persons` | Create a person |
| `GET` | `/api/persons/{id}` | Fetch person detail |
| `PUT` | `/api/persons/{id}` | Update a person |
| `DELETE` | `/api/persons/{id}` | Delete a person |
| `GET` | `/api/persons/{id}/pdf` | Download person detail as PDF |

Swagger UI is exposed at `/swagger-ui.html`; OpenAPI JSON is exposed under `/v3/api-docs`.

## Data Model

Flyway owns the schema and Hibernate runs with `ddl-auto=validate`.

- `PERSON` stores names, email, address, state, and phone number. Email is unique.
- `USERS` stores username, BCrypt password hash, name, role, and enabled status. Username is unique.
- Migrations deliberately avoid H2-specific SQL so a future PostgreSQL profile is easier to introduce.
- Local data persists in an H2 file under `api/data/` when the API is started from `api/`.

## Testing And Quality Gates

The repository contains 37 backend production classes, 9 backend test classes, 91 TypeScript/TSX source files, and 3 Playwright specifications.

Backend verification:

```bash
cd api
./mvnw checkstyle:check test
./mvnw clean verify
```

`verify` runs Checkstyle, unit tests, integration tests, and JaCoCo reporting. Integration tests use H2 and do not require Docker.

Frontend verification:

```bash
cd ui
npm run lint
npm run build
npm run smoke
npm run smoke:storybook
```

The main automated controls are Checkstyle, TypeScript strict checks, ESLint, Playwright, CircleCI, Dependabot, FOSSA, GitGuardian, Trivy, and SonarQube. `./doctor` combines the main local build and scan steps, but requires the FOSSA CLI for its final checks.

## Local Development

The intended local workflow is Docker-free:

```bash
# Terminal 1
cd api
sdk env
./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# Terminal 2
cd ui
npm install --legacy-peer-deps
npm start
```

The explicit `local` profile enables local conveniences and creates the test-only `admin/admin`
account. The default and `prod` profiles do not create a known user.

## Security Posture

The API uses database-backed Spring Security sessions, CSRF cookie/header protection, exact-origin
CORS, session-ID rotation, health-only public Actuator access, and explicit cookie settings. H2 and
the known local account remain development tools, not deployment targets. Rate limiting, production
identity lifecycle, TLS termination, and audit operations still depend on the selected platform.

## Active Versus Stale Assets

The source, Maven build, npm build, H2 configuration, and CircleCI checks represent the active application path.

Unsupported Docker, Compose, ECR, and Elastic Beanstalk scaffolding has been removed. The remaining
`run_application.sh` is local-only. Observability configuration files are reference inputs and do
not define a supported deployment.

## Planned Direction

The documented deferred production path is:

1. Add a PostgreSQL production profile and PostgreSQL-backed integration coverage.
2. Replace the local session-login layer with Keycloak/OIDC when a real deployment requires it.
3. Add platform-specific TLS termination, rate limiting, audit logging, and secret management.
4. Introduce packaging and fuller observability only when deployment requirements justify them.
5. Increase backend coverage and enforce a meaningful coverage gate as the business surface grows.

## Recommended Reading Order

1. `README.md` for setup and common commands.
2. This file for the current architecture and boundaries.
3. `CLAUDE.md` for implementation conventions and verification depth.
4. `SECURITY.md` for demo caveats and production hardening.
5. `docs/UPGRADE_PLAN.md` for migration history and deferred phases.
6. `docs/decisions/` for architecture decision records.
