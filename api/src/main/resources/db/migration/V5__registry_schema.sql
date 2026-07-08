-- ============================================================================
-- V5 — Civil-registry schema (ported from db/schema.sql).
--
-- Portable ANSI SQL for embedded H2 (file-mode) with a documented path back to
-- PostgreSQL. Natural primary keys as designed (person_uid, codelist codes,
-- varchar/uuid FKs). PostgreSQL-only mechanisms (RLS, roles/grants, audit &
-- delete-protection triggers, pgcrypto hash chain, GiST EXCLUDE bitemporal
-- no-overlap, partial unique indexes, generated columns, table partitioning)
-- not expressible in portable H2 DDL; each is annotated below with a
-- `-- PG-delta:` marker and re-implemented in the application layer (Spring
-- Security, BitemporalGuard, AuditService, PersonMergeService, ...). The full
-- list to re-add on the PostgreSQL switch is the "PostgreSQL re-hardening
-- delta" appendix in db/DESIGN.md.
--
-- Type conversions vs schema.sql: text -> varchar; timestamptz -> timestamp;
-- uuid -> varchar(36) (app-generated); inet -> varchar(45); jsonb -> clob;
-- text[] -> CSV varchar; interval -> integer years; bytea -> varchar (base64,
-- crypto done in NationalIdCryptoService). Codelist codes and UIDs are
-- varchar(32); actor columns are varchar(255) to match USERS.USERNAME.
--
-- staff_user is absorbed into the existing USERS table (one credential store);
-- actor columns reference USERS(USERNAME). USERS gains OFFICE_CODE / PERSON_UID
-- in V7 (deferred so those FKs land after this schema exists).
-- ============================================================================

create schema if not exists CODELIST;
create schema if not exists REGISTRY;
create schema if not exists GDPR;
create schema if not exists AUDIT;

-- ----------------------------------------------------------------------------
-- 1. Codelists (reference data; MetaIS provenance columns on every table).
--    Identity tables: PK = official MetaIS item code. Semantic tables
--    (sex, address_type, religion): internal stable keys baked into app logic.
-- ----------------------------------------------------------------------------
create table CODELIST.COUNTRY (
	COUNTRY_CODE    varchar(32) primary key,
	NAME            varchar(255) not null,
	SOURCE_CODELIST varchar(32),
	METAIS_CODE     varchar(32),
	ITEM_VALID_FROM date,
	ITEM_VALID_TO   date
);

create table CODELIST.REGION (
	REGION_CODE     varchar(32) primary key,
	NAME            varchar(255) not null,
	SOURCE_CODELIST varchar(32),
	METAIS_CODE     varchar(32),
	ITEM_VALID_FROM date,
	ITEM_VALID_TO   date
);

create table CODELIST.DISTRICT (
	DISTRICT_CODE   varchar(32) primary key,
	NAME            varchar(255) not null,
	REGION_CODE     varchar(32) references CODELIST.REGION,
	SOURCE_CODELIST varchar(32),
	METAIS_CODE     varchar(32),
	ITEM_VALID_FROM date,
	ITEM_VALID_TO   date
);

create table CODELIST.MUNICIPALITY (
	MUNICIPALITY_CODE varchar(32) primary key,
	NAME              varchar(255) not null,
	DISTRICT_CODE     varchar(32) references CODELIST.DISTRICT,
	COUNTRY_CODE      varchar(32) references CODELIST.COUNTRY,
	SOURCE_CODELIST   varchar(32),
	METAIS_CODE       varchar(32),
	ITEM_VALID_FROM   date,
	ITEM_VALID_TO     date
);

create table CODELIST.MUNICIPALITY_PART (
	PART_CODE         varchar(32) primary key,
	NAME              varchar(255) not null,
	MUNICIPALITY_CODE varchar(32) references CODELIST.MUNICIPALITY,
	SOURCE_CODELIST   varchar(32),
	METAIS_CODE       varchar(32),
	ITEM_VALID_FROM   date,
	ITEM_VALID_TO     date
);

create table CODELIST.ADDRESS_TYPE (
	ADDRESS_TYPE_CODE varchar(32) primary key,
	NAME              varchar(255) not null,
	SOURCE_CODELIST   varchar(32),
	METAIS_CODE       varchar(32),
	ITEM_VALID_FROM   date,
	ITEM_VALID_TO     date
);
insert into CODELIST.ADDRESS_TYPE (ADDRESS_TYPE_CODE, NAME) values
	('permanent', 'Trvaly pobyt'), ('temporary', 'Prechodny pobyt'),
	('foreign', 'Pobyt v zahranici'), ('official', 'Uradna adresa');

