---
status: accepted
date: 2026-06-25
deciders: Foundation engineering + CTO
---
# Observability via Actuator + Micrometer; full stack deferred

## Context and Problem Statement

The template needs an observability story without dragging a dedicated
monitoring stack (Prometheus, Grafana, Loki, Jaeger) into local development — a
set of stateful services that would violate the minimal-stack premise
([ADR-0002](0002-minimal-docker-free-stack.md)). It must still expose health and
metrics in a way a real stack can attach to later.

## Decision Drivers

* Minimal moving parts; no extra services for local dev.
* A health/metrics surface that downstream projects can extend.
* Keep the path to a full observability stack open (config, not rewrite).

## Considered Options

* **Spring Boot Actuator + Micrometer** (Prometheus-compatible endpoint).
* **Full OpenTelemetry + Prometheus + Grafana + Loki + Jaeger** from day one.

## Decision Outcome

Chosen option: **Actuator + Micrometer**. The API exposes Actuator with a
**health-only public** surface (`management.endpoints.web.exposure.include=health`);
all other operational endpoints require `ADMIN`
([ADR-0004](0004-spring-security-session-auth.md)). The Micrometer Prometheus
registry is enabled (`micrometer-registry-prometheus`), so a Prometheus scrape
or a full observability stack is a *configuration* step later, not a rewrite.

The full OTel/Prometheus/Grafana/Loki/Jaeger stack is deferred; the
`grafana/`, `loki/`, `prometheus/`, and `otel-config.yml` assets in the repo are
reference inputs, not a supported stack
([ADR-0007](0007-no-supported-deployment.md)).

### Consequences

* Good — no stateful monitoring services to run locally; one fewer thing to break.
* Good — Micrometer keeps a Prometheus-compatible endpoint, so attaching a stack
  later is cheap.
* Bad — no dashboards or metric history out of the box; trend diagnostics are
  manual until a stack is attached. Accepted at template scope.

## More Information

Source: `application.properties` (Actuator health-only, Prometheus registry
enabled), `api/pom.xml` (`micrometer-registry-prometheus`), and the ADMIN gate
on `/actuator/**` in `SecurityConfig`. Deferred phases in
[`docs/UPGRADE_PLAN.md`](../UPGRADE_PLAN.md).
