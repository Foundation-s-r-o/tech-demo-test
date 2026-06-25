# tech-demo UI — React 19.x Upgrade Plan

> Status: **EXECUTED 2026-05-27.** React core 19.0.0 → **19.2.6**, react-helmet removed.
> Verified: lint clean, production build OK, app smoke (1) + storybook smoke (2) pass, Trivy HIGH/CRITICAL = 0.
> Scope: `ui/` only. Backend untouched.

## TL;DR

tech-demo's UI is **already on React 19** (`react@19.0.0`, `react-dom@19.0.0`, `@types/react@19`),
already uses `createRoot`, the new JSX transform, functional components only, and **React Router v7**.
So this is **not an 18→19 migration** — it is a *consolidation to the latest 19.2.x* plus clearing the
**one library without a React 19 peer** (`react-helmet`). Effort is **S–M (~0.5–1 day)**, not a full 18→19 migration's L (~2–3d).

## Current state (verified 2026-05-27)

| Package | Installed | React 19 ready? | Action |
|---|---|---|---|
| `react` / `react-dom` / `react-is` | 19.0.0 | yes (on 19) | bump to latest **19.2.x** |
| `@types/react` / `@types/react-dom` | 19.0.x | yes | bump to 19.2.x |
| `react-router-dom` | 7.12.0 | yes (v7) | none |
| `react-bootstrap` | 2.10.9 | yes (peer ≥16.14) | none (optionally latest 2.10.x) |
| `react-select` | 5.10.1 | yes (peer incl. ^19) | none |
| `react-i18next` / `i18next` | 15.4.1 / 24.2.2 | yes (peer ≥16.8) | none required |
| `react-datepicker` | ^8.1.0 | yes (v8 supports 19) | verify peer; none expected |
| `formik` / `yup` | 2.4.6 / 1.6.1 | yes | none |
| `styled-components` | 6.1.15 | yes | none |
| **`react-helmet`** + `@types/react-helmet` | 6.1.0 | **NO — unmaintained, no React 19 peer** | **remove → React 19 native `<title>`** |

**Already satisfied (no work — the standard React 19 "low risk" list):**
`createRoot` in `index.tsx`; functional components only; tests are Playwright
(no `react-test-renderer`/`react-dom/test-utils`).
Note: this repo uses the **classic JSX transform** (`tsconfig` `jsx: "react"`; every component
imports React). React 19 does NOT require the automatic transform — classic still works — so we keep
it (migrating the whole tree's transform would be out-of-scope churn). Components must keep `import React`.

**The one real item:** `react-helmet@6.1.0` is used in a single file —
`src/components/shared/PageInfo.tsx` (`<Helmet><title>…</title></Helmet>`).

---

## Plan

### Step 1 — Pre-scan & codemods (before bumping)  [S]
Already-React-19 code is unlikely to trip these, but confirm:
- Grep & fix if present: `defaultProps` on function components → default params; `propTypes`;
  `useRef()` with no arg → `useRef<T>(null)`; ref callbacks with implicit returns → block body;
  any `ReactDOM.render/hydrate/unmountComponentAtNode/findDOMNode`; global `JSX` namespace
  augmentation under `src/@types` → `declare module 'react'`.
- Run `npx types-react-codemod@latest preset-19 ./src` (and the React 19 migration recipe if it flags anything).
- Expected: near-zero changes (repo is already on 19.0).

### Step 2 — Remove `react-helmet`, use React 19 document metadata  [S, the main item]
React 19 hoists `<title>`/`<meta>`/`<link>` rendered anywhere in the tree, so the wrapper is no longer needed.
- Rewrite `PageInfo.tsx` to render `<title>{…}</title>` (and any `<meta>`) directly — drop the `<Helmet>` wrapper.
- Remove deps: `react-helmet`, `@types/react-helmet`. Drop the import.
- Alternative if richer head management is later needed: `react-helmet-async` + `HelmetProvider`. Not needed now (single title use) — prefer the native path (one fewer dependency).

### Step 3 — Bump React core to latest 19.2.x  [S]
- `react@^19.2`, `react-dom@^19.2`, `react-is@^19.2`, `@types/react@^19.2`, `@types/react-dom@^19.2`.
- `npm install` against the **public registry** (the global `~/.npmrc` points at the unreachable TFS feed —
  use `--registry=https://registry.npmjs.org/`; do not edit `.npmrc`).
- Let `tsc` surface ref-cleanup / `useRef` typing fallout (rare here); fix as needed.

### Step 4 — Verify peers (no expected changes)  [S]
- Confirm `react-datepicker@8` resolves a React 19 peer (it does on v8); only bump if `npm` warns.
- Optional, not required by React 19: bump `react-i18next` 15→latest, `react-bootstrap` to latest 2.10.x.
  Defer unless a warning appears — YAGNI.

### Step 5 — Verify gate  [S]
```
cd ui
npm run lint                 # tsc --noEmit + eslint — must pass
npm run build                # production webpack build must succeed
npm run smoke                # app smoke (login shell, console-error gate)
npm run smoke:storybook      # Button/Form stories render clean
trivy fs . --scanners vuln --severity HIGH,CRITICAL   # peer bumps change the tree
```
- Manual click-through: page `<title>` still updates per route (the react-helmet replacement).
- Fix every error; flag warnings.

---

## Risks
- **`react-helmet` removal** is the only behavioral change — page-title updates must still work; the app smoke
  + a manual route check are the safety net. Blast radius is tiny (one component).
- React 19.0 → 19.2 is a patch/minor within the same major — low risk; `tsc` catches the rare typing fallout.
- Storybook 9 + the smoke suite already exist here, so the regression
  net is in place from the start.

## Execution order
`Step 1 (codemod scan) → Step 2 (drop react-helmet) → Step 3 (bump 19.2.x) → Step 4 (peers) → Step 5 (verify)`.
Single sitting; each step followed by `npm run lint` before the next.

## Notes
- Build requires the public npm registry (TFS feed unreachable) — see Step 3.
- This plan deliberately omits items that don't apply here (react-router v7 move — already done;
  react-helmet-async — unnecessary for a single title; major datepicker/bootstrap bumps — already 19-ready).