create table CODELIST.SEX (
	SEX_CODE        varchar(32) primary key,
	NAME            varchar(255) not null,
	SOURCE_CODELIST varchar(32),
	METAIS_CODE     varchar(32),
	ITEM_VALID_FROM date,
	ITEM_VALID_TO   date
);
insert into CODELIST.SEX (SEX_CODE, NAME, SOURCE_CODELIST, METAIS_CODE) values
	('M', 'muz', 'CL003003', '1'), ('F', 'zena', 'CL003003', '2'),
	('X', 'nezistene', 'CL003003', '0');

create table CODELIST.MARITAL_STATUS (
	MARITAL_STATUS_CODE varchar(32) primary key,
	NAME            varchar(255) not null,
	SOURCE_CODELIST varchar(32),
	METAIS_CODE     varchar(32),
	ITEM_VALID_FROM date,
	ITEM_VALID_TO   date
);

create table CODELIST.EXISTENCE_STATUS (
	EXISTENCE_STATUS_CODE varchar(32) primary key,
	NAME            varchar(255) not null,
	SOURCE_CODELIST varchar(32),
	METAIS_CODE     varchar(32),
	ITEM_VALID_FROM date,
	ITEM_VALID_TO   date
);

-- Composite PK: title codes overlap between CL000062 (before) and CL000063 (after).
create table CODELIST.ACADEMIC_TITLE (
	TITLE_CODE      varchar(32) not null,
	NAME            varchar(255) not null,
	TITLE_POSITION  varchar(32) not null check (TITLE_POSITION in ('before', 'after')),
	SOURCE_CODELIST varchar(32),
	METAIS_CODE     varchar(32),
	ITEM_VALID_FROM date,
	ITEM_VALID_TO   date,
	primary key (TITLE_CODE, TITLE_POSITION)
);

create table CODELIST.COURT (
	COURT_CODE      varchar(32) primary key,
	NAME            varchar(255) not null,
	SOURCE_CODELIST varchar(32),
	METAIS_CODE     varchar(32),
	ITEM_VALID_FROM date,
	ITEM_VALID_TO   date
);

create table CODELIST.FAMILY_RELATION_TYPE (
	RELATION_CODE   varchar(32) primary key,
	NAME            varchar(255) not null,
	SOURCE_CODELIST varchar(32),
	METAIS_CODE     varchar(32),
	ITEM_VALID_FROM date,
	ITEM_VALID_TO   date
);

-- Not in MetaIS — own list based on the SU SR statistical codelist.
create table CODELIST.RELIGION (
	RELIGION_CODE   varchar(32) primary key,
	NAME            varchar(255) not null,
	SOURCE_CODELIST varchar(32),
	METAIS_CODE     varchar(32),
	ITEM_VALID_FROM date,
	ITEM_VALID_TO   date
);
insert into CODELIST.RELIGION (RELIGION_CODE, NAME) values
	('RIMKAT',      'Rimskokatolicka cirkev'),
	('GRKAT',       'Greckokatolicka cirkev'),
	('PRAVOSL',     'Pravoslavna cirkev'),
	('ECAV',        'Evanjelicka cirkev augsburskeho vyznania'),
	('REFORM',      'Reformovana krestanska cirkev'),
	('ZIDOV',       'Ustredny zvaz zidovskych nabozenskych obci'),
	('SVEDKOVIA',   'Nabozenska spolocnost Jehovovi svedkovia'),
	('OTHER',       'Ine vyznanie'),
	('NONE',        'Bez vyznania'),
	('UNDISCLOSED', 'Neuvedene');

create table CODELIST.REGISTRY_OFFICE (
	OFFICE_CODE       varchar(32) primary key,
	NAME              varchar(255) not null,
	MUNICIPALITY_CODE varchar(32) references CODELIST.MUNICIPALITY,
	SOURCE_CODELIST   varchar(32),
	METAIS_CODE       varchar(32),
	ITEM_VALID_FROM   date,
	ITEM_VALID_TO     date
);

-- ----------------------------------------------------------------------------
-- 2. Person — immutable identity anchor. Mutable data lives in the bitemporal
--    satellites below.
-- PG-delta: person_uid CHECK (registry.person_uid_is_valid) and the
--   generate_person_uid() default are dropped; PersonUidService generates and
--   validates the UID (Crockford base32 + mod-32 check char) in the app.
-- PG-delta: national_id ciphertext/HMAC stored as base64 varchar; encryption
--   (AES-GCM) + keyed HMAC done by NationalIdCryptoService (bytea in PG).
-- ----------------------------------------------------------------------------
create table REGISTRY.PERSON (
	PERSON_UID       varchar(32) primary key,
	NATIONAL_ID_ENC  varchar(255),
	NATIONAL_ID_HMAC varchar(255) unique,
	ANONYMIZED       boolean not null default false,
	CREATED_AT       timestamp not null default current_timestamp,
	CREATED_BY       varchar(255) not null references PUBLIC.USERS(USERNAME)
);

-- 2.1 Identity details (bitemporal): names, sex, birth data, citizenship.
-- PG-delta: EXCLUDE USING gist (one current-belief version per person per point
--   in valid time) -> BitemporalGuard overlap check inside the write txn.
-- PG-delta: titles_before/after text[] -> CSV varchar (app splits/joins).
-- PG-delta: partial index (only rows where superseded_at is unset) -> plain index.
create table REGISTRY.PERSON_DETAIL (
	DETAIL_ID       bigint generated by default as identity primary key,
	PERSON_UID      varchar(32) not null references REGISTRY.PERSON,
	GIVEN_NAMES     varchar(255) not null,
	FAMILY_NAME     varchar(255) not null,
	BIRTH_SURNAME   varchar(255),
	SEX             varchar(32) not null references CODELIST.SEX,
	DATE_OF_BIRTH   date not null,
	DATE_OF_BIRTH_PRECISION varchar(32) not null default 'day'
		check (DATE_OF_BIRTH_PRECISION in ('day', 'month', 'year')),
	PLACE_OF_BIRTH_MUNICIPALITY varchar(32) references CODELIST.MUNICIPALITY,
	PLACE_OF_BIRTH_FOREIGN      varchar(255),
	CITIZENSHIP     varchar(32) not null references CODELIST.COUNTRY,
	MARITAL_STATUS_CODE   varchar(32) references CODELIST.MARITAL_STATUS,
	EXISTENCE_STATUS_CODE varchar(32) references CODELIST.EXISTENCE_STATUS,
	TITLES_BEFORE   varchar(255),
	TITLES_AFTER    varchar(255),
	VALID_FROM      date not null,
	VALID_TO        date,
	RECORDED_AT     timestamp not null default current_timestamp,
	RECORDED_BY     varchar(255) not null references PUBLIC.USERS(USERNAME),
	SUPERSEDED_AT   timestamp,
	SUPERSEDED_BY   bigint references REGISTRY.PERSON_DETAIL(DETAIL_ID),
	CHANGE_REASON   varchar(4000),
	check (VALID_TO is null or VALID_TO > VALID_FROM)
);
create index IDX_PERSON_DETAIL_CURRENT on REGISTRY.PERSON_DETAIL (PERSON_UID);
create index IDX_PERSON_DETAIL_NAME on REGISTRY.PERSON_DETAIL (FAMILY_NAME, GIVEN_NAMES);

-- 2.2 Address (normalized) + bitemporal person link.
-- PG-delta: address_id uuid -> varchar(36) (app-generated UUID).
create table REGISTRY.ADDRESS (
	ADDRESS_ID        varchar(36) primary key,
	STREET            varchar(255),
	HOUSE_NUMBER      varchar(255),
	POSTAL_CODE       varchar(255),
	MUNICIPALITY_CODE varchar(32) references CODELIST.MUNICIPALITY,
	MUNICIPALITY_PART_CODE varchar(32) references CODELIST.MUNICIPALITY_PART,
	COUNTRY_CODE      varchar(32) not null references CODELIST.COUNTRY,
	FOREIGN_ADDRESS   varchar(4000),
	check (MUNICIPALITY_CODE is not null or FOREIGN_ADDRESS is not null)
);

-- PG-delta: EXCLUDE USING gist (one current address per person+type per point
--   in valid time) -> BitemporalGuard.
create table REGISTRY.PERSON_ADDRESS (
	PERSON_ADDRESS_ID bigint generated by default as identity primary key,
	PERSON_UID        varchar(32) not null references REGISTRY.PERSON,
	ADDRESS_ID        varchar(36) not null references REGISTRY.ADDRESS,
	ADDRESS_TYPE      varchar(32) not null references CODELIST.ADDRESS_TYPE,
	VALID_FROM        date not null,
	VALID_TO          date,
	RECORDED_AT       timestamp not null default current_timestamp,
	RECORDED_BY       varchar(255) not null references PUBLIC.USERS(USERNAME),
	SUPERSEDED_AT     timestamp,
	SUPERSEDED_BY     bigint references REGISTRY.PERSON_ADDRESS(PERSON_ADDRESS_ID),
	CHANGE_REASON     varchar(4000),
	check (VALID_TO is null or VALID_TO > VALID_FROM)
);
create index IDX_PERSON_ADDRESS_CURRENT on REGISTRY.PERSON_ADDRESS (PERSON_UID);

-- 2.3 Religion — GDPR Art. 9 special category. Every row carries an Art. 9(2)
--     legal basis; consent-based rows reference GDPR.CONSENT (FK added below).
-- PG-delta: EXCLUDE USING gist -> BitemporalGuard.
create table REGISTRY.PERSON_RELIGION (
	PERSON_RELIGION_ID bigint generated by default as identity primary key,
	PERSON_UID         varchar(32) not null references REGISTRY.PERSON,
	RELIGION_CODE      varchar(32) not null references CODELIST.RELIGION,
	LEGAL_BASIS        varchar(32) not null check (LEGAL_BASIS in
		('art9_2a_consent', 'art9_2g_public_interest', 'art9_2j_archiving')),
	CONSENT_ID         bigint,
	VALID_FROM         date not null,
	VALID_TO           date,
	RECORDED_AT        timestamp not null default current_timestamp,
	RECORDED_BY        varchar(255) not null references PUBLIC.USERS(USERNAME),
	SUPERSEDED_AT      timestamp,
	SUPERSEDED_BY      bigint references REGISTRY.PERSON_RELIGION(PERSON_RELIGION_ID),
	CHANGE_REASON      varchar(4000),
	check (VALID_TO is null or VALID_TO > VALID_FROM)
);
create index IDX_PERSON_RELIGION_CURRENT on REGISTRY.PERSON_RELIGION (PERSON_UID);

-- 2.4 Court-ordered legal-capacity restrictions (drives portal eligibility).
create table REGISTRY.LEGAL_CAPACITY_RESTRICTION (
	RESTRICTION_ID   bigint generated by default as identity primary key,
	PERSON_UID       varchar(32) not null references REGISTRY.PERSON,
	RESTRICTION_TYPE varchar(32) not null check (RESTRICTION_TYPE in
		('full_deprivation', 'partial_limitation')),
	SCOPE            varchar(4000),
	COURT_CODE       varchar(32) references CODELIST.COURT,
	COURT_REFERENCE  varchar(255),
	VALID_FROM       date not null,
	VALID_TO         date,
	RECORDED_AT      timestamp not null default current_timestamp,
	RECORDED_BY      varchar(255) not null references PUBLIC.USERS(USERNAME),
	SUPERSEDED_AT    timestamp,
	SUPERSEDED_BY    bigint references REGISTRY.LEGAL_CAPACITY_RESTRICTION(RESTRICTION_ID),
	CHANGE_REASON    varchar(4000),
	check (VALID_TO is null or VALID_TO > VALID_FROM)
);
create index IDX_CAPACITY_CURRENT on REGISTRY.LEGAL_CAPACITY_RESTRICTION (PERSON_UID);

-- 2.5 Representation (parent / court-appointed guardian).
-- PG-delta: EXCLUDE USING gist -> BitemporalGuard.
create table REGISTRY.PERSON_REPRESENTATION (
	REPRESENTATION_ID   bigint generated by default as identity primary key,
	REPRESENTED_UID     varchar(32) not null references REGISTRY.PERSON,
	REPRESENTATIVE_UID  varchar(32) not null references REGISTRY.PERSON,
	REPRESENTATION_TYPE varchar(32) not null check (REPRESENTATION_TYPE in ('parent', 'guardian')),
	LEGAL_BASIS_REF     varchar(255),
	COURT_CODE          varchar(32) references CODELIST.COURT,
	VALID_FROM          date not null,
	VALID_TO            date,
	RECORDED_AT         timestamp not null default current_timestamp,
	RECORDED_BY         varchar(255) not null references PUBLIC.USERS(USERNAME),
	SUPERSEDED_AT       timestamp,
	SUPERSEDED_BY       bigint references REGISTRY.PERSON_REPRESENTATION(REPRESENTATION_ID),
	CHANGE_REASON       varchar(4000),
	check (REPRESENTED_UID <> REPRESENTATIVE_UID),
	check (VALID_TO is null or VALID_TO > VALID_FROM)
);
create index IDX_REPRESENTATION_REPRESENTATIVE on REGISTRY.PERSON_REPRESENTATION (REPRESENTATIVE_UID);

-- 2.6 Person identity merge (duplicate -> primary), reversible via supersede.
-- PG-delta: partial unique (one active merge per duplicate) -> PersonMergeService
--   uniqueness check; person_merge_guard trigger (no chains) -> PersonMergeService.
create table REGISTRY.PERSON_MERGE (
	MERGE_ID        bigint generated by default as identity primary key,
	DUPLICATE_UID   varchar(32) not null references REGISTRY.PERSON,
	PRIMARY_UID     varchar(32) not null references REGISTRY.PERSON,
	REASON          varchar(4000) not null,
	MERGED_AT       timestamp not null default current_timestamp,
	DECIDED_BY      varchar(255) not null references PUBLIC.USERS(USERNAME),
	APPROVED_BY     varchar(255) references PUBLIC.USERS(USERNAME),
	SUPERSEDED_AT   timestamp,
	REVERSAL_REASON varchar(4000),
	check (DUPLICATE_UID <> PRIMARY_UID),
	check (SUPERSEDED_AT is null or REVERSAL_REASON is not null)
);
create index IDX_PERSON_MERGE_ACTIVE on REGISTRY.PERSON_MERGE (DUPLICATE_UID);

-- ----------------------------------------------------------------------------
-- 3. Vital records — append-only, transaction-time versioned.
-- PG-delta: partial unique registry-number index (office, year, entry among
--   current versions) -> BitemporalGuard uniqueness check; the plain index
--   below only serves lookups.
-- ----------------------------------------------------------------------------
create table REGISTRY.BIRTH_RECORD (
	RECORD_ID           varchar(36) primary key,
	OFFICE_CODE         varchar(32) not null references CODELIST.REGISTRY_OFFICE,
	BOOK_YEAR           integer not null,
	ENTRY_NO            integer not null,
	CHILD_UID           varchar(32) not null references REGISTRY.PERSON,
	DATE_OF_BIRTH       date not null,
	TIME_OF_BIRTH       time,
	PLACE_MUNICIPALITY  varchar(32) references CODELIST.MUNICIPALITY,
	PLACE_FOREIGN       varchar(255),
	FACILITY            varchar(255),
	MULTIPLE_BIRTH_ORDER integer,
	MOTHER_UID          varchar(32) references REGISTRY.PERSON,
	FATHER_UID          varchar(32) references REGISTRY.PERSON,
	RECORDED_AT         timestamp not null default current_timestamp,
	RECORDED_BY         varchar(255) not null references PUBLIC.USERS(USERNAME),
	SUPERSEDED_AT       timestamp,
	SUPERSEDES_ID       varchar(36) references REGISTRY.BIRTH_RECORD(RECORD_ID),
	AMENDMENT_REASON    varchar(4000),
	APPROVED_BY         varchar(255) references PUBLIC.USERS(USERNAME),
	check (SUPERSEDES_ID is null or AMENDMENT_REASON is not null)
);
create index IDX_BIRTH_REGISTRY_NO on REGISTRY.BIRTH_RECORD (OFFICE_CODE, BOOK_YEAR, ENTRY_NO);
create index IDX_BIRTH_CHILD on REGISTRY.BIRTH_RECORD (CHILD_UID);

create table REGISTRY.MARRIAGE_RECORD (
	RECORD_ID           varchar(36) primary key,
	OFFICE_CODE         varchar(32) not null references CODELIST.REGISTRY_OFFICE,
	BOOK_YEAR           integer not null,
	ENTRY_NO            integer not null,
	SPOUSE1_UID         varchar(32) not null references REGISTRY.PERSON,
	SPOUSE2_UID         varchar(32) not null references REGISTRY.PERSON,
	MARRIAGE_DATE       date not null,
	PLACE_MUNICIPALITY  varchar(32) references CODELIST.MUNICIPALITY,
	PLACE_FOREIGN       varchar(255),
	MARRIAGE_FORM       varchar(32) not null check (MARRIAGE_FORM in ('civil', 'religious')),
	OFFICIANT           varchar(255),
	WITNESS1_NAME       varchar(255),
	WITNESS2_NAME       varchar(255),
	SURNAME_AGREEMENT   varchar(4000),
	RECORDED_AT         timestamp not null default current_timestamp,
	RECORDED_BY         varchar(255) not null references PUBLIC.USERS(USERNAME),
	SUPERSEDED_AT       timestamp,
	SUPERSEDES_ID       varchar(36) references REGISTRY.MARRIAGE_RECORD(RECORD_ID),
	AMENDMENT_REASON    varchar(4000),
	APPROVED_BY         varchar(255) references PUBLIC.USERS(USERNAME),
	check (SPOUSE1_UID <> SPOUSE2_UID),
	check (SUPERSEDES_ID is null or AMENDMENT_REASON is not null)
);
create index IDX_MARRIAGE_REGISTRY_NO on REGISTRY.MARRIAGE_RECORD (OFFICE_CODE, BOOK_YEAR, ENTRY_NO);
create index IDX_MARRIAGE_SPOUSE1 on REGISTRY.MARRIAGE_RECORD (SPOUSE1_UID);
create index IDX_MARRIAGE_SPOUSE2 on REGISTRY.MARRIAGE_RECORD (SPOUSE2_UID);

create table REGISTRY.MARRIAGE_DISSOLUTION (
	DISSOLUTION_ID    varchar(36) primary key,
	MARRIAGE_ID       varchar(36) not null references REGISTRY.MARRIAGE_RECORD,
	DISSOLUTION_TYPE  varchar(32) not null check (DISSOLUTION_TYPE in
		('divorce', 'annulment', 'spouse_death')),
	EFFECTIVE_DATE    date not null,
	COURT_CODE        varchar(32) references CODELIST.COURT,
	COURT_REFERENCE   varchar(255),
	RECORDED_AT       timestamp not null default current_timestamp,
	RECORDED_BY       varchar(255) not null references PUBLIC.USERS(USERNAME),
	SUPERSEDED_AT     timestamp,
	SUPERSEDES_ID     varchar(36) references REGISTRY.MARRIAGE_DISSOLUTION(DISSOLUTION_ID),
	AMENDMENT_REASON  varchar(4000)
);

create table REGISTRY.DEATH_RECORD (
	RECORD_ID           varchar(36) primary key,
	OFFICE_CODE         varchar(32) not null references CODELIST.REGISTRY_OFFICE,
	BOOK_YEAR           integer not null,
	ENTRY_NO            integer not null,
	PERSON_UID          varchar(32) not null references REGISTRY.PERSON,
	DATE_OF_DEATH       date,
	DATE_FOUND          date,
	DECLARED_BY_COURT   boolean not null default false,
	COURT_CODE          varchar(32) references CODELIST.COURT,
	COURT_REFERENCE     varchar(255),
	PLACE_MUNICIPALITY  varchar(32) references CODELIST.MUNICIPALITY,
	PLACE_FOREIGN       varchar(255),
	RECORDED_AT         timestamp not null default current_timestamp,
	RECORDED_BY         varchar(255) not null references PUBLIC.USERS(USERNAME),
	SUPERSEDED_AT       timestamp,
	SUPERSEDES_ID       varchar(36) references REGISTRY.DEATH_RECORD(RECORD_ID),
	AMENDMENT_REASON    varchar(4000),
	APPROVED_BY         varchar(255) references PUBLIC.USERS(USERNAME),
	check (DATE_OF_DEATH is not null or DATE_FOUND is not null or DECLARED_BY_COURT),
	check (SUPERSEDES_ID is null or AMENDMENT_REASON is not null)
);
create index IDX_DEATH_REGISTRY_NO on REGISTRY.DEATH_RECORD (OFFICE_CODE, BOOK_YEAR, ENTRY_NO);
create index IDX_DEATH_PERSON on REGISTRY.DEATH_RECORD (PERSON_UID);

-- Cause of death = health data (GDPR Art. 9); isolated for stricter grants.
create table REGISTRY.DEATH_MEDICAL (
	DEATH_RECORD_ID  varchar(36) primary key references REGISTRY.DEATH_RECORD,
	CAUSE_OF_DEATH_ICD10 varchar(255),
	AUTOPSY_PERFORMED    boolean,
	CERTIFIED_BY         varchar(255),
	RECORDED_AT          timestamp not null default current_timestamp,
	RECORDED_BY          varchar(255) not null references PUBLIC.USERS(USERNAME)
);

-- ----------------------------------------------------------------------------
-- 4. GDPR schema
-- ----------------------------------------------------------------------------
create table GDPR.CONSENT (
	CONSENT_ID    bigint generated by default as identity primary key,
	PERSON_UID    varchar(32) not null references REGISTRY.PERSON,
	PURPOSE       varchar(255) not null,
	GIVEN_AT      timestamp not null,
	GIVEN_VIA     varchar(255) not null,
	WITHDRAWN_AT  timestamp,
	RECORDED_BY   varchar(255) not null references PUBLIC.USERS(USERNAME)
);

-- Deferred religion->consent FK + Art. 9(2)(a) consent-required CHECK.
alter table REGISTRY.PERSON_RELIGION
	add constraint FK_RELIGION_CONSENT foreign key (CONSENT_ID) references GDPR.CONSENT(CONSENT_ID);
alter table REGISTRY.PERSON_RELIGION
	add constraint RELIGION_CONSENT_REQUIRED
	check (LEGAL_BASIS <> 'art9_2a_consent' or CONSENT_ID is not null);

-- Data subject requests (Art. 15-21).
-- PG-delta: due_at generated stored column (received_at + 1 month) ->
--   DataSubjectRequestService computes the statutory due date on create.
create table GDPR.DATA_SUBJECT_REQUEST (
	REQUEST_ID    bigint generated by default as identity primary key,
	PERSON_UID    varchar(32) references REGISTRY.PERSON,
	REQUESTER_REF varchar(255) not null,
	REQUEST_TYPE  varchar(32) not null check (REQUEST_TYPE in
		('access', 'rectification', 'erasure', 'restriction', 'objection', 'portability')),
	RECEIVED_AT   date not null default current_date,
	DUE_AT        date not null,
	STATUS        varchar(32) not null default 'received' check (STATUS in
		('received', 'identity_check', 'in_progress', 'extended', 'fulfilled', 'refused')),
	HANDLED_BY    varchar(255) references PUBLIC.USERS(USERNAME),
	RESOLVED_AT   date,
	RESOLUTION    varchar(4000)
);

-- Art. 30 disclosure register.
-- PG-delta: disclosed_fields text[] -> CSV varchar.
create table GDPR.DISCLOSURE_REGISTER (
	DISCLOSURE_ID   bigint generated by default as identity primary key,
	PERSON_UID      varchar(32) not null references REGISTRY.PERSON,
	RECIPIENT       varchar(255) not null,
	RECIPIENT_TYPE  varchar(32) not null check (RECIPIENT_TYPE in
		('state_agency', 'court', 'municipality', 'data_subject', 'other')),
	LEGAL_BASIS     varchar(255) not null,
	DISCLOSED_FIELDS varchar(1000) not null,
	DISCLOSED_AT    timestamp not null default current_timestamp,
	DISCLOSED_BY    varchar(255) not null references PUBLIC.USERS(USERNAME),
	DSR_REQUEST_ID  bigint references GDPR.DATA_SUBJECT_REQUEST(REQUEST_ID)
);

-- Retention rules (Art. 5(1)(e)).
-- PG-delta: retention interval -> RETENTION_YEARS integer.
create table GDPR.RETENTION_POLICY (
	POLICY_ID       bigint generated by default as identity primary key,
	RECORD_CLASS    varchar(255) not null unique,
	RETENTION_YEARS integer not null,
	LEGAL_REFERENCE varchar(255) not null,
	ACTION_AFTER    varchar(32) not null check (ACTION_AFTER in ('review', 'anonymize', 'delete'))
);
insert into GDPR.RETENTION_POLICY (RECORD_CLASS, RETENTION_YEARS, LEGAL_REFERENCE, ACTION_AFTER) values
	('birth_record',    100, 'zakon c. 154/1994 Z.z. o matrikach', 'review'),
	('marriage_record',  75, 'zakon c. 154/1994 Z.z. o matrikach', 'review'),
	('death_record',     75, 'zakon c. 154/1994 Z.z. o matrikach', 'review');

-- ----------------------------------------------------------------------------
-- 5. Audit schema — NIS2 / GDPR accountability.
-- PG-delta: audit_log/access_log PARTITION BY RANGE -> plain tables.
-- PG-delta: trigger-captured row changes + pgcrypto SHA-256 hash chain ->
--   AuditService (Java SHA-256 chain; chain-head row pessimistically locked).
-- PG-delta: inet -> varchar(45); jsonb -> clob.
-- ----------------------------------------------------------------------------
create table AUDIT.CHAIN_HEAD (
	ID        integer primary key check (ID = 1),
	LAST_HASH varchar(64) not null
);
-- Genesis = lowercase hex SHA-256 of the string 'genesis' (AuditService convention).
insert into AUDIT.CHAIN_HEAD (ID, LAST_HASH) values
	(1, 'aeebad4a796fcc2e15dc4c6061b45ed9b373f26adfc798ca7d2d8cc58182718e');

create table AUDIT.AUDIT_LOG (
	AUDIT_ID     bigint generated by default as identity primary key,
	EVENT_TIME   timestamp not null default current_timestamp,
	ACTOR        varchar(255) not null,
	DB_ROLE      varchar(255) not null,
	CLIENT_ADDR  varchar(45),
	REQUEST_ID   varchar(255),
	LEGAL_BASIS  varchar(255),
	ACTION       varchar(32) not null check (ACTION in ('INSERT', 'UPDATE', 'DELETE')),
	SCHEMA_NAME  varchar(255) not null,
	TABLE_NAME   varchar(255) not null,
	OLD_ROW      clob,
	NEW_ROW      clob,
	PREV_HASH    varchar(64) not null,
	ROW_HASH     varchar(64) not null
);
create index IDX_AUDIT_LOG_TIME on AUDIT.AUDIT_LOG (EVENT_TIME);

create table AUDIT.ACCESS_LOG (
	ACCESS_ID    bigint generated by default as identity primary key,
	ACCESSED_AT  timestamp not null default current_timestamp,
	ACTOR        varchar(255) not null,
	PERSON_UID   varchar(32) not null,
	PURPOSE      varchar(255) not null,
	LEGAL_BASIS  varchar(255),
	FIELDS       varchar(1000),
	REQUEST_ID   varchar(255)
);
create index IDX_ACCESS_LOG_PERSON on AUDIT.ACCESS_LOG (PERSON_UID);

create table AUDIT.SECURITY_EVENT (
	EVENT_ID            bigint generated by default as identity primary key,
	EVENT_TIME          timestamp not null default current_timestamp,
	EVENT_TYPE          varchar(32) not null check (EVENT_TYPE in
		('login_failure', 'login_success', 'privilege_change', 'config_change',
		'backup', 'restore', 'incident', 'incident_early_warning',
		'incident_notification', 'incident_final_report')),
	SEVERITY            varchar(32) not null check (SEVERITY in ('info', 'warning', 'critical')),
	ACTOR               varchar(255),
	SOURCE_ADDR         varchar(45),
	DETAILS             clob,
	INCIDENT_REF        varchar(255),
	REPORTED_TO_CSIRT_AT timestamp
);

-- ----------------------------------------------------------------------------
-- 6. Minimal-exposure current-state view for other state agencies.
--    No religion, no health, no national_id, no history; anonymized persons
--    and merged duplicates excluded. Portable ANSI rewrite of the PG view
--    (daterange @> current_date -> explicit valid_from/valid_to comparisons;
--    LATERAL permanent-address pick -> left join, unique current permanent
--    address per person guaranteed by BitemporalGuard).
-- ----------------------------------------------------------------------------
create view REGISTRY.V_PERSON_CURRENT as
select
	p.PERSON_UID,
	d.GIVEN_NAMES,
	d.FAMILY_NAME,
	d.BIRTH_SURNAME,
	d.SEX,
	d.DATE_OF_BIRTH,
	d.DATE_OF_BIRTH_PRECISION,
	d.CITIZENSHIP,
	a.STREET,
	a.HOUSE_NUMBER,
	a.POSTAL_CODE,
	a.MUNICIPALITY_CODE,
	a.COUNTRY_CODE,
	case when exists (
		select 1 from REGISTRY.DEATH_RECORD dr
		where dr.PERSON_UID = p.PERSON_UID and dr.SUPERSEDED_AT is null
	) then true else false end as DECEASED
from REGISTRY.PERSON p
join REGISTRY.PERSON_DETAIL d
	on d.PERSON_UID = p.PERSON_UID
	and d.SUPERSEDED_AT is null
	and d.VALID_FROM <= current_date
	and (d.VALID_TO is null or d.VALID_TO > current_date)
left join REGISTRY.PERSON_ADDRESS pa
	on pa.PERSON_UID = p.PERSON_UID
	and pa.ADDRESS_TYPE = 'permanent'
	and pa.SUPERSEDED_AT is null
	and pa.VALID_FROM <= current_date
	and (pa.VALID_TO is null or pa.VALID_TO > current_date)
left join REGISTRY.ADDRESS a on a.ADDRESS_ID = pa.ADDRESS_ID
where p.ANONYMIZED = false
	and not exists (
		select 1 from REGISTRY.PERSON_MERGE pm
		where pm.DUPLICATE_UID = p.PERSON_UID and pm.SUPERSEDED_AT is null
	);
