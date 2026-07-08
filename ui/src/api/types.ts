import { PersonSummaryDTO } from '@api/generated'

// A person search row for FndtTable, keyed by the person UID.
export type UiPersonSummary = PersonSummaryDTO & { id: string }
