declare module '*.svg' {
    const content: string
    export default content
}

// Style side-effect imports (import './x.scss'). Handled by webpack loaders at build
// time; this ambient decl satisfies TS6's noUncheckedSideEffectImports (default true).
declare module '*.scss'
declare module '*.css'

// dotenv ships types but its package.json "exports" doesn't expose them under
// moduleResolution: "bundler" (TS6). Shim the single function the tests use.
declare module 'dotenv' {
    export function config(options?: { path?: string }): void
}
