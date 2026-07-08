---
status: accepted
---
# Source repository: GitHub

## Context and Problem Statement

`tech-demo-test` needs version control that balances security, collaboration,
CI/CD integration, and accessibility. The options were local hosting (company
servers), GitLab, or GitHub.

## Decision Drivers

* **Security** — code stored securely, in line with our data-protection policy.
* **Collaboration** — effective across teams and locations.
* **Integration** — CI/CD and tooling with minimal setup.
* **Accessibility** — easy access for all team members.

## Considered Options

* Local code storage (company servers)
* GitLab
* GitHub

## Decision Outcome

Chosen option: **GitHub**, for its CI/CD via GitHub Actions, native security
features (secret scanning, Dependabot, CodeQL), and the largest integration
ecosystem — all of which this repo's quality gate relies on
([ADR-0008](0008-strict-quality-security-gate.md)).

### Consequences

* Good — GitHub Actions and the native scanners drive the quality gate.
* Good — broad third-party integration (FOSSA, SonarQube) with little setup.
* Bad — a dependency on an external cloud service; accepted for a public template
  repo with no real-world data.

Local hosting was rejected (setup/maintenance cost, no built-in tooling); GitLab
is comparable but has a smaller ecosystem for our needs. Revisit if compliance or
data-sensitivity requirements change.
