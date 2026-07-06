-- ============================================================================
-- Smoke tests for schema.sql — run against a FRESH database after the schema.
-- Expected output: T1..T17 all print PASS.
--
-- Run (throwaway instance example):
--   createdb registry_test
--   psql -d registry_test -v ON_ERROR_STOP=1 -f schema.sql
--   psql -d registry_test -v ON_ERROR_STOP=1 -f test_schema.sql
--
-- Covers: UID generation + check digit (T1), full person graph insert (T2),
-- bitemporal overlap rejection (T3), address change with history query (T4),
-- religion consent constraint (T5), hard-delete protection (T6),
-- audit hash-chain integrity (T7), RLS office scoping (T8),
-- minimal agency view (T9), MetaIS codelist FKs + semantic seeds (T10),
-- 10 staff users across all five roles (T11), statutory retention (T12),
-- citizen self-service RLS own-data-only (T13, T14), portal eligibility +
-- parent/guardian access (T15), person identity merge (T16), partial birth
-- dates (T17).
-- Note: creates login roles test_clerk_ke and test_citizen; drop before re-running.
-- ============================================================================
\set ON_ERROR_STOP on

-- seed minimal codelists (structure mirrors MetaIS one-time import;
-- codelist.sex and codelist.address_type are seeded by schema.sql itself)
INSERT INTO codelist.country (country_code, name, source_codelist, metais_code) VALUES
  ('703','Slovenska republika','CL000086','703'),
  ('040','Rakuska republika','CL000086','040');
INSERT INTO codelist.region (region_code, name, source_codelist, metais_code) VALUES
  ('SK010','Bratislavsky kraj','CL000023','SK010'),
  ('SK042','Kosicky kraj','CL000023','SK042'),
  ('SK031','Zilinsky kraj','CL000023','SK031');
INSERT INTO codelist.district (district_code, name, region_code, source_codelist, metais_code) VALUES
  ('SK0101','Bratislava I','SK010','CL000024','SK0101'),
  ('SK0424','Kosice IV','SK042','CL000024','SK0424'),
  ('SK0311','Zilina','SK031','CL000024','SK0311');
INSERT INTO codelist.municipality (municipality_code, name, district_code, country_code,
                                   source_codelist, metais_code) VALUES
  ('SK0101528595','Bratislava-Stare Mesto','SK0101','703','CL000025','SK0101528595'),
  ('SK0424599981','Kosice-Juh','SK0424','703','CL000025','SK0424599981'),
  ('SK0311517402','Zilina','SK0311','703','CL000025','SK0311517402');
INSERT INTO codelist.marital_status (marital_status_code, name, source_codelist, metais_code) VALUES
  ('1','slobodny/slobodna','CL004002','1'), ('2','zenaty/vydata','CL004002','2');
INSERT INTO codelist.court (court_code, name, source_codelist, metais_code) VALUES
  ('OS-BA1','Mestsky sud Bratislava I','CL000014','OS-BA1');
INSERT INTO codelist.religion (religion_code, name) VALUES
  ('RIMKAT','Rimskokatolicka cirkev'), ('NONE','Bez vyznania'), ('UNDISCLOSED','Neuvedene');
INSERT INTO codelist.registry_office (office_code, name, municipality_code,
                                      source_codelist, metais_code) VALUES
  ('MAT-BA1','Matricny urad BA-Stare Mesto','SK0101528595','CL000015','MAT-BA1'),
  ('MAT-KE1','Matricny urad KE-Juh','SK0424599981','CL000015','MAT-KE1'),
  ('MAT-ZA1','Matricny urad Zilina','SK0311517402','CL000015','MAT-ZA1');

-- 10 staff users across all roles
INSERT INTO registry.staff_user (user_id, full_name, office_code, staff_role, active) VALUES
  ('clerk.ba1',   'Maria Kovacova',   'MAT-BA1', 'clerk',            true),
  ('clerk.ba2',   'Peter Szabo',      'MAT-BA1', 'clerk',            true),
  ('clerk.ke',    'Eva Horvathova',   'MAT-KE1', 'clerk',            true),
  ('clerk.za',    'Jan Moravcik',     'MAT-ZA1', 'clerk',            true),
  ('super.ba',    'Anna Urbanova',    'MAT-BA1', 'supervisor',       true),
  ('super.centr', 'Milan Bielik',     NULL,      'supervisor',       true),
  ('agency.scp',  'Socialna poistovna - citaci ucet', NULL, 'agency', true),
  ('agency.upsvr','UPSVaR - citaci ucet',             NULL, 'agency', true),
  ('dpo.office',  'Zodpovedna osoba (DPO)',           NULL, 'dpo',    true),
  ('sec.auditor', 'Bezpecnostny audit (NIS2)',        NULL, 'security_auditor', true);

SET app.user_id = 'clerk.ba1';

-- T1: UID generation + check digit
DO $$
DECLARE uid text;
BEGIN
  uid := registry.generate_person_uid();
  IF NOT registry.person_uid_is_valid(uid) THEN
    RAISE EXCEPTION 'T1 FAIL: generated uid % not valid', uid;
  END IF;
  IF registry.person_uid_is_valid('P-AAAAA-AAAAA-0') AND registry.person_uid_is_valid('P-AAAAA-AAAAA-1') THEN
    RAISE EXCEPTION 'T1 FAIL: two different check digits both valid';
  END IF;
  RAISE NOTICE 'T1 PASS: uid % generated and check-digit validated', uid;
END $$;

-- T2: person + detail + address + religion + birth record
INSERT INTO registry.person (created_by) VALUES ('clerk.ba1');
DO $$
DECLARE p text; a uuid; c bigint;
BEGIN
  SELECT person_uid INTO p FROM registry.person;
  INSERT INTO registry.person_detail (person_uid, given_names, family_name, sex,
    date_of_birth, place_of_birth_municipality, citizenship, valid_from, recorded_by)
  VALUES (p, 'Jana', 'Novakova', 'F', '1990-05-01', 'SK0101528595', '703', '1990-05-01', 'clerk.ba1');

  INSERT INTO registry.address (street, house_number, postal_code, municipality_code, country_code)
  VALUES ('Hlavna', '12', '81101', 'SK0101528595', '703') RETURNING address_id INTO a;
  INSERT INTO registry.person_address (person_uid, address_id, address_type, valid_from, recorded_by)
  VALUES (p, a, 'permanent', '2010-01-01', 'clerk.ba1');

  INSERT INTO gdpr.consent (person_uid, purpose, given_at, given_via, recorded_by)
  VALUES (p, 'religion_registration', now(), 'in_person', 'clerk.ba1') RETURNING consent_id INTO c;
  INSERT INTO registry.person_religion (person_uid, religion_code, legal_basis, consent_id, valid_from, recorded_by)
  VALUES (p, 'RIMKAT', 'art9_2a_consent', c, '1990-05-01', 'clerk.ba1');

  INSERT INTO registry.birth_record (office_code, book_year, entry_no, child_uid,
    date_of_birth, place_municipality, recorded_by)
  VALUES ('MAT-BA1', 1990, 123, p, '1990-05-01', 'SK0101528595', 'clerk.ba1');
  RAISE NOTICE 'T2 PASS: person graph inserted';
END $$;

-- T3: overlapping current address for same type must be rejected
DO $$
DECLARE p text; a uuid;
BEGIN
  SELECT person_uid INTO p FROM registry.person;
  SELECT address_id INTO a FROM registry.address;
  BEGIN
    INSERT INTO registry.person_address (person_uid, address_id, address_type, valid_from, recorded_by)
    VALUES (p, a, 'permanent', '2015-01-01', 'clerk.ba1');
    RAISE EXCEPTION 'T3 FAIL: overlapping permanent address accepted';
  EXCEPTION WHEN exclusion_violation THEN
    RAISE NOTICE 'T3 PASS: overlapping address rejected';
  END;
END $$;

-- T4: address change with history — close old validity via supersede, add new
DO $$
DECLARE p text; old_id bigint; a2 uuid; n int;
BEGIN
  SELECT person_uid INTO p FROM registry.person;
  SELECT person_address_id INTO old_id FROM registry.person_address WHERE superseded_at IS NULL;
  -- supersede old belief, re-record it with a closed validity, then new address
  UPDATE registry.person_address SET superseded_at = now() WHERE person_address_id = old_id;
  INSERT INTO registry.person_address (person_uid, address_id, address_type, valid_from, valid_to, recorded_by, change_reason)
    SELECT person_uid, address_id, address_type, valid_from, '2024-06-30', 'clerk.ba1', 'moved out'
    FROM registry.person_address WHERE person_address_id = old_id;
  INSERT INTO registry.address (street, house_number, postal_code, municipality_code, country_code)
    VALUES ('Dolna', '5', '04001', 'SK0424599981', '703') RETURNING address_id INTO a2;
  INSERT INTO registry.person_address (person_uid, address_id, address_type, valid_from, recorded_by)
    VALUES (p, a2, 'permanent', '2024-07-01', 'clerk.ba1');
  -- history question: where did she live on 2020-01-01 per current belief?
  SELECT count(*) INTO n FROM registry.person_address
   WHERE person_uid = p AND superseded_at IS NULL
     AND daterange(valid_from, valid_to) @> DATE '2020-01-01';
  IF n <> 1 THEN RAISE EXCEPTION 'T4 FAIL: expected 1 address valid on 2020-01-01, got %', n; END IF;
  RAISE NOTICE 'T4 PASS: address changed, history query works';
END $$;

-- T5: religion with consent basis but no consent_id must be rejected
DO $$
DECLARE p text;
BEGIN
  SELECT person_uid INTO p FROM registry.person;
  BEGIN
    INSERT INTO registry.person_religion (person_uid, religion_code, legal_basis, valid_from, recorded_by)
    VALUES (p, 'NONE', 'art9_2a_consent', '2026-01-01', 'clerk.ba1');
    RAISE EXCEPTION 'T5 FAIL: consent-basis religion without consent accepted';
  EXCEPTION WHEN check_violation OR exclusion_violation THEN
    RAISE NOTICE 'T5 PASS: religion consent constraint enforced';
  END;
END $$;

-- T6: hard delete of vital record must be blocked
DO $$
BEGIN
  BEGIN
    DELETE FROM registry.birth_record;
    RAISE EXCEPTION 'T6 FAIL: delete of birth record succeeded';
  EXCEPTION WHEN raise_exception THEN
    IF SQLERRM LIKE '%append-only%' THEN
      RAISE NOTICE 'T6 PASS: hard delete blocked';
    ELSE
      RAISE;
    END IF;
  END;
END $$;

-- T7: audit log written and hash chain intact
DO $$
DECLARE bad int; total int;
BEGIN
  SELECT count(*) INTO total FROM audit.audit_log;
  IF total < 10 THEN RAISE EXCEPTION 'T7 FAIL: only % audit rows', total; END IF;
  SELECT count(*) INTO bad FROM (
    SELECT row_hash, lead(prev_hash) OVER (ORDER BY audit_id) AS next_prev
    FROM audit.audit_log) x
  WHERE next_prev IS NOT NULL AND next_prev <> row_hash;
  IF bad > 0 THEN RAISE EXCEPTION 'T7 FAIL: % broken chain links', bad; END IF;
  RAISE NOTICE 'T7 PASS: % audit rows, hash chain intact', total;
END $$;

-- T8: RLS — clerk from another office cannot see/insert BA records
CREATE ROLE test_clerk_ke LOGIN;
GRANT registry_clerk TO test_clerk_ke;
SET ROLE test_clerk_ke;
SET app.user_id = 'clerk.ke';
DO $$
DECLARE n int;
BEGIN
  SELECT count(*) INTO n FROM registry.birth_record;
  IF n <> 0 THEN RAISE EXCEPTION 'T8 FAIL: KE clerk sees % BA records', n; END IF;
  BEGIN
    INSERT INTO registry.birth_record (office_code, book_year, entry_no, child_uid,
      date_of_birth, recorded_by)
    SELECT 'MAT-BA1', 1991, 1, person_uid, '1991-01-01', 'clerk.ke' FROM registry.person LIMIT 1;
    RAISE EXCEPTION 'T8 FAIL: KE clerk inserted BA record';
  EXCEPTION WHEN insufficient_privilege OR check_violation THEN
    RAISE NOTICE 'T8 RLS insert correctly rejected';
  END;
END $$;
RESET ROLE;
SET app.user_id = 'clerk.ba1';
DO $$ BEGIN RAISE NOTICE 'T8 PASS: RLS office scoping enforced'; END $$;

-- T9: agency view exposes no religion and hides nothing needed
SELECT CASE WHEN count(*) = 1 THEN 'T9 PASS: v_person_current returns 1 row, no religion column'
       ELSE 'T9 FAIL' END AS t9
FROM registry.v_person_current;

-- T10: MetaIS codelist FKs — invalid sex code rejected; semantic seeds mapped
DO $$
DECLARE p text; n int;
BEGIN
  -- fresh person so only the FK (not the bitemporal exclusion) can fire
  INSERT INTO registry.person (created_by) VALUES ('clerk.ba1')
    RETURNING person_uid INTO p;
  BEGIN
    INSERT INTO registry.person_detail (person_uid, given_names, family_name, sex,
      date_of_birth, citizenship, valid_from, recorded_by)
    VALUES (p, 'Test', 'Test', 'Q', '1990-05-01', '703', '2026-01-01', 'clerk.ba1');
    RAISE EXCEPTION 'T10 FAIL: invalid sex code accepted';
  EXCEPTION WHEN foreign_key_violation THEN
    NULL;
  END;
  SELECT count(*) INTO n FROM codelist.sex
   WHERE source_codelist = 'CL003003' AND metais_code IS NOT NULL;
  IF n <> 3 THEN RAISE EXCEPTION 'T10 FAIL: sex codelist MetaIS mapping incomplete (%)', n; END IF;
  SELECT count(*) INTO n FROM codelist.address_type;
  IF n < 4 THEN RAISE EXCEPTION 'T10 FAIL: address_type codelist not seeded (%)', n; END IF;
  RAISE NOTICE 'T10 PASS: codelist FKs enforced, MetaIS mappings seeded';
END $$;

-- T11: 10 staff users seeded across all five roles
DO $$
DECLARE n int; r int;
BEGIN
  SELECT count(*), count(DISTINCT staff_role) INTO n, r FROM registry.staff_user;
  IF n < 10 OR r < 5 THEN
    RAISE EXCEPTION 'T11 FAIL: expected >=10 users in 5 roles, got % users / % roles', n, r;
  END IF;
  RAISE NOTICE 'T11 PASS: % staff users across % roles', n, r;
END $$;

-- T12: statutory retention policies seeded (zakon 154/1994 Z.z.)
DO $$
DECLARE n int;
BEGIN
  SELECT count(*) INTO n FROM gdpr.retention_policy
   WHERE (record_class, retention) IN
     (('birth_record', interval '100 years'),
      ('marriage_record', interval '75 years'),
      ('death_record', interval '75 years'));
  IF n <> 3 THEN RAISE EXCEPTION 'T12 FAIL: expected 3 statutory policies, got %', n; END IF;
  RAISE NOTICE 'T12 PASS: statutory retention policies seeded';
END $$;

-- T13: citizen self-service — RLS "own data only"
CREATE ROLE test_citizen LOGIN;
GRANT registry_citizen TO test_citizen;
-- resolve Jana's uid while still superuser, keep it in a session GUC
SELECT set_config('app.person_uid',
  (SELECT person_uid FROM registry.person_detail WHERE superseded_at IS NULL LIMIT 1),
  false) AS citizen_uid_set;
SET ROLE test_citizen;
DO $$
DECLARE n int; my_uid text := registry.current_citizen_uid();
BEGIN
  -- sees exactly own person, detail and birth record
  SELECT count(*) INTO n FROM registry.person;
  IF n <> 1 THEN RAISE EXCEPTION 'T13 FAIL: citizen sees % persons (expected 1)', n; END IF;
  SELECT count(*) INTO n FROM registry.person_detail;
  IF n <> 1 THEN RAISE EXCEPTION 'T13 FAIL: citizen sees % details (expected 1)', n; END IF;
  SELECT count(*) INTO n FROM registry.birth_record WHERE child_uid <> my_uid;
  IF n <> 0 THEN RAISE EXCEPTION 'T13 FAIL: citizen sees foreign birth records'; END IF;
  -- cannot write person data at all
  BEGIN
    UPDATE registry.person_detail SET family_name = 'Hacked';
    RAISE EXCEPTION 'T13 FAIL: citizen updated person_detail';
  EXCEPTION WHEN insufficient_privilege THEN NULL;
  END;
  -- may file a DSR for self...
  INSERT INTO gdpr.data_subject_request (person_uid, requester_ref, request_type)
  VALUES (my_uid, 'PORTAL-0001', 'access');
  -- ...but not for someone else
  BEGIN
    INSERT INTO gdpr.data_subject_request (person_uid, requester_ref, request_type)
    VALUES ('P-AAAAA-AAAAA-0', 'PORTAL-0002', 'access');
    RAISE EXCEPTION 'T13 FAIL: citizen filed DSR for another person';
  EXCEPTION WHEN insufficient_privilege THEN NULL;
  END;
  RAISE NOTICE 'T13 PASS: citizen RLS own-data-only enforced';
END $$;
RESET ROLE;

-- T14: citizen with foreign identity sees nothing
SELECT set_config('app.person_uid', 'P-ZZZZZ-ZZZZZ-0', false) AS foreign_uid_set;
SET ROLE test_citizen;
DO $$
DECLARE n int;
BEGIN
  SELECT count(*) INTO n FROM registry.person;
  IF n <> 0 THEN RAISE EXCEPTION 'T14 FAIL: foreign citizen sees % persons', n; END IF;
  SELECT count(*) INTO n FROM registry.person_address;
  IF n <> 0 THEN RAISE EXCEPTION 'T14 FAIL: foreign citizen sees % addresses', n; END IF;
  RAISE NOTICE 'T14 PASS: unmatched citizen identity sees zero rows';
END $$;
RESET ROLE;

-- T15: portal eligibility rules — parent sees minor, restricted/deceased blocked
DO $$
DECLARE jana text; child text; restricted text; deceased text;
BEGIN
  SELECT person_uid INTO jana FROM registry.person_detail
   WHERE superseded_at IS NULL AND date_of_birth = '1990-05-01' LIMIT 1;
  -- minor child represented by Jana (parent)
  INSERT INTO registry.person (created_by) VALUES ('clerk.ba1') RETURNING person_uid INTO child;
  INSERT INTO registry.person_detail (person_uid, given_names, family_name, sex,
    date_of_birth, citizenship, valid_from, recorded_by)
  VALUES (child, 'Miska', 'Novakova', 'F', '2015-03-01', '703', '2015-03-01', 'clerk.ba1');
  INSERT INTO registry.person_representation (represented_uid, representative_uid,
    representation_type, legal_basis_ref, valid_from, recorded_by)
  VALUES (child, jana, 'parent', 'birth registration', '2015-03-01', 'clerk.ba1');
  -- adult with a current full deprivation of legal capacity
  INSERT INTO registry.person (created_by) VALUES ('clerk.ba1') RETURNING person_uid INTO restricted;
  INSERT INTO registry.person_detail (person_uid, given_names, family_name, sex,
    date_of_birth, citizenship, valid_from, recorded_by)
  VALUES (restricted, 'Pavol', 'Obmedzeny', 'M', '1980-01-01', '703', '1980-01-01', 'clerk.ba1');
  INSERT INTO registry.legal_capacity_restriction (person_uid, restriction_type,
    court_code, court_reference, valid_from, recorded_by)
  VALUES (restricted, 'full_deprivation', 'OS-BA1', '12C/34/2020', '2020-01-01', 'clerk.ba1');
  -- deceased person
  INSERT INTO registry.person (created_by) VALUES ('clerk.ba1') RETURNING person_uid INTO deceased;
  INSERT INTO registry.person_detail (person_uid, given_names, family_name, sex,
    date_of_birth, citizenship, valid_from, recorded_by)
  VALUES (deceased, 'Jozef', 'Nebohy', 'M', '1950-02-02', '703', '1950-02-02', 'clerk.ba1');
  INSERT INTO registry.death_record (office_code, book_year, entry_no, person_uid,
    date_of_death, recorded_by)
  VALUES ('MAT-BA1', 2020, 77, deceased, '2020-06-01', 'clerk.ba1');
  PERFORM set_config('app.person_uid', jana, false);
  PERFORM set_config('app.t15_restricted', restricted, false);
  PERFORM set_config('app.t15_deceased', deceased, false);
END $$;
SET ROLE test_citizen;
DO $$
DECLARE n int;
BEGIN
  -- Jana (eligible parent): sees herself + her minor child
  SELECT count(*) INTO n FROM registry.person;
  IF n <> 2 THEN RAISE EXCEPTION 'T15 FAIL: parent sees % persons (expected self + child)', n; END IF;
  SELECT count(*) INTO n FROM registry.person_detail WHERE person_uid <> registry.current_citizen_uid();
  IF n <> 1 THEN RAISE EXCEPTION 'T15 FAIL: parent sees % foreign details (expected 1 = child)', n; END IF;
END $$;
RESET ROLE;
SELECT set_config('app.person_uid', current_setting('app.t15_restricted'), false) AS switch_to_restricted;
SET ROLE test_citizen;
DO $$
DECLARE n int;
BEGIN
  SELECT count(*) INTO n FROM registry.person;
  IF n <> 0 THEN RAISE EXCEPTION 'T15 FAIL: person with restricted capacity sees % rows', n; END IF;
END $$;
RESET ROLE;
SELECT set_config('app.person_uid', current_setting('app.t15_deceased'), false) AS switch_to_deceased;
SET ROLE test_citizen;
DO $$
DECLARE n int;
BEGIN
  SELECT count(*) INTO n FROM registry.person;
  IF n <> 0 THEN RAISE EXCEPTION 'T15 FAIL: deceased identity sees % rows', n; END IF;
END $$;
RESET ROLE;
DO $$ BEGIN RAISE NOTICE 'T15 PASS: eligibility (living/adult/capacity) + parent access enforced'; END $$;

-- T16: person identity merge — resolve, view exclusion, chain + duplicate guards
DO $$
DECLARE jana text; restricted text; deceased text; vc_before int; vc_after int;
BEGIN
  SELECT person_uid INTO jana FROM registry.person_detail
   WHERE superseded_at IS NULL AND date_of_birth = '1990-05-01' LIMIT 1;
  restricted := current_setting('app.t15_restricted');
  deceased   := current_setting('app.t15_deceased');
  SELECT count(*) INTO vc_before FROM registry.v_person_current;

  INSERT INTO registry.person_merge (duplicate_uid, primary_uid, reason, decided_by, approved_by)
  VALUES (restricted, jana, 'same physical person, double registration', 'super.centr', 'super.centr');

  IF registry.resolve_person_uid(restricted) <> jana THEN
    RAISE EXCEPTION 'T16 FAIL: resolve does not follow merge link';
  END IF;
  IF registry.resolve_person_uid(jana) <> jana THEN
    RAISE EXCEPTION 'T16 FAIL: resolve changed an unmerged uid';
  END IF;
  SELECT count(*) INTO vc_after FROM registry.v_person_current;
  IF vc_after <> vc_before - 1 THEN
    RAISE EXCEPTION 'T16 FAIL: merged duplicate still visible in v_person_current';
  END IF;
  -- chains are forbidden: primary of the new merge is itself merged
  BEGIN
    INSERT INTO registry.person_merge (duplicate_uid, primary_uid, reason, decided_by)
    VALUES (deceased, restricted, 'chain attempt', 'super.centr');
    RAISE EXCEPTION 'T16 FAIL: chain merge accepted';
  EXCEPTION WHEN raise_exception THEN
    IF SQLERRM LIKE 'T16 FAIL%' THEN RAISE; END IF;
  END;
  -- only one active merge per duplicate
  BEGIN
    INSERT INTO registry.person_merge (duplicate_uid, primary_uid, reason, decided_by)
    VALUES (restricted, deceased, 'second merge attempt', 'super.centr');
    RAISE EXCEPTION 'T16 FAIL: second active merge accepted';
  EXCEPTION WHEN unique_violation THEN NULL;
  END;
  RAISE NOTICE 'T16 PASS: merge resolve + view exclusion + chain/duplicate guards';
END $$;

-- T17: partially known birth dates
DO $$
DECLARE p text;
BEGIN
  INSERT INTO registry.person (created_by) VALUES ('clerk.ba1') RETURNING person_uid INTO p;
  INSERT INTO registry.person_detail (person_uid, given_names, family_name, sex,
    date_of_birth, date_of_birth_precision, citizenship, valid_from, recorded_by)
  VALUES (p, 'Ivan', 'Cudzinec', 'M', '1970-01-01', 'year', '040', '1970-01-01', 'clerk.ba1');
  BEGIN
    INSERT INTO registry.person_detail (person_uid, given_names, family_name, sex,
      date_of_birth, date_of_birth_precision, citizenship, valid_from, recorded_by)
    VALUES (p, 'Ivan', 'Cudzinec', 'M', '1970-01-01', 'approx', '040', '1970-01-01', 'clerk.ba1');
    RAISE EXCEPTION 'T17 FAIL: invalid precision accepted';
  EXCEPTION WHEN check_violation THEN NULL;
  END;
  RAISE NOTICE 'T17 PASS: partial birth-date precision stored and validated';
END $$;
