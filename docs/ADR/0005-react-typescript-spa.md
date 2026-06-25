---
status: accepted
date: 2026-06-25
deciders: Foundation engineering + CTO
---
# Frontend as a React + TypeScript SPA

## Context and Problem Statement

The template's frontend framework sets the default for downstream projects.
Foundation's existing portfolio and developer skill base are concentrated in
React, and projects are often staffed with juniors under mentoring. The choice
also has to fit the company stack (TypeScript, ECMA2018+, multi-browser).

## Decision Drivers

* Existing team competence and portfolio (faster onboarding, easier staffing).
* Alignment with the company frontend stack.
* Maturity and stability of the React ecosystem (router, forms, state).

## Considered Options

* **React + TypeScript SPA.**
* **Vue 3 + TypeScript** (more batteries-included, arguably gentler for juniors).

## Decision Outcome

Chosen option: **React + TypeScript SPA**, served separately in dev (webpack dev
server `:8080`) and consuming the API over REST
([ADR-0006](0006-rest-openapi-generated-client.md)). Concretely:

* **React 19.2** with the classic JSX transform, **TypeScript strict mode**.
* **React Router 7** with `RequireAuth`-guarded routes; auth state restored from
  the live backend session via `AuthProvider`.
* **Formik + Yup** forms, **Bootstrap 5 + SCSS (BEM)** styling, **i18next** (SK).
* **Zustand-style local state** and React context; no heavyweight global store
  at this size.
* Path aliases (`@components`, `@api`, `@pages`, `@common`), single quotes, no
  semicolons; ESLint + `tsc --noEmit` as the gate.

Code quality is treated as a function of mentoring and review, not of framework
choice — which neutralises Vue's main advantage for junior-heavy teams.

### Consequences

* Good — leverages existing competence; faster ramp-up and easier staffing.
* Good — shareable internal components and conventions across Foundation projects.
* Bad — React's ecosystem is assembled from several libraries (router, forms,
  state) rather than one framework; accepted as a one-time setup cost the
  template pays once on behalf of downstream projects.

## More Information

Source: `ui/src` (entry `index.tsx`, routes `components/router/routes.tsx`).
