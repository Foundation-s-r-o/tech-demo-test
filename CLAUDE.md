# Development Guidelines

## Backend (Java/Spring Boot)
- Build: `./mvnw clean install`
- Run tests: `./mvnw test`
- Run single test: `./mvnw -Dtest=TestClass#testMethod test`
- Run integration tests: `./mvnw verify`
- Code style: Enforced by Checkstyle (tab indentation, line length max 120 chars)

## Frontend (React/TypeScript)
- Install: `npm install`
- Start dev server: `npm start`
- Build: `npm run build`
- Lint: `npm run lint`
- Run tests: `npm run playwright`
- Generate API client: `npm run generate-openapi-services`

## Style Guidelines
- Java: Use tabs for indentation, follow Spring conventions, SQL keywords lowercase
- TypeScript: Import paths use aliases (@components, @api, etc.)
- All variables/methods follow camelCase naming
- Max line length: 120 characters
- Avoid wildcard imports
- Use appropriate error handling (no System.out.println)