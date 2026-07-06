-- ============================================================================
-- Civil Registry System — PostgreSQL schema (schema only, no application code)
--
-- Purpose:  State/country registry of persons and vital events (births,
--           marriages, deaths) with:
--             * opaque unique person identifier (generated, check-digit)
--             * bitemporal history for mutable person attributes
--               (names, addresses, religion): valid time + transaction time
--             * versioned (append-only) vital records with amendment trail
--             * role-based access (DB roles + RLS + column-level grants)
--             * tamper-evident audit log (hash chain) — NIS2
--             * GDPR structures: DSR workflow, disclosure register,
--               consent, retention policy, read-access log
--
-- Prerequisites: PostgreSQL 14+, extensions pgcrypto, btree_gist.
-- Run:           psql -d registry -f schema.sql   (as superuser / owner)
--
-- Notes:
--   * The application authenticates end users and sets per-transaction:
--       SET LOCAL app.user_id    = '<staff_user.user_id>';
--       SET LOCAL app.request_id = '<correlation id>';
--       SET LOCAL app.legal_basis= '<legal basis code>';
--   * national_id (rodne cislo equivalent) is stored encrypted; the
--     encryption key lives OUTSIDE the database (KMS/HSM). The DB stores
--     ciphertext + keyed HMAC for exact-match lookup.
-- ============================================================================

BEGIN;

CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA public;
CREATE EXTENSION IF NOT EXISTS btree_gist;

-- ----------------------------------------------------------------------------
-- 0. Schemas and hardening
-- ----------------------------------------------------------------------------
CREATE SCHEMA IF NOT EXISTS codelist;   -- reference data (low sensitivity)
CREATE SCHEMA IF NOT EXISTS registry;   -- core registry (personal data)
CREATE SCHEMA IF NOT EXISTS audit;      -- append-only audit trail
CREATE SCHEMA IF NOT EXISTS gdpr;       -- data-protection operations

REVOKE CREATE ON SCHEMA public FROM PUBLIC;

-- ----------------------------------------------------------------------------
-- 1. Roles (group roles, NOLOGIN; login users are granted membership)
-- ----------------------------------------------------------------------------
DO $$
BEGIN
  -- application service account (connection pool); no direct human use
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'registry_app') THEN
    CREATE ROLE registry_app NOLOGIN;
  END IF;
  -- registry office clerk: registers events, edits data for own office only
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'registry_clerk') THEN
    CREATE ROLE registry_clerk NOLOGIN;
  END IF;
  -- supervisor/matrikar: nationwide read, approves amendments
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'registry_supervisor') THEN
    CREATE ROLE registry_supervisor NOLOGIN;
  END IF;
  -- other state agencies: minimal current-state view, NO religion, NO health
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'agency_reader') THEN
    CREATE ROLE agency_reader NOLOGIN;
  END IF;
  -- data protection officer: full read + GDPR request handling, no data edits
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'dpo_officer') THEN
    CREATE ROLE dpo_officer NOLOGIN;
  END IF;
  -- security auditor (NIS2): audit/security logs only, no personal data
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'security_auditor') THEN
    CREATE ROLE security_auditor NOLOGIN;
  END IF;
  -- citizen self-service portal: read own data only (RLS-enforced); the
  -- gateway authenticates via eID and sets app.person_uid per transaction
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'registry_citizen') THEN
    CREATE ROLE registry_citizen NOLOGIN;
  END IF;
END $$;

-- ----------------------------------------------------------------------------
-- 2. Codelists
--    Sourced from the official MetaIS codelist repository (one-time import via
--    import_codelists.py). Every codelist table carries provenance columns:
--      source_codelist  MetaIS codelist code (e.g. 'CL000025'); NULL = own list
--      metais_code      official item code in the source codelist
--      item_valid_from / item_valid_to   official validity of the item
--    Two classes of tables:
--      * identity codelists — PK IS the official MetaIS item code
--        (country, region, district, municipality, ...)
--      * semantic codelists — PK is an internal stable key baked into app
--        logic (sex, address_type); metais_code maps to the official item.
-- ----------------------------------------------------------------------------

-- CL000086 Krajina OSN
CREATE TABLE codelist.country (
  country_code    text PRIMARY KEY,             -- official MetaIS item code
  name            text NOT NULL,
  source_codelist text,
  metais_code     text,
  item_valid_from date,
  item_valid_to   date
);

-- CL000023 Regionalna statisticka uzemna jednotka 3 - kraj
CREATE TABLE codelist.region (
  region_code     text PRIMARY KEY,
  name            text NOT NULL,
  source_codelist text,
  metais_code     text,
  item_valid_from date,
  item_valid_to   date
);

-- CL000024 Lokalna statisticka uzemna jednotka 1 - okres
CREATE TABLE codelist.district (
  district_code   text PRIMARY KEY,
  name            text NOT NULL,
  region_code     text REFERENCES codelist.region,   -- MetaIS items carry no
  source_codelist text,                              -- hierarchy; filled manually
  metais_code     text,                              -- or by code-prefix heuristics
  item_valid_from date,
  item_valid_to   date
);

-- CL000025 Lokalna statisticka uzemna jednotka 2 - obec
CREATE TABLE codelist.municipality (
  municipality_code text PRIMARY KEY,
  name              text NOT NULL,
  district_code     text REFERENCES codelist.district,  -- nullable: not in MetaIS items
  country_code      text REFERENCES codelist.country,
  source_codelist   text,
  metais_code       text,
  item_valid_from   date,
  item_valid_to     date
);

-- CL010141 Cast obce
CREATE TABLE codelist.municipality_part (
  part_code         text PRIMARY KEY,
  name              text NOT NULL,
  municipality_code text REFERENCES codelist.municipality,  -- nullable: not in MetaIS items
  source_codelist   text,
  metais_code       text,
  item_valid_from   date,
  item_valid_to     date
);

-- CL010139 Druh adresy — semantic: internal keys used by app logic and views
CREATE TABLE codelist.address_type (
  address_type_code text PRIMARY KEY,
  name              text NOT NULL,
  source_codelist   text,
  metais_code       text,
  item_valid_from   date,
  item_valid_to     date
);
INSERT INTO codelist.address_type (address_type_code, name) VALUES
  ('permanent','Trvaly pobyt'), ('temporary','Prechodny pobyt'),
  ('foreign','Pobyt v zahranici'), ('official','Uradna adresa');

-- CL003003 Pohlavie — semantic: internal keys M/F/X; MetaIS codes 1/2/0
CREATE TABLE codelist.sex (
  sex_code        text PRIMARY KEY,
  name            text NOT NULL,
  source_codelist text,
  metais_code     text,
  item_valid_from date,
  item_valid_to   date
);
INSERT INTO codelist.sex (sex_code, name, source_codelist, metais_code) VALUES
  ('M','muz','CL003003','1'), ('F','zena','CL003003','2'),
  ('X','nezistene','CL003003','0');

-- CL004002 Rodinny stav
CREATE TABLE codelist.marital_status (
  marital_status_code text PRIMARY KEY,
  name            text NOT NULL,
  source_codelist text,
  metais_code     text,
  item_valid_from date,
  item_valid_to   date
);

-- CL004003 Existencny stav (zije / mrtvy / nezvestny / vyhlaseny za mrtveho)
CREATE TABLE codelist.existence_status (
  existence_status_code text PRIMARY KEY,
  name            text NOT NULL,
  source_codelist text,
  metais_code     text,
  item_valid_from date,
  item_valid_to   date
);

-- CL000062 Titul pred menom + CL000063 Titul za menom (one table, two sources).
-- Item codes overlap between the two source codelists, hence the composite PK.
CREATE TABLE codelist.academic_title (
  title_code      text NOT NULL,
  name            text NOT NULL,
  title_position  text NOT NULL CHECK (title_position IN ('before','after')),
  PRIMARY KEY (title_code, title_position),
  source_codelist text,
  metais_code     text,
  item_valid_from date,
  item_valid_to   date
);

-- CL000014 Sud
CREATE TABLE codelist.court (
  court_code      text PRIMARY KEY,
  name            text NOT NULL,
  source_codelist text,
  metais_code     text,
  item_valid_from date,
  item_valid_to   date
);

-- CL000010 Typ rodinneho vztahu — imported for a future kinship module;
-- no registry.* table references it yet.
CREATE TABLE codelist.family_relation_type (
  relation_code   text PRIMARY KEY,
  name            text NOT NULL,
  source_codelist text,
  metais_code     text,
  item_valid_from date,
  item_valid_to   date
);

-- Registered churches / religious societies + administrative values.
-- NOT in MetaIS — own list based on the SU SR statistical codelist
-- "Nabozenske vyznanie" (source_codelist stays NULL).
CREATE TABLE codelist.religion (
  religion_code   text PRIMARY KEY,             -- e.g. 'RIMKAT', 'NONE', 'UNDISCLOSED'
  name            text NOT NULL,
  source_codelist text,
  metais_code     text,
  item_valid_from date,
  item_valid_to   date
);

-- CL000015 Pracovisko matricneho uradu — office_code = official MetaIS item code
CREATE TABLE codelist.registry_office (
  office_code       text PRIMARY KEY,
  name              text NOT NULL,
  municipality_code text REFERENCES codelist.municipality,  -- nullable: not in MetaIS items
  source_codelist   text,
  metais_code       text,
  item_valid_from   date,
  item_valid_to     date
);

-- ----------------------------------------------------------------------------
-- 3. Staff (needed for RLS scoping and audit actor references)
-- ----------------------------------------------------------------------------
CREATE TABLE registry.staff_user (
  user_id     text PRIMARY KEY,                 -- application login id
  full_name   text NOT NULL,
  office_code text REFERENCES codelist.registry_office,  -- NULL = central authority
  staff_role  text NOT NULL CHECK (staff_role IN
                ('clerk','supervisor','agency','dpo','security_auditor')),
  active      boolean NOT NULL DEFAULT true,
  created_at  timestamptz NOT NULL DEFAULT now()
);

-- ----------------------------------------------------------------------------
-- 4. Person UID generator
--    Format: P-XXXXX-XXXXX-C
--      X = Crockford base32 char (no I, L, O, U), crypto-random
--      C = position-weighted mod-32 check character
--    Opaque: carries no birth date, sex, or sequence information.
--    Space: 32^10 ~ 1.1e15; collision handled by retry loop.
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION registry.generate_person_uid()
RETURNS text
LANGUAGE plpgsql
VOLATILE
AS $$
DECLARE
  alphabet   constant text := '0123456789ABCDEFGHJKMNPQRSTVWXYZ';
  raw        bytea;
  body       text;
  csum       int;
  candidate  text;
  i          int;
BEGIN
  LOOP
    raw  := gen_random_bytes(10);
    body := '';
    FOR i IN 0..9 LOOP
      body := body || substr(alphabet, 1 + (get_byte(raw, i) % 32), 1);
    END LOOP;

    csum := 0;
    FOR i IN 1..10 LOOP
      csum := csum + i * (strpos(alphabet, substr(body, i, 1)) - 1);
    END LOOP;

    candidate := 'P-' || substr(body, 1, 5) || '-' || substr(body, 6, 5)
              || '-' || substr(alphabet, 1 + (csum % 32), 1);

    EXIT WHEN NOT EXISTS (SELECT 1 FROM registry.person WHERE person_uid = candidate);
  END LOOP;
  RETURN candidate;
END $$;

CREATE OR REPLACE FUNCTION registry.person_uid_is_valid(uid text)
RETURNS boolean
LANGUAGE plpgsql
IMMUTABLE
AS $$
DECLARE
  alphabet constant text := '0123456789ABCDEFGHJKMNPQRSTVWXYZ';
  body     text;
  csum     int := 0;
  i        int;
BEGIN
  IF uid !~ '^P-[0-9A-HJKMNP-TV-Z]{5}-[0-9A-HJKMNP-TV-Z]{5}-[0-9A-HJKMNP-TV-Z]$' THEN
    RETURN false;
  END IF;
  body := substr(uid, 3, 5) || substr(uid, 9, 5);
  FOR i IN 1..10 LOOP
    csum := csum + i * (strpos(alphabet, substr(body, i, 1)) - 1);
  END LOOP;
  RETURN substr(alphabet, 1 + (csum % 32), 1) = substr(uid, 15, 1);
END $$;

-- ----------------------------------------------------------------------------
-- 5. Person — immutable identity anchor
--    Everything mutable lives in bitemporal satellite tables below.
-- ----------------------------------------------------------------------------
CREATE TABLE registry.person (
  person_uid       text PRIMARY KEY
                     CONSTRAINT person_uid_valid CHECK (registry.person_uid_is_valid(person_uid)),
  -- legacy national identifier (rodne cislo equivalent): ciphertext only,
  -- key held in external KMS/HSM; HMAC allows exact-match lookup w/o decryption
  national_id_enc  bytea,
  national_id_hmac bytea UNIQUE,
  anonymized       boolean NOT NULL DEFAULT false,   -- retention-expiry tombstone
  created_at       timestamptz NOT NULL DEFAULT now(),
  created_by       text NOT NULL REFERENCES registry.staff_user
);

ALTER TABLE registry.person
  ALTER COLUMN person_uid SET DEFAULT registry.generate_person_uid();

-- ----------------------------------------------------------------------------
-- 6. Bitemporal satellites
--    Two time axes on every row:
--      valid_from / valid_to        — when the fact was true in the real world
--      recorded_at / superseded_at  — when the registry believed it
--    Rows are never UPDATEd in place except to set superseded_at/_by.
--    Current belief  = superseded_at IS NULL
--    Current fact    = current belief AND valid_to IS NULL (or >= today)
-- ----------------------------------------------------------------------------

-- 6.1 Identity details (names, sex, birth data, citizenship)
CREATE TABLE registry.person_detail (
  detail_id       bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  person_uid      text NOT NULL REFERENCES registry.person,
  given_names     text NOT NULL,
  family_name     text NOT NULL,
  birth_surname   text,
  sex             text NOT NULL REFERENCES codelist.sex,
  -- partially known birth dates (foreign or historic records): store the
  -- earliest day of the known period ('month' -> 1st, 'year' -> Jan 1)
  date_of_birth   date NOT NULL,
  date_of_birth_precision text NOT NULL DEFAULT 'day'
    CHECK (date_of_birth_precision IN ('day','month','year')),
  place_of_birth_municipality text REFERENCES codelist.municipality,
  place_of_birth_foreign      text,             -- free text when born abroad
  citizenship     text NOT NULL REFERENCES codelist.country,
  marital_status_code   text REFERENCES codelist.marital_status,
  existence_status_code text REFERENCES codelist.existence_status,
  -- academic titles: arrays of codelist.academic_title codes; PostgreSQL
  -- cannot enforce FKs on array elements — validated by the application
  titles_before   text[],
  titles_after    text[],
  -- bitemporal columns
  valid_from      date NOT NULL,
  valid_to        date,
  recorded_at     timestamptz NOT NULL DEFAULT now(),
  recorded_by     text NOT NULL REFERENCES registry.staff_user,
  superseded_at   timestamptz,
  superseded_by   bigint REFERENCES registry.person_detail,
  change_reason   text,
  CHECK (valid_to IS NULL OR valid_to > valid_from),
  -- at most one current-belief version per person per point in valid time
  EXCLUDE USING gist (
    person_uid WITH =,
    daterange(valid_from, valid_to) WITH &&
  ) WHERE (superseded_at IS NULL)
);

