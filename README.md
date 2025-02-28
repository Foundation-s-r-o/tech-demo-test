# Project - Demo / Template Application 

This repository contains a demo application that will be used as a template for a new application. 

The goal is to have a 'standard project stack' we could use as a template for future projects.

## A Standard Tech Stack Document

The application should implement suggestions defined in the tech stack proposal document:
https://docs.google.com/document/d/16zLZa3JL4DSsK9vqUzfF046CPau5ziO5cJfsdrBqhe8/edit?usp=drive_link

This document also contains additional information on development (e.g., tools, dev environment, etc.). These should also be applied here in this repo if it makes sense. 

Please comment on the document if you have any feedback.

## TODO
This repository must be updated to become an app usable as a starting template for new projects.

The app must also include things like telemetry, logging, etc., everything mentioned in that ideal tech stack, including infrastructure (Docker, etc.) and development tools (CD/CI - GitHub Actions, Linter/SonarQube, etc.).

The project must be continuously updated to new versions, security issues must be fixed, etc. See also https://github.com/Foundation-s-r-o/tech-demo/security/dependabot

## Issues
Make changes to the project only through tasks created in [Issues](https://github.com/Foundation-s-r-o/tech-demo/issues?q=is%3Aissue+is%3Aopen+sort%3Aupdated-desc).

- Anything done in the repo should have a task in Issues, 
- Issues should be assigned to a person
- The tasks/issues must be small, so that they can be finished in 1-2 days
- Rule of thumb - if the PR has 100s of lines of code, it is too big. Use small PRs. There are exceptions, but they must be justified.
- All commits must reference the specific task/Issue

## Metrics

This repo is connected to [LinearB](https://app.linearb.io/dashboard?isOrgSettings=true) that tracks engineering metrics. Use your GitHub credentials to log in. 

LinearB tracks DORA metrics. These metrics are for internal use only! The team can use them to gauge how it compares to other teams; see

- [DORA 2023 report](https://services.google.com/fh/files/misc/2023_final_report_sodr.pdf)
- [DORA Quick Check](https://dora.dev/quickcheck/)

## MVP

Minimum Viable Product: you do not have to include everything in the project. Certainly, you do not have to have it all in the beginning. For simplicity, start with the minimal configuration, add more features later.

The rule of thumb is to use minimal tools so you can run, test, and deploy functional application

What is needed at the beginning:

- Static code checking is probably needed very early
- Docker (?)

What is NOT needed at the beginning:

- Telemetry
- Storybook
- Microservices are not needed, but modularity is
- TBD ...

## Private info

Store all private keys and values in the local environment variables. Please do not put them into the code repo.

## Running the application locally

>You can run whole app via script from root of the project.
### Both API and UI via Script
```
1. Copy '.env - Sample' to '.env'
2. Edit '.env' file & fill in the missing values
3. Open Git Bash terminal
4. ./run_application.sh
5. you can choose between running app locally on your machine(l/L), in docker(d/D) or both(b/B)
```
**if you have any issues, please read outputs from terminal and ensure that you have right values in .env file + you are using right versions metioned in UI [here](ui/README.md) and API [here](api/README.md).**

### Just UI locally
```
1. Copy '.env - Sample' to '.env'
2. Edit '.env' file & fill in missing values
3. cd ui
4. npm install
5. npm run start
6. The UI will be running on port 8080 by default.
```

For more details and all required version see UI Documentation [here](ui/README.md)

### Just API locally

If you didn´t do it already in previous UI Step then:
```
1. Copy '.env - Sample' to '.env'
2. Edit '.env' file & fill in missing values
```
else run just those commands:
```
3. cd api
4. ./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.datasource.url=jdbc:mysql://localhost:3306/tech-demo --spring.datasource.username=[db user] --spring.datasource.password=[db pwd]"
```

For more details and all required version see API Documentation [here](api/README.md)

## Running the application in Docker
1. Copy '.env - Sample' to '.env'
2. Edit '.env' file & fill in missing values
3. Run
```
docker compose up --build -d
```

## Notes
1. Redis is disabled by default as it is not available during building and unit-tests in GitHub.

   To enable it (currently will work in local environment) in application.properties:
```
   caching.enabled=true 
```   
2. Loki appender is disabled by default as it is not available during building and unit-tests in GitHub.

   To enable it (currently will work in local environment) in application.properties:
```
   loki.enabled=true
```

## Code Quality

Results from Sonar and CodeClimate analysis can be found in ReadMe file for backend in /api folder and for frontend in /ui folder. 
# FOSSA License Scanning

This project uses FOSSA for license scanning and dependency vulnerability detection.

## Current Status

[\![FOSSA Status](https://app.fossa.com/api/projects/custom%2B46919%2Fgithub.com%2FFoundation-s-r-o%2Ftech-demo-test.git.svg?type=shield)](https://app.fossa.com/projects/custom%2B46919%2Fgithub.com%2FFoundation-s-r-o%2Ftech-demo-test.git?ref=badge_shield)

## How to Run FOSSA Scans

1. Install the FOSSA CLI:
   ```bash
   curl -H 'Cache-Control: no-cache' https://raw.githubusercontent.com/fossas/fossa-cli/master/install.sh | bash
   ```

2. Set your FOSSA API key as an environment variable (NEVER commit this to source control):
   ```bash
   export FOSSA_API_KEY=your-api-key
   ```

3. Run a scan:
   ```bash
   fossa analyze
   ```

4. Test for license or security issues:
   ```bash
   fossa test
   ```

## Viewing Reports

After running a scan, you can view the results in the [FOSSA dashboard](https://app.fossa.com/projects/custom%2B46919%2Fgithub.com%2FFoundation-s-r-o%2Ftech-demo-test.git).

## CI/CD Integration

This project includes a GitHub Actions workflow in `.github/workflows/fossa.yml` that automatically runs FOSSA scans on pushes to the main branch and on pull requests.

For more information on FOSSA, visit [the FOSSA documentation](https://docs.fossa.com/).
