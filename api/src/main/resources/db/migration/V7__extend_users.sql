-- ============================================================================
-- V7 — Extend USERS for the registry role model (one credential store).
--
-- USERS absorbs registry.staff_user (documented deviation from db/schema.sql):
--   OFFICE_CODE  clerk office scope (RLS office scoping is now app-layer, read
--                from this column by the service layer).
--   PERSON_UID   citizen-portal identity (the person this login represents).
-- Both nullable: staff logins have an office (or none = central authority),
-- citizen logins have a person uid; other rows leave both unset.
-- ============================================================================

alter table USERS add column OFFICE_CODE varchar(32);
alter table USERS add column PERSON_UID varchar(32);

alter table USERS add constraint FK_USERS_OFFICE
	foreign key (OFFICE_CODE) references CODELIST.REGISTRY_OFFICE(OFFICE_CODE);
alter table USERS add constraint FK_USERS_PERSON
	foreign key (PERSON_UID) references REGISTRY.PERSON(PERSON_UID);