CREATE INDEX idx_person_detail_current
  ON registry.person_detail (person_uid) WHERE superseded_at IS NULL;
CREATE INDEX idx_person_detail_name
  ON registry.person_detail (family_name, given_names) WHERE superseded_at IS NULL;

-- 6.2 Addresses — normalized address + bitemporal person link
CREATE TABLE registry.address (
  address_id        uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  street            text,
  house_number      text,
  postal_code       text,
  municipality_code text REFERENCES codelist.municipality,
  municipality_part_code text REFERENCES codelist.municipality_part,
  country_code      text NOT NULL REFERENCES codelist.country,
  foreign_address   text,                       -- unstructured, non-domestic
  CHECK (municipality_code IS NOT NULL OR foreign_address IS NOT NULL)
);

CREATE TABLE registry.person_address (
  person_address_id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  person_uid        text NOT NULL REFERENCES registry.person,
  address_id        uuid NOT NULL REFERENCES registry.address,
  address_type      text NOT NULL REFERENCES codelist.address_type,
  -- bitemporal columns
  valid_from        date NOT NULL,
  valid_to          date,
  recorded_at       timestamptz NOT NULL DEFAULT now(),
  recorded_by       text NOT NULL REFERENCES registry.staff_user,
  superseded_at     timestamptz,
  superseded_by     bigint REFERENCES registry.person_address,
  change_reason     text,
  CHECK (valid_to IS NULL OR valid_to > valid_from),
  -- one current-belief address per person and type at any point in valid time
  EXCLUDE USING gist (
    person_uid   WITH =,
    address_type WITH =,
    daterange(valid_from, valid_to) WITH &&
  ) WHERE (superseded_at IS NULL)
);

CREATE INDEX idx_person_address_current
  ON registry.person_address (person_uid) WHERE superseded_at IS NULL;

-- 6.3 Religion — GDPR Art. 9 special category; isolated table, own grants.
--     Every row must carry its Art. 9(2) legal basis; consent-based rows
--     reference gdpr.consent (FK added after gdpr schema below).
CREATE TABLE registry.person_religion (
  person_religion_id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  person_uid         text NOT NULL REFERENCES registry.person,
  religion_code      text NOT NULL REFERENCES codelist.religion,
  legal_basis        text NOT NULL CHECK (legal_basis IN
                       ('art9_2a_consent','art9_2g_public_interest','art9_2j_archiving')),
  consent_id         bigint,                    -- FK -> gdpr.consent (added below)
  -- bitemporal columns
  valid_from         date NOT NULL,
  valid_to           date,
  recorded_at        timestamptz NOT NULL DEFAULT now(),
  recorded_by        text NOT NULL REFERENCES registry.staff_user,
  superseded_at      timestamptz,
  superseded_by      bigint REFERENCES registry.person_religion,
  change_reason      text,
  CHECK (valid_to IS NULL OR valid_to > valid_from),
  EXCLUDE USING gist (
    person_uid WITH =,
    daterange(valid_from, valid_to) WITH &&
  ) WHERE (superseded_at IS NULL)
);

CREATE INDEX idx_person_religion_current
  ON registry.person_religion (person_uid) WHERE superseded_at IS NULL;

-- 6.4 Legal capacity restrictions (court-ordered deprivation or limitation
--     of legal capacity). Drives the self-service portal eligibility rule
--     and is relevant to the validity of registered acts.
CREATE TABLE registry.legal_capacity_restriction (
  restriction_id   bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  person_uid       text NOT NULL REFERENCES registry.person,
  restriction_type text NOT NULL CHECK (restriction_type IN
                     ('full_deprivation','partial_limitation')),
  scope            text,                        -- affected acts (partial limitation)
  court_code       text REFERENCES codelist.court,
  court_reference  text,                        -- decision / file number
  -- bitemporal columns
  valid_from       date NOT NULL,
  valid_to         date,
  recorded_at      timestamptz NOT NULL DEFAULT now(),
  recorded_by      text NOT NULL REFERENCES registry.staff_user,
  superseded_at    timestamptz,
  superseded_by    bigint REFERENCES registry.legal_capacity_restriction,
  change_reason    text,
  CHECK (valid_to IS NULL OR valid_to > valid_from)
);

CREATE INDEX idx_capacity_current
  ON registry.legal_capacity_restriction (person_uid) WHERE superseded_at IS NULL;

-- 6.5 Representation (parent, court-appointed guardian). Parents may access
--     a minor's data via the portal; guardians access the represented
--     person's data regardless of age.
CREATE TABLE registry.person_representation (
  representation_id   bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  represented_uid     text NOT NULL REFERENCES registry.person,
  representative_uid  text NOT NULL REFERENCES registry.person,
  representation_type text NOT NULL CHECK (representation_type IN ('parent','guardian')),
  legal_basis_ref     text,                     -- birth record ref / court decision
  court_code          text REFERENCES codelist.court,
  -- bitemporal columns
  valid_from          date NOT NULL,
  valid_to            date,
  recorded_at         timestamptz NOT NULL DEFAULT now(),
  recorded_by         text NOT NULL REFERENCES registry.staff_user,
  superseded_at       timestamptz,
  superseded_by       bigint REFERENCES registry.person_representation,
  change_reason       text,
  CHECK (represented_uid <> representative_uid),
  CHECK (valid_to IS NULL OR valid_to > valid_from),
  -- one current relationship of a given type per pair at any point in time
  EXCLUDE USING gist (
    represented_uid WITH =,
    representative_uid WITH =,
    representation_type WITH =,
    daterange(valid_from, valid_to) WITH &&
  ) WHERE (superseded_at IS NULL)
);

CREATE INDEX idx_representation_representative
  ON registry.person_representation (representative_uid) WHERE superseded_at IS NULL;

-- 6.6 Person identity merge — two records found to describe the same physical
--     person. The duplicate record is preserved (append-only); an active link
--     redirects to the primary. Merges are reversible via supersede.
--     No chains: a primary must not itself be merged.
CREATE TABLE registry.person_merge (
  merge_id       bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  duplicate_uid  text NOT NULL REFERENCES registry.person,
  primary_uid    text NOT NULL REFERENCES registry.person,
  reason         text NOT NULL,
  merged_at      timestamptz NOT NULL DEFAULT now(),
  decided_by     text NOT NULL REFERENCES registry.staff_user,
  approved_by    text REFERENCES registry.staff_user,   -- supervisor sign-off
  superseded_at  timestamptz,                           -- merge reversed
  reversal_reason text,
  CHECK (duplicate_uid <> primary_uid),
  CHECK (superseded_at IS NULL OR reversal_reason IS NOT NULL)
);

CREATE UNIQUE INDEX uq_person_merge_active
  ON registry.person_merge (duplicate_uid) WHERE superseded_at IS NULL;

CREATE OR REPLACE FUNCTION registry.person_merge_guard()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
  IF EXISTS (SELECT 1 FROM registry.person_merge
             WHERE duplicate_uid = NEW.primary_uid AND superseded_at IS NULL) THEN
    RAISE EXCEPTION 'person_merge: primary % is itself merged (chains not allowed)',
      NEW.primary_uid;
  END IF;
  IF EXISTS (SELECT 1 FROM registry.person_merge
             WHERE primary_uid = NEW.duplicate_uid AND superseded_at IS NULL) THEN
    RAISE EXCEPTION 'person_merge: duplicate % is primary of another active merge',
      NEW.duplicate_uid;
  END IF;
  RETURN NEW;
END $$;

CREATE TRIGGER trg_person_merge_guard
  BEFORE INSERT ON registry.person_merge
  FOR EACH ROW EXECUTE FUNCTION registry.person_merge_guard();

-- Canonical identity: follow the active merge link (single hop by design).
CREATE OR REPLACE FUNCTION registry.resolve_person_uid(uid text)
RETURNS text
LANGUAGE sql
STABLE
AS $$
  SELECT coalesce(
    (SELECT primary_uid FROM registry.person_merge
     WHERE duplicate_uid = uid AND superseded_at IS NULL),
    uid)
$$;

-- ----------------------------------------------------------------------------
-- 7. Vital records — append-only, transaction-time versioned
--    A record describes an event; the event date itself is the fact, so only
--    transaction time applies. Corrections INSERT a new version referencing
--    the superseded one (supersedes_id); originals are never updated/deleted.
--    Registry number (office, year, entry) is unique among current versions.
-- ----------------------------------------------------------------------------

CREATE TABLE registry.birth_record (
  record_id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  office_code         text NOT NULL REFERENCES codelist.registry_office,
  book_year           int  NOT NULL,
  entry_no            int  NOT NULL,
  child_uid           text NOT NULL REFERENCES registry.person,
  date_of_birth       date NOT NULL,
  time_of_birth       time,
  place_municipality  text REFERENCES codelist.municipality,
  place_foreign       text,
  facility            text,                     -- hospital / facility name
  multiple_birth_order int,                     -- NULL = single birth
  mother_uid          text REFERENCES registry.person,
  father_uid          text REFERENCES registry.person,
  -- versioning
  recorded_at         timestamptz NOT NULL DEFAULT now(),
  recorded_by         text NOT NULL REFERENCES registry.staff_user,
  superseded_at       timestamptz,
  supersedes_id       uuid REFERENCES registry.birth_record,
  amendment_reason    text,
  approved_by         text REFERENCES registry.staff_user,  -- supervisor sign-off on amendments
  CHECK (supersedes_id IS NULL OR amendment_reason IS NOT NULL)
);

CREATE UNIQUE INDEX uq_birth_registry_no
  ON registry.birth_record (office_code, book_year, entry_no)
  WHERE superseded_at IS NULL;
CREATE INDEX idx_birth_child ON registry.birth_record (child_uid);

CREATE TABLE registry.marriage_record (
  record_id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  office_code         text NOT NULL REFERENCES codelist.registry_office,
  book_year           int  NOT NULL,
  entry_no            int  NOT NULL,
  spouse1_uid         text NOT NULL REFERENCES registry.person,
  spouse2_uid         text NOT NULL REFERENCES registry.person,
  marriage_date       date NOT NULL,
  place_municipality  text REFERENCES codelist.municipality,
  place_foreign       text,
  marriage_form       text NOT NULL CHECK (marriage_form IN ('civil','religious')),
  officiant           text,
  witness1_name       text,
  witness2_name       text,
  -- post-marriage surname agreements produce new person_detail versions;
  -- the record only stores the declared agreement text
  surname_agreement   text,
  -- versioning
  recorded_at         timestamptz NOT NULL DEFAULT now(),
  recorded_by         text NOT NULL REFERENCES registry.staff_user,
  superseded_at       timestamptz,
  supersedes_id       uuid REFERENCES registry.marriage_record,
  amendment_reason    text,
  approved_by         text REFERENCES registry.staff_user,
  CHECK (spouse1_uid <> spouse2_uid),
  CHECK (supersedes_id IS NULL OR amendment_reason IS NOT NULL)
);

CREATE UNIQUE INDEX uq_marriage_registry_no
  ON registry.marriage_record (office_code, book_year, entry_no)
  WHERE superseded_at IS NULL;
CREATE INDEX idx_marriage_spouse1 ON registry.marriage_record (spouse1_uid);
CREATE INDEX idx_marriage_spouse2 ON registry.marriage_record (spouse2_uid);

CREATE TABLE registry.marriage_dissolution (
  dissolution_id    uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  marriage_id       uuid NOT NULL REFERENCES registry.marriage_record,
  dissolution_type  text NOT NULL CHECK (dissolution_type IN
                      ('divorce','annulment','spouse_death')),
  effective_date    date NOT NULL,
  court_code        text REFERENCES codelist.court,
  court_reference   text,                       -- case / file number
  recorded_at       timestamptz NOT NULL DEFAULT now(),
  recorded_by       text NOT NULL REFERENCES registry.staff_user,
  superseded_at     timestamptz,
  supersedes_id     uuid REFERENCES registry.marriage_dissolution,
  amendment_reason  text
);

CREATE TABLE registry.death_record (
  record_id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  office_code         text NOT NULL REFERENCES codelist.registry_office,
  book_year           int  NOT NULL,
  entry_no            int  NOT NULL,
  person_uid          text NOT NULL REFERENCES registry.person,
  date_of_death       date,                     -- NULL when only found/declared
  date_found          date,
  declared_by_court   boolean NOT NULL DEFAULT false,   -- presumed death
  court_code          text REFERENCES codelist.court,
  court_reference     text,                             -- case / file number
  place_municipality  text REFERENCES codelist.municipality,
  place_foreign       text,
  -- versioning
  recorded_at         timestamptz NOT NULL DEFAULT now(),
  recorded_by         text NOT NULL REFERENCES registry.staff_user,
  superseded_at       timestamptz,
  supersedes_id       uuid REFERENCES registry.death_record,
  amendment_reason    text,
  approved_by         text REFERENCES registry.staff_user,
  CHECK (date_of_death IS NOT NULL OR date_found IS NOT NULL OR declared_by_court),
  CHECK (supersedes_id IS NULL OR amendment_reason IS NOT NULL)
);

CREATE UNIQUE INDEX uq_death_registry_no
  ON registry.death_record (office_code, book_year, entry_no)
  WHERE superseded_at IS NULL;
CREATE INDEX idx_death_person ON registry.death_record (person_uid);

-- Cause of death = health data (GDPR Art. 9). Isolated so grants can differ
-- from the administrative death record.
CREATE TABLE registry.death_medical (
  death_record_id  uuid PRIMARY KEY REFERENCES registry.death_record,
  cause_of_death_icd10 text,
  autopsy_performed    boolean,
  certified_by         text,                    -- physician name / license no.
  recorded_at          timestamptz NOT NULL DEFAULT now(),
  recorded_by          text NOT NULL REFERENCES registry.staff_user
);

-- ----------------------------------------------------------------------------
-- 8. GDPR schema
-- ----------------------------------------------------------------------------

CREATE TABLE gdpr.consent (
  consent_id    bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  person_uid    text NOT NULL REFERENCES registry.person,
  purpose       text NOT NULL,                  -- e.g. 'religion_registration'
  given_at      timestamptz NOT NULL,
  given_via     text NOT NULL,                  -- 'in_person','written','portal'
  withdrawn_at  timestamptz,
  recorded_by   text NOT NULL REFERENCES registry.staff_user
);

ALTER TABLE registry.person_religion
  ADD CONSTRAINT fk_religion_consent
  FOREIGN KEY (consent_id) REFERENCES gdpr.consent,
  ADD CONSTRAINT religion_consent_required
  CHECK (legal_basis <> 'art9_2a_consent' OR consent_id IS NOT NULL);

-- Data subject requests (Art. 15-21) with statutory one-month deadline.
CREATE TABLE gdpr.data_subject_request (
  request_id    bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  person_uid    text REFERENCES registry.person,   -- NULL until identity verified
  requester_ref text NOT NULL,                     -- ticket / filing number
  request_type  text NOT NULL CHECK (request_type IN
                  ('access','rectification','erasure','restriction',
                   'objection','portability')),
  received_at   date NOT NULL DEFAULT current_date,
  due_at        date NOT NULL GENERATED ALWAYS AS
                  (((received_at + interval '1 month'))::date) STORED,
  status        text NOT NULL DEFAULT 'received' CHECK (status IN
                  ('received','identity_check','in_progress','extended',
                   'fulfilled','refused')),
  handled_by    text REFERENCES registry.staff_user,
  resolved_at   date,
  resolution    text                               -- outcome incl. Art.17(3)(b) refusals
);

-- Art. 30 disclosure register: every hand-over of personal data to a recipient.
CREATE TABLE gdpr.disclosure_register (
  disclosure_id   bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  person_uid      text NOT NULL REFERENCES registry.person,
  recipient       text NOT NULL,                 -- agency / court / person
  recipient_type  text NOT NULL CHECK (recipient_type IN
                    ('state_agency','court','municipality','data_subject','other')),
  legal_basis     text NOT NULL,
  disclosed_fields text[] NOT NULL,
  disclosed_at    timestamptz NOT NULL DEFAULT now(),
  disclosed_by    text NOT NULL REFERENCES registry.staff_user,
  dsr_request_id  bigint REFERENCES gdpr.data_subject_request
);

-- Retention rules (Art. 5(1)(e)); enforcement is a scheduled job that flags
-- expired records for anonymization (registry books are long-retention by law).
CREATE TABLE gdpr.retention_policy (
  policy_id       bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  record_class    text NOT NULL UNIQUE,          -- 'birth_record','marriage_record',...
  retention       interval NOT NULL,             -- e.g. '100 years'
  legal_reference text NOT NULL,
  action_after    text NOT NULL CHECK (action_after IN ('review','anonymize','delete'))
);

-- Statutory retention per zakon c. 154/1994 Z.z. o matrikach (kniha narodeni
-- 100 rokov, kniha manzelstiev a kniha umrti 75 rokov od posledneho zapisu);
-- after expiry records are handed to the state archive, hence 'review'.
INSERT INTO gdpr.retention_policy (record_class, retention, legal_reference, action_after) VALUES
  ('birth_record',    interval '100 years', 'zakon c. 154/1994 Z.z. o matrikach', 'review'),
  ('marriage_record', interval '75 years',  'zakon c. 154/1994 Z.z. o matrikach', 'review'),
  ('death_record',    interval '75 years',  'zakon c. 154/1994 Z.z. o matrikach', 'review');

-- ----------------------------------------------------------------------------
-- 9. Audit schema — NIS2 / GDPR accountability
-- ----------------------------------------------------------------------------

-- 9.1 Tamper-evident hash chain head (serialized via row lock; the write
--     serialization is an accepted trade-off for integrity of the chain).
CREATE TABLE audit.chain_head (
  id        int PRIMARY KEY CHECK (id = 1),
  last_hash bytea NOT NULL
);
INSERT INTO audit.chain_head VALUES (1, digest('genesis', 'sha256'));

-- 9.2 Row-change audit log: every INSERT/UPDATE/DELETE on registry + gdpr
--     tables, captured by trigger. Append-only; partitioned by month.
CREATE SEQUENCE audit.audit_log_id_seq;
CREATE TABLE audit.audit_log (
  audit_id     bigint NOT NULL DEFAULT nextval('audit.audit_log_id_seq'),
  event_time   timestamptz NOT NULL DEFAULT now(),
  actor        text NOT NULL,                   -- app.user_id GUC or session_user
  db_role      text NOT NULL,
  client_addr  inet,
  request_id   text,                            -- app correlation id
  legal_basis  text,                            -- app.legal_basis GUC
  action       text NOT NULL CHECK (action IN ('INSERT','UPDATE','DELETE')),
  schema_name  text NOT NULL,
  table_name   text NOT NULL,
  old_row      jsonb,
  new_row      jsonb,
  prev_hash    bytea NOT NULL,
  row_hash     bytea NOT NULL,
  PRIMARY KEY (audit_id, event_time)
) PARTITION BY RANGE (event_time);

CREATE TABLE audit.audit_log_default PARTITION OF audit.audit_log DEFAULT;
-- monthly partitions created by ops job, e.g.:
-- CREATE TABLE audit.audit_log_2026_07 PARTITION OF audit.audit_log
--   FOR VALUES FROM ('2026-07-01') TO ('2026-08-01');

-- 9.3 Read-access log (GDPR Art. 15 "who saw my data"; written by the
--     application on every personal-data read, incl. purpose).
CREATE SEQUENCE audit.access_log_id_seq;
CREATE TABLE audit.access_log (
  access_id    bigint NOT NULL DEFAULT nextval('audit.access_log_id_seq'),
  accessed_at  timestamptz NOT NULL DEFAULT now(),
  actor        text NOT NULL,
  person_uid   text NOT NULL,
  purpose      text NOT NULL,
  legal_basis  text,
  fields       text[],
  request_id   text,
  PRIMARY KEY (access_id, accessed_at)
) PARTITION BY RANGE (accessed_at);

CREATE TABLE audit.access_log_default PARTITION OF audit.access_log DEFAULT;

-- 9.4 Security events (NIS2 Art. 21/23): authn failures, privilege changes,
--     config changes, incidents + CSIRT reporting timestamps (24h/72h).
CREATE TABLE audit.security_event (
  event_id            bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  event_time          timestamptz NOT NULL DEFAULT now(),
  event_type          text NOT NULL CHECK (event_type IN
                        ('login_failure','login_success','privilege_change',
                         'config_change','backup','restore','incident',
                         'incident_early_warning','incident_notification',
                         'incident_final_report')),
  severity            text NOT NULL CHECK (severity IN ('info','warning','critical')),
  actor               text,
  source_addr         inet,
  details             jsonb,
  incident_ref        text,                     -- groups events of one incident
  reported_to_csirt_at timestamptz              -- set when notification sent
);

-- 9.5 Generic audit trigger
CREATE OR REPLACE FUNCTION audit.log_row_change()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = audit, pg_temp
AS $$
DECLARE
  prev    bytea;
  payload text;
  h       bytea;
  v_old   jsonb;
  v_new   jsonb;
BEGIN
  IF TG_OP IN ('UPDATE','DELETE') THEN v_old := to_jsonb(OLD); END IF;
  IF TG_OP IN ('INSERT','UPDATE') THEN v_new := to_jsonb(NEW); END IF;

  SELECT last_hash INTO prev FROM audit.chain_head WHERE id = 1 FOR UPDATE;

  payload := TG_TABLE_SCHEMA || '|' || TG_TABLE_NAME || '|' || TG_OP || '|'
          || coalesce(v_old::text, '') || '|' || coalesce(v_new::text, '')
          || '|' || now()::text;
  h := public.digest(prev || convert_to(payload, 'UTF8'), 'sha256');

  INSERT INTO audit.audit_log
    (actor, db_role, client_addr, request_id, legal_basis,
     action, schema_name, table_name, old_row, new_row, prev_hash, row_hash)
  VALUES
    (coalesce(current_setting('app.user_id', true), session_user),
     current_user,
     inet_client_addr(),
     current_setting('app.request_id', true),
     current_setting('app.legal_basis', true),
     TG_OP, TG_TABLE_SCHEMA, TG_TABLE_NAME, v_old, v_new, prev, h);

  UPDATE audit.chain_head SET last_hash = h WHERE id = 1;
  RETURN coalesce(NEW, OLD);
END $$;

-- 9.6 Hard-delete protection: vital records and persons are append-only.
--     Retention expiry anonymizes (UPDATE) under a documented procedure;
--     it never deletes rows.
CREATE OR REPLACE FUNCTION registry.prevent_hard_delete()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
  RAISE EXCEPTION 'Registry table %.% is append-only: supersede or anonymize instead',
    TG_TABLE_SCHEMA, TG_TABLE_NAME;
END $$;

-- Attach audit + delete-protection triggers to all data tables.
DO $$
DECLARE t record;
BEGIN
  FOR t IN
    SELECT schemaname, tablename FROM pg_tables
    WHERE schemaname IN ('registry','gdpr','codelist')
  LOOP
    EXECUTE format(
      'CREATE TRIGGER trg_audit AFTER INSERT OR UPDATE OR DELETE ON %I.%I
       FOR EACH ROW EXECUTE FUNCTION audit.log_row_change()',
      t.schemaname, t.tablename);
  END LOOP;

  FOR t IN
    SELECT schemaname, tablename FROM pg_tables
    WHERE schemaname = 'registry'
      AND tablename IN ('person','person_detail','person_address','person_religion',
                        'birth_record','marriage_record','marriage_dissolution',
                        'death_record','death_medical',
                        'legal_capacity_restriction','person_representation',
                        'person_merge')
  LOOP
    EXECUTE format(
      'CREATE TRIGGER trg_no_delete BEFORE DELETE ON %I.%I
       FOR EACH ROW EXECUTE FUNCTION registry.prevent_hard_delete()',
      t.schemaname, t.tablename);
  END LOOP;
END $$;

-- ----------------------------------------------------------------------------
-- 10. Row-level security — clerks scoped to their registry office
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION registry.current_staff_office()
RETURNS text
LANGUAGE sql
STABLE
AS $$
  SELECT office_code FROM registry.staff_user
  WHERE user_id = current_setting('app.user_id', true) AND active
$$;

ALTER TABLE registry.birth_record    ENABLE ROW LEVEL SECURITY;
ALTER TABLE registry.marriage_record ENABLE ROW LEVEL SECURITY;
ALTER TABLE registry.death_record    ENABLE ROW LEVEL SECURITY;

-- clerks: full access to records of their own office only
CREATE POLICY clerk_own_office_birth ON registry.birth_record
  FOR ALL TO registry_clerk
  USING (office_code = registry.current_staff_office())
  WITH CHECK (office_code = registry.current_staff_office());
CREATE POLICY clerk_own_office_marriage ON registry.marriage_record
  FOR ALL TO registry_clerk
  USING (office_code = registry.current_staff_office())
  WITH CHECK (office_code = registry.current_staff_office());
CREATE POLICY clerk_own_office_death ON registry.death_record
  FOR ALL TO registry_clerk
  USING (office_code = registry.current_staff_office())
  WITH CHECK (office_code = registry.current_staff_office());

-- supervisors and DPO: nationwide read (and supervisor write)
CREATE POLICY supervisor_all_birth ON registry.birth_record
  FOR ALL TO registry_supervisor USING (true) WITH CHECK (true);
CREATE POLICY supervisor_all_marriage ON registry.marriage_record
  FOR ALL TO registry_supervisor USING (true) WITH CHECK (true);
CREATE POLICY supervisor_all_death ON registry.death_record
  FOR ALL TO registry_supervisor USING (true) WITH CHECK (true);
CREATE POLICY dpo_read_birth ON registry.birth_record
  FOR SELECT TO dpo_officer USING (true);
CREATE POLICY dpo_read_marriage ON registry.marriage_record
  FOR SELECT TO dpo_officer USING (true);
CREATE POLICY dpo_read_death ON registry.death_record
  FOR SELECT TO dpo_officer USING (true);

-- ----------------------------------------------------------------------------
-- 10b. Citizen self-service portal — "own data only"
--      The gateway authenticates the citizen via eID, resolves their
--      person_uid and sets:  SET LOCAL app.person_uid = 'P-...';
--      Every citizen-accessible table gets RLS; existing staff roles receive
--      explicit pass-through policies on the newly protected tables.
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION registry.current_citizen_uid()
RETURNS text
LANGUAGE sql
STABLE
AS $$
  SELECT current_setting('app.person_uid', true)
$$;

-- Statutory self-service eligibility: the portal user must be registered as
-- a living person, be an adult (18+), and have no current court-ordered
-- restriction of legal capacity. SECURITY DEFINER: evaluated inside citizen
-- RLS policies, so it must read tables the citizen cannot see directly.
CREATE OR REPLACE FUNCTION registry.citizen_is_eligible(uid text)
RETURNS boolean
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = registry, pg_temp
AS $$
  SELECT uid IS NOT NULL
     AND EXISTS (SELECT 1 FROM registry.person_detail d
                 WHERE d.person_uid = uid AND d.superseded_at IS NULL
                   AND daterange(d.valid_from, d.valid_to) @> current_date
                   AND d.date_of_birth + interval '18 years' <= current_date)
     AND NOT EXISTS (SELECT 1 FROM registry.death_record dr
                     WHERE dr.person_uid = uid AND dr.superseded_at IS NULL)
     AND NOT EXISTS (SELECT 1 FROM registry.legal_capacity_restriction r
                     WHERE r.person_uid = uid AND r.superseded_at IS NULL
                       AND daterange(r.valid_from, r.valid_to) @> current_date)
$$;

-- May the authenticated citizen access data of `target`?
--   * own data — if eligible;
--   * represented person's data — guardian always, parent while the
--     represented person is a minor (representation must be current).
CREATE OR REPLACE FUNCTION registry.citizen_portal_uid_accessible(target text)
RETURNS boolean
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = registry, pg_temp
AS $$
  SELECT CASE
    WHEN me.uid IS NULL OR target IS NULL THEN false
    WHEN NOT registry.citizen_is_eligible(me.uid) THEN false
    WHEN target = me.uid THEN true
    ELSE EXISTS (
      SELECT 1 FROM registry.person_representation pr
      WHERE pr.representative_uid = me.uid
        AND pr.represented_uid = target
        AND pr.superseded_at IS NULL
        AND daterange(pr.valid_from, pr.valid_to) @> current_date
        AND (pr.representation_type = 'guardian'
             OR (pr.representation_type = 'parent' AND EXISTS (
                   SELECT 1 FROM registry.person_detail d
                   WHERE d.person_uid = target AND d.superseded_at IS NULL
                     AND daterange(d.valid_from, d.valid_to) @> current_date
                     AND d.date_of_birth + interval '18 years' > current_date))))
  END
  FROM (SELECT registry.current_citizen_uid() AS uid) me
$$;

REVOKE EXECUTE ON FUNCTION registry.citizen_is_eligible(text) FROM PUBLIC;
REVOKE EXECUTE ON FUNCTION registry.citizen_portal_uid_accessible(text) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION registry.citizen_is_eligible(text) TO registry_citizen;
GRANT EXECUTE ON FUNCTION registry.citizen_portal_uid_accessible(text) TO registry_citizen;

-- person + bitemporal satellites
ALTER TABLE registry.person          ENABLE ROW LEVEL SECURITY;
ALTER TABLE registry.person_detail   ENABLE ROW LEVEL SECURITY;
ALTER TABLE registry.person_address  ENABLE ROW LEVEL SECURITY;
ALTER TABLE registry.person_religion ENABLE ROW LEVEL SECURITY;
ALTER TABLE registry.marriage_dissolution ENABLE ROW LEVEL SECURITY;
ALTER TABLE registry.legal_capacity_restriction ENABLE ROW LEVEL SECURITY;
ALTER TABLE registry.person_representation      ENABLE ROW LEVEL SECURITY;

-- staff pass-through (clerks work nationwide on person satellites; office
-- scoping applies to vital records only). Supervisor inherits via membership.
CREATE POLICY staff_all_person ON registry.person
  FOR ALL TO registry_clerk USING (true) WITH CHECK (true);
CREATE POLICY staff_all_person_detail ON registry.person_detail
  FOR ALL TO registry_clerk USING (true) WITH CHECK (true);
CREATE POLICY staff_all_person_address ON registry.person_address
  FOR ALL TO registry_clerk USING (true) WITH CHECK (true);
CREATE POLICY staff_all_person_religion ON registry.person_religion
  FOR ALL TO registry_clerk USING (true) WITH CHECK (true);
CREATE POLICY staff_all_dissolution ON registry.marriage_dissolution
  FOR ALL TO registry_clerk USING (true) WITH CHECK (true);
CREATE POLICY staff_all_capacity ON registry.legal_capacity_restriction
  FOR ALL TO registry_clerk USING (true) WITH CHECK (true);
CREATE POLICY staff_all_representation ON registry.person_representation
  FOR ALL TO registry_clerk USING (true) WITH CHECK (true);
CREATE POLICY dpo_read_person ON registry.person
  FOR SELECT TO dpo_officer USING (true);
CREATE POLICY dpo_read_person_detail ON registry.person_detail
  FOR SELECT TO dpo_officer USING (true);
CREATE POLICY dpo_read_person_address ON registry.person_address
  FOR SELECT TO dpo_officer USING (true);
CREATE POLICY dpo_read_person_religion ON registry.person_religion
  FOR SELECT TO dpo_officer USING (true);
CREATE POLICY dpo_read_dissolution ON registry.marriage_dissolution
  FOR SELECT TO dpo_officer USING (true);
CREATE POLICY dpo_read_capacity ON registry.legal_capacity_restriction
  FOR SELECT TO dpo_officer USING (true);
CREATE POLICY dpo_read_representation ON registry.person_representation
  FOR SELECT TO dpo_officer USING (true);

-- citizen: own data and data of persons they currently represent, all gated
-- by the statutory eligibility rule (living, adult, unrestricted capacity)
CREATE POLICY citizen_own_person ON registry.person
  FOR SELECT TO registry_citizen
  USING (registry.citizen_portal_uid_accessible(person_uid));
CREATE POLICY citizen_own_person_detail ON registry.person_detail
  FOR SELECT TO registry_citizen
  USING (registry.citizen_portal_uid_accessible(person_uid));
CREATE POLICY citizen_own_person_address ON registry.person_address
  FOR SELECT TO registry_citizen
  USING (registry.citizen_portal_uid_accessible(person_uid));
CREATE POLICY citizen_own_person_religion ON registry.person_religion
  FOR SELECT TO registry_citizen
  USING (registry.citizen_portal_uid_accessible(person_uid));
CREATE POLICY citizen_own_capacity ON registry.legal_capacity_restriction
  FOR SELECT TO registry_citizen
  USING (registry.citizen_portal_uid_accessible(person_uid));
CREATE POLICY citizen_own_representation ON registry.person_representation
  FOR SELECT TO registry_citizen
  USING (registry.citizen_is_eligible(registry.current_citizen_uid())
     AND (representative_uid = registry.current_citizen_uid()
       OR represented_uid   = registry.current_citizen_uid()));

-- vital records: citizen sees records they (or persons they represent)
-- participate in
CREATE POLICY citizen_own_birth ON registry.birth_record
  FOR SELECT TO registry_citizen
  USING (registry.citizen_portal_uid_accessible(child_uid)
      OR registry.citizen_portal_uid_accessible(mother_uid)
      OR registry.citizen_portal_uid_accessible(father_uid));
CREATE POLICY citizen_own_marriage ON registry.marriage_record
  FOR SELECT TO registry_citizen
  USING (registry.citizen_portal_uid_accessible(spouse1_uid)
      OR registry.citizen_portal_uid_accessible(spouse2_uid));
CREATE POLICY citizen_own_dissolution ON registry.marriage_dissolution
  FOR SELECT TO registry_citizen
  USING (EXISTS (SELECT 1 FROM registry.marriage_record m
                 WHERE m.record_id = marriage_id
                   AND (registry.citizen_portal_uid_accessible(m.spouse1_uid)
                     OR registry.citizen_portal_uid_accessible(m.spouse2_uid))));
CREATE POLICY citizen_own_death ON registry.death_record
  FOR SELECT TO registry_citizen
  USING (registry.citizen_portal_uid_accessible(person_uid));

-- GDPR self-service: own consents, own DSRs (may file new), own disclosures
ALTER TABLE gdpr.consent               ENABLE ROW LEVEL SECURITY;
ALTER TABLE gdpr.data_subject_request  ENABLE ROW LEVEL SECURITY;
ALTER TABLE gdpr.disclosure_register   ENABLE ROW LEVEL SECURITY;
CREATE POLICY staff_all_consent ON gdpr.consent
  FOR ALL TO registry_clerk USING (true) WITH CHECK (true);
CREATE POLICY dpo_all_consent ON gdpr.consent
  FOR ALL TO dpo_officer USING (true) WITH CHECK (true);
CREATE POLICY dpo_all_dsr ON gdpr.data_subject_request
  FOR ALL TO dpo_officer USING (true) WITH CHECK (true);
CREATE POLICY dpo_all_disclosure ON gdpr.disclosure_register
  FOR ALL TO dpo_officer USING (true) WITH CHECK (true);
CREATE POLICY citizen_own_consent ON gdpr.consent
  FOR SELECT TO registry_citizen
  USING (registry.citizen_portal_uid_accessible(person_uid));
CREATE POLICY citizen_own_dsr_read ON gdpr.data_subject_request
  FOR SELECT TO registry_citizen
  USING (registry.citizen_portal_uid_accessible(person_uid));
-- a representative may file a DSR for the represented person (Art. 12)
CREATE POLICY citizen_own_dsr_file ON gdpr.data_subject_request
  FOR INSERT TO registry_citizen
  WITH CHECK (registry.citizen_portal_uid_accessible(person_uid));
