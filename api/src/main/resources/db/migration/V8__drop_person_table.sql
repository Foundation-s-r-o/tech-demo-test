-- ============================================================================
-- V8 - Drop the demo PERSON table.
--
-- The demo Person feature (flat PERSON CRUD) is superseded by the civil-registry
-- person aggregate (REGISTRY.PERSON + bitemporal satellites). Its table is
-- removed here; the registry person API owns /api/persons from now on. Demo
-- PERSON rows in any local ./data database are disposable (user-approved).
-- ============================================================================

drop table if exists PERSON;
