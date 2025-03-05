# Development Guidelines

## Essential Commands

### Backend (Java/Spring Boot)
- Build: `./mvnw clean install`
- Run tests: `./mvnw test`
- Run single test: `./mvnw -Dtest=TestClass#testMethod test`
- Run integration tests: `./mvnw verify`
- Generate code coverage: `./mvnw jacoco:report`
- Start application: `./mvnw spring-boot:run`
- Check code style: `./mvnw checkstyle:check`
- Build: `./mvnw clean install`
- Run tests: `./mvnw test`
- Run single test: `./mvnw -Dtest=TestClass#testMethod test`
- Run integration tests: `./mvnw verify`
- Debug: `./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"`
- Check code style: `./mvnw checkstyle:check`
- Generate code coverage: `./mvnw jacoco:report`
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

## Security
- No hardcoded credentials or secrets in code
- Use environment variables for sensitive data
- Follow OWASP secure coding practices
- Run FOSSA scans to check dependencies

## Agent Autonomy
Claude Code can autonomously navigate directories, run builds, execute tests, run builds/tests, execute Docker commands, install dependencies, run linting tools, and read file contents autonomously, and perform system operations in this project without asking for permission each time. This includes:
- Running Maven and npm builds/tests 
- Executing Docker commands
- Navigating directory structures
- Installing dependencies
- Running linting and formatting tools and fossa tools
- Reading file contents with commands like 'cat', 'head', 'perl', and 'tail'