CREATE POLICY citizen_own_disclosure ON gdpr.disclosure_register
  FOR SELECT TO registry_citizen
  USING (registry.citizen_portal_uid_accessible(person_uid));

-- transparency: citizen sees who accessed their data (Art. 15)
ALTER TABLE audit.access_log ENABLE ROW LEVEL SECURITY;
CREATE POLICY app_insert_access_log ON audit.access_log
  FOR INSERT TO registry_app WITH CHECK (true);
CREATE POLICY dpo_read_access_log ON audit.access_log
  FOR SELECT TO dpo_officer USING (true);
CREATE POLICY secaudit_read_access_log ON audit.access_log
  FOR SELECT TO security_auditor USING (true);
CREATE POLICY citizen_own_access_log ON audit.access_log
  FOR SELECT TO registry_citizen
  USING (registry.citizen_portal_uid_accessible(person_uid));
-- close the direct-partition path: RLS policies live on the parent; access
-- must go through it (future partitions get no direct grants either)
REVOKE ALL ON audit.access_log_default FROM dpo_officer, security_auditor, registry_app;

-- ----------------------------------------------------------------------------
-- 11. Minimal-exposure view for other state agencies
--     No religion, no health data, no national_id ciphertext, no history.
--     View owner must be a role with table access; runs with owner rights.
-- ----------------------------------------------------------------------------
CREATE VIEW registry.v_person_current WITH (security_barrier) AS
SELECT
  p.person_uid,
  d.given_names,
  d.family_name,
  d.birth_surname,
  d.sex,
  d.date_of_birth,
  d.date_of_birth_precision,
  d.citizenship,
  a.street,
  a.house_number,
  a.postal_code,
  a.municipality_code,
  a.country_code,
  EXISTS (SELECT 1 FROM registry.death_record dr
          WHERE dr.person_uid = p.person_uid
            AND dr.superseded_at IS NULL) AS deceased
FROM registry.person p
JOIN registry.person_detail d
  ON d.person_uid = p.person_uid
 AND d.superseded_at IS NULL
 AND daterange(d.valid_from, d.valid_to) @> current_date
LEFT JOIN LATERAL (
  SELECT ad.*
  FROM registry.person_address pa
  JOIN registry.address ad ON ad.address_id = pa.address_id
  WHERE pa.person_uid = p.person_uid
    AND pa.address_type = 'permanent'
    AND pa.superseded_at IS NULL
    AND daterange(pa.valid_from, pa.valid_to) @> current_date
) a ON true
WHERE NOT p.anonymized
  -- merged duplicates are hidden; consumers see only canonical records
  AND NOT EXISTS (SELECT 1 FROM registry.person_merge pm
                  WHERE pm.duplicate_uid = p.person_uid
                    AND pm.superseded_at IS NULL);

-- ----------------------------------------------------------------------------
-- 12. Grants (deny by default; grant per role)
-- ----------------------------------------------------------------------------
GRANT USAGE ON SCHEMA codelist TO registry_app, registry_clerk,
  registry_supervisor, agency_reader, dpo_officer;
GRANT USAGE ON SCHEMA registry TO registry_app, registry_clerk,
  registry_supervisor, agency_reader, dpo_officer;
GRANT USAGE ON SCHEMA gdpr  TO registry_app, dpo_officer, registry_clerk;
GRANT USAGE ON SCHEMA audit TO registry_app, dpo_officer, security_auditor;
GRANT USAGE ON SCHEMA registry, gdpr, audit, codelist TO registry_citizen;

-- codelists: read for all working roles, write for supervisor only
GRANT SELECT ON ALL TABLES IN SCHEMA codelist
  TO registry_app, registry_clerk, registry_supervisor, agency_reader, dpo_officer;
GRANT INSERT, UPDATE ON ALL TABLES IN SCHEMA codelist TO registry_supervisor;

-- clerks: work on persons + records (RLS narrows records to own office);
-- UPDATE is required only to close bitemporal rows (superseded_at/_by)
GRANT SELECT, INSERT, UPDATE ON
  registry.person, registry.person_detail, registry.address,
  registry.person_address, registry.person_religion,
  registry.birth_record, registry.marriage_record,
  registry.marriage_dissolution, registry.death_record, registry.death_medical
  TO registry_clerk;
GRANT SELECT ON registry.staff_user TO registry_clerk;
GRANT USAGE ON ALL SEQUENCES IN SCHEMA registry TO registry_clerk;
-- clerks record consents at registration (e.g. religion, Art. 9(2)(a))
GRANT SELECT, INSERT ON gdpr.consent TO registry_clerk;
GRANT USAGE ON ALL SEQUENCES IN SCHEMA gdpr TO registry_clerk;
-- capacity restrictions and representations are registered by clerks from
-- court decisions / birth registrations
GRANT SELECT, INSERT, UPDATE ON
  registry.legal_capacity_restriction, registry.person_representation
  TO registry_clerk;
-- identity merges are a supervisor-level operation (clerks may read)
GRANT SELECT ON registry.person_merge TO registry_clerk;
GRANT SELECT, INSERT, UPDATE ON registry.person_merge TO registry_supervisor;

-- supervisors: clerk rights nationwide + staff administration
GRANT registry_clerk TO registry_supervisor;
GRANT SELECT, INSERT, UPDATE ON registry.staff_user TO registry_supervisor;

-- agencies: current-state view only; religion/health tables NOT granted
GRANT SELECT ON registry.v_person_current TO agency_reader;

-- DPO: read everything, run the GDPR workflow, read audit; no registry writes
GRANT SELECT ON ALL TABLES IN SCHEMA registry TO dpo_officer;
GRANT SELECT, INSERT, UPDATE ON ALL TABLES IN SCHEMA gdpr TO dpo_officer;
GRANT USAGE ON ALL SEQUENCES IN SCHEMA gdpr TO dpo_officer;
GRANT SELECT ON audit.audit_log, audit.access_log TO dpo_officer;

-- security auditor: logs only, no personal-data tables
GRANT SELECT ON audit.audit_log, audit.access_log,
  audit.security_event, audit.chain_head TO security_auditor;
GRANT INSERT ON audit.security_event TO registry_app;

-- citizen self-service portal: read-only on own data (narrowed by RLS to
-- rows where person_uid / participant = app.person_uid), may file own DSRs
GRANT SELECT ON ALL TABLES IN SCHEMA codelist TO registry_citizen;
GRANT SELECT ON
  registry.person, registry.person_detail, registry.address,
  registry.person_address, registry.person_religion,
  registry.birth_record, registry.marriage_record,
  registry.marriage_dissolution, registry.death_record,
  registry.legal_capacity_restriction, registry.person_representation
  TO registry_citizen;
GRANT SELECT ON gdpr.consent, gdpr.disclosure_register TO registry_citizen;
GRANT SELECT, INSERT ON gdpr.data_subject_request TO registry_citizen;
GRANT USAGE ON ALL SEQUENCES IN SCHEMA gdpr TO registry_citizen;
GRANT SELECT ON audit.access_log TO registry_citizen;

-- application service role: acts via SET ROLE to the end user's group role;
-- direct rights limited to logging
GRANT INSERT ON audit.access_log TO registry_app;
GRANT USAGE ON ALL SEQUENCES IN SCHEMA audit TO registry_app;

-- audit log is append-only for everyone below superuser
REVOKE UPDATE, DELETE, TRUNCATE ON ALL TABLES IN SCHEMA audit FROM PUBLIC;

COMMIT;
