insert into PERSON (ID, FIRST_NAME, LAST_NAME, EMAIL) values
	(1, 'Admin', 'Admin', 'admin@tech-demo.sk'),
	(2, 'DruhyFN', 'DruhyLN', 'druhy@tech-demo.sk');

-- Seed uses explicit IDs for deterministic test references; H2 does not advance the
-- identity counter on explicit inserts, so restart it past the seeded rows to avoid
-- PK collisions when a test persists a new Person. (Test-fixture only; H2 syntax.)
alter table PERSON alter column ID restart with 3;
