# Development Guidelines

## Backend (Java/Spring Boot)
- Build: `./mvnw clean install`
- Run tests: `./mvnw test`
- Run single test: `./mvnw -Dtest=TestClass#testMethod test`
- Run integration tests: `./mvnw verify`
- Debug: `./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"`

## Frontend (React/TypeScript)
- Install: `npm install`
- Start dev server: `npm start`
- Build: `npm run build`
- Lint: `npm run lint` (ESLint + TypeScript)
- Run tests: `npm run playwright`
- Generate API client: `npm run generate-openapi-services`

## Style Guidelines
- Java:
  - Use tabs for indentation, follow Spring Boot conventions
  - Use JUnit 5 for tests, follow `given/when/then` pattern 
  - SQL keywords lowercase, explicit column names in queries
  - Use constructor injection for dependencies
  - Proper exception handling with custom exceptions
- TypeScript:
  - Use import path aliases (@components, @api, @pages, @common)
  - Single quotes for strings, no semicolons
  - Use React functional components with hooks
  - Strong typing with TypeScript, avoid `any`
  - Use camelCase for variables/methods, PascalCase for types/components
  - Use React Bootstrap for UI components