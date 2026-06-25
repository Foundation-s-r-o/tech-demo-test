# Choosing a Source Repository for Tech-Demo Project

## Context and Problem Statement

The tech-demo project requires a reliable and scalable version control system to manage its source code and collaborate effectively. The choice is between using local code storage solutions (on company servers), GitLab, or GitHub, with the need to balance security, collaboration efficiency, integration ease, and accessibility.

## Decision Drivers

* **Security**: Ensuring that our source code is stored securely and complies with our data protection policies.
* **Collaboration**: Facilitating effective collaboration across different teams and departments.
* **Integration**: Seamless integration with existing tools and workflows, especially CI/CD pipelines.
* **Accessibility**: Easy access for all team members, regardless of their location.

## Considered Options

* Local Code Storage (on company servers)
* GitLab
* GitHub

## Decision Outcome

Chosen option: **"GitHub"**, because it provides superior tooling, integration options for CI/CD via GitHub Actions, and robust security features. GitHub's extensive community and support resources also make it an ideal choice for open collaboration.

### Consequences

* **Good**, because GitHub Actions offer powerful automation and CI/CD capabilities that can streamline our workflows.
* **Good**, because GitHub's security features, such as automated vulnerability scanning, enhance our project's protection.
* **Bad**, because reliance on an external service (GitHub) introduces a dependency and potential for service disruptions.
* **Good**, because GitHub's integration with tools like SonarQube for code quality and Docker for container management will simplify our DevOps processes.

## Pros and Cons of the Options

### Local Code Storage

* **Good**, because it allows for full control over our data and infrastructure.
* **Bad**, because it requires significant setup and maintenance effort.
* **Bad**, because it lacks the built-in integration and tools that GitLab and GitHub offer.
* **Neutral**, because while it enhances security through control, it demands extensive security expertise to set up properly.

### GitLab

* **Good**, because it provides a similar range of features as GitHub, including built-in CI/CD.
* **Good**, because it allows self-hosting options, which can be aligned with company policies.
* **Bad**, because it generally has smaller community support and fewer integration plugins compared to GitHub.
* **Neutral**, because while it offers comprehensive DevOps features, the setup can be complex depending on the deployment model.

### GitHub

* **Good**, because of its extensive ecosystem, including a large number of users and third-party integrations.
* **Good**, because GitHub Actions and GitHub Packages seamlessly integrate with our existing workflows.
* **Bad**, because it is a cloud-based solution, which involves potential risks of third-party service failures.
* **Good**, because it offers advanced security features and regular updates that enhance project security.

## More Information

GitHub is chosen for its robust ecosystem, which supports an agile development environment with tools like Docker for containerization and SonarQube for continuous code quality checks. This decision will be revisited as our project scales and if significant changes in compliance or security requirements occur.
