# Development Guidelines

## Project Overview

This is a full-stack Java/Spring Boot + React/TypeScript demo application serving as a template for future projects. It implements a comprehensive tech stack with observability, security scanning, and modern development practices.

### Architecture

**Backend (Java/Spring Boot)**
- Entry Point: `TechDemoApplication.java:10` - Standard Spring Boot application
- Framework: Spring Boot 3.4.3 with Java 17
- Database: MariaDB with JPA/Hibernate, Flyway migrations
- Caching: Redis with Redisson
- Key Features:
  - Person CRUD operations (`persons/` package)
  - Authentication system (`auth/` package) 
  - AI integration (`ai/` package)
  - Infrastructure utilities (`infrastructure/` package)

**Frontend (React/TypeScript)**
- Entry Point: `index.tsx:10` - React 19 with ReactDOM
- Framework: React with TypeScript, React Router v7
- Styling: Bootstrap 5.3.3 with SCSS
- Key Features:
  - Authentication context and protected routes
  - Person management pages
  - Reusable UI components
  - Form handling with Formik + Yup validation

### Development Stack

**Build & Packaging**
- Backend: Maven with comprehensive plugin setup (Checkstyle, JaCoCo, Failsafe)
- Frontend: Webpack with TypeScript, ESLint, Playwright testing

**Containerization**
- Full Docker Compose stack with 9 services:
  - Application (API + UI), MariaDB, Redis
  - Observability: Prometheus, Grafana, Jaeger, Loki, OpenTelemetry Collector
  - Redis Commander, New Relic agent

**Security & Quality**
- FOSSA license scanning and vulnerability detection
- Checkstyle code style enforcement  
- Dependabot automated dependency updates
- Bouncy Castle cryptography libraries
- Explicit security patches (Tomcat, okio)

**Observability**
- Distributed tracing with OpenTelemetry and Jaeger
- Metrics collection with Prometheus
- Centralized logging with Loki
- Grafana dashboards

### Key Architectural Patterns
- Clean architecture with separate domain/API layers
- Constructor injection with Lombok `@RequiredArgsConstructor`
- Repository pattern with JPA
- React functional components with hooks
- Type-safe API client generation from OpenAPI specs

## Essential Commands

### Backend (Java/Spring Boot)
- Build: `./mvnw clean install`
- Run tests: `./mvnw test`
- Run single test: `./mvnw -Dtest=TestClass#testMethod test`
- Run integration tests: `./mvnw verify`
- Debug: `./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"`
- Generate code coverage: `./mvnw jacoco:report`
- Check code style: `./mvnw checkstyle:check`
- Start application: `./mvnw spring-boot:run`

### Frontend (React/TypeScript)
- Install: `npm install --legacy-peer-deps`
- Start dev server: `npm start`
- Build: `npm run build`
- Lint: `npm run lint`
- Run tests: `npm run playwright`
- Run specific test: `npx playwright test tests/path/to/test.spec.ts`
- Generate API client: `npm run generate-openapi-services`

### Docker Commands
- Start all services: `docker-compose up -d`
- Run tests in container: `docker-compose exec api ./mvnw test`
- View logs: `docker-compose logs -f [service_name]`

### Project Health & Quality
- **Comprehensive health check**: `./doctor` - Runs build, lint, tests, and FOSSA scans
- **License scanning**: `fossa analyze` - Scan dependencies for license compliance
- **Security testing**: `fossa test` - Test for license or security issues

## Code Style Guidelines

### Java
- Indentation: tabs (4 spaces width), max line length 120
- Imports: avoid star imports, no unused imports
- Tests: JUnit 5 with given/when/then pattern
- DI: constructor injection with `@RequiredArgsConstructor` and final fields
- SQL: keywords lowercase, explicit column names
- Exceptions: specialized exceptions handled in `ApiExceptionHandler`
- Lombok: use annotations to reduce boilerplate
- JPA: entities extend `IdentifiableEntity<>`, use proper column mappings

### TypeScript/React
- Paths: use aliases (@components, @api, @pages, @common)
- Syntax: single quotes, no semicolons
- Components: functional with hooks, avoid class components
- Types: strong typing with interfaces, avoid `any` type
- Naming: camelCase for variables/methods, PascalCase for types/components
- Props: explicit interfaces with proper naming (ComponentProps)
- Styling: React Bootstrap with SCSS using BEM convention
- ESLint: Enhanced configuration with `@typescript-eslint/no-unsafe-argument` error enforcement

## Security
- No hardcoded credentials or secrets in code
- Use environment variables for sensitive data
- Follow OWASP secure coding practices
- Run FOSSA scans to check dependencies

## Recent Updates (Updated: 2025-08-19)

### Major Improvements
- **Project Health Script**: Added `./doctor` comprehensive health check tool
- **Enhanced Linting**: Improved ESLint configuration with stricter TypeScript rules
- **Security Updates**: Multiple Dependabot updates including:
  - TestContainers BOM to 1.20.6
  - Bouncy Castle libraries (bcprov-jdk18on, bcpkix-jdk18on) to 1.80
  - OpenTelemetry exporter to 1.48.0
  - okio-jvm to 3.10.2 (CVE fix)

### Code Quality Enhancements
- Fixed TypeScript issues in React DatePicker components
- Updated `DatePickerProps` import for better type safety
- Enhanced ESLint rules with `@typescript-eslint/no-unsafe-argument` enforcement
- Cleaned up duplicate commands in build configuration

### Infrastructure Changes
- Improved log directory management with .gitignore updates
- Enhanced build scripts for UI components
- Updated OpenAPI service generation scripts

## Agent Autonomy
Claude Code can autonomously navigate directories, run builds, execute tests, run builds/tests, execute Docker commands, install dependencies, run linting tools, and read file contents autonomously, and perform system operations in this project without asking for permission each time. This includes:
- Running Maven and npm builds/tests 
- Executing Docker commands
- Navigating directory structures
- Installing dependencies
- Running linting and formatting tools and fossa tools
- Reading file contents with commands like 'cat', 'head', 'perl', and 'tail'
- Running the `./doctor` health check script
