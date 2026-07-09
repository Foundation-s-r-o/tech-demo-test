# tech-demo-test

Foundation reference template for a small Spring Boot + React application. The
repo is a rolling demo on `main`, not a released product.

The template keeps the local loop Docker-free while retaining the security,
quality, and documentation habits expected in real projects.

## Quick Start

Run the backend:

```bash
cd api
sdk env
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Run the frontend:

```bash
cd ui
npm install --legacy-peer-deps
npm start
```

Open `http://localhost:8080`. The API runs on `http://localhost:8082`.

The `local` profile creates demo accounts only for local development. See
[SECURITY.md](./SECURITY.md#local-test-accounts-and-production-provisioning)
before changing account or profile behavior.

## Stack

- Backend: Java 21, Spring Boot 4, Spring Security session auth, Flyway, H2.
- Frontend: React 19, TypeScript, webpack, Axios, generated OpenAPI client.
- Observability: Spring Boot Actuator + Micrometer only; full runtime stack is
  deferred.
- Verification: `./doctor` is the full local health check.

Pinned versions live in [api/pom.xml](./api/pom.xml),
[ui/package.json](./ui/package.json), and [docs/UPGRADE_PLAN.md](./docs/UPGRADE_PLAN.md).

## Daily Commands

```bash
# backend checks
cd api && ./mvnw checkstyle:check test

# frontend checks
cd ui && npm run lint

# full local gate
./doctor
```

Security scan entry point:

```bash
./scripts/security_scan.sh
```

The script runs Semgrep with metrics/tracing disabled, Trivy, and `npm audit`.
`doctor` calls the same script.

## Verification Rule

Use [CLAUDE.md](./CLAUDE.md#verifying-code-changes) as the authoritative
verification recipe. In short:

- Small frontend changes: `cd ui && npm run lint`.
- Small backend changes: `cd api && ./mvnw checkstyle:check test`.
- Broad or risky changes: run `./doctor`.

## Documentation Map

- [SECURITY.md](./SECURITY.md) - security policy, controls, caveats, residual gaps.
- [CLAUDE.md](./CLAUDE.md) - development rules and verification workflow.
- [docs/ARCHITECTURE.md](./docs/ARCHITECTURE.md) - architecture, layout, data flows.
- [docs/ADR/](./docs/ADR/) - architecture decision records.
- [docs/UPGRADE_PLAN.md](./docs/UPGRADE_PLAN.md) - current baseline and deferred work.
- [TASKS.md](./TASKS.md) - ordered cleanup and implementation task list.

## Deployment Status

There is no supported deployment artifact in this template. A production fork
must define its own threat model, `prod` profile, secret management, TLS
boundary, identity provisioning, and observability runtime.
