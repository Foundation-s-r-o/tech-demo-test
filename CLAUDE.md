# Development Guidelines

## Backend (Java/Spring Boot)
- Build: `./mvnw clean install`
- Run tests: `./mvnw test`
- Run single test: `./mvnw -Dtest=TestClass#testMethod test`
- Run integration tests: `./mvnw verify`
- Debug: `./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"`
- Check code style: `./mvnw checkstyle:check`
- Generate code coverage: `./mvnw jacoco:report`

## Frontend (React/TypeScript)
- Install: `npm install`
- Start dev server: `npm start`
- Build: `npm run build`
- Lint: `npm run lint` (ESLint + TypeScript)
- Run tests: `npm run playwright`
- Generate API client: `npm run generate-openapi-services`
- Run Storybook: `npm run storybook`

## Style Guidelines
- Java:
  - Use tabs for indentation (4 spaces width), max line length 120
  - Use JUnit 5 for tests, follow `given/when/then` pattern 
  - SQL keywords lowercase, explicit column names in queries
  - Use constructor injection for dependencies
  - Proper exception handling with custom exceptions
  - Follow checkstyle rules for naming, imports and formatting
- TypeScript:
  - Use import path aliases (@components, @api, @pages, @common)
  - Single quotes for strings, no semicolons
  - Use React functional components with hooks
  - Strong typing with TypeScript, avoid `any`
  - Use camelCase for variables/methods, PascalCase for types/components
  - Use React Bootstrap for UI components

## Docker Commands
- Start with Docker: `docker-compose up -d`
- Run API tests in Docker: `docker-compose exec api ./mvnw test`
- Build UI in Docker: `docker-compose exec ui npm run build`