package sk.foundation.techdemo.infrastructure.db;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import sk.foundation.techdemo.TechDemoTestConfiguration;

/**
 * Verifies Flyway applied the schema migrations against the embedded H2 database (no Docker).
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TechDemoTestConfiguration.class)
@ActiveProfiles("tc-flyway")
class FlywayMigrationIT {

	@Autowired
	private DataSource dataSource;

	@Test
	void schemaMigrationsWereAppliedSuccessfully() throws SQLException {
		try (Connection connection = dataSource.getConnection();
				Statement statement = connection.createStatement();
				ResultSet resultSet = statement.executeQuery(
						// Flyway creates its history table (and columns) lowercase-quoted; quote them
						// so H2 does not upper-case the unquoted identifiers and miss them.
						"select count(*) from \"flyway_schema_history\" where \"success\" = true")) {
			resultSet.next();
			assertTrue(resultSet.getInt(1) >= 1, "expected at least one successful Flyway migration");
		}
	}

	@Test
	void migrationsDoNotLeaveKnownAdminAccount() throws SQLException {
		try (Connection connection = dataSource.getConnection();
				Statement statement = connection.createStatement();
				ResultSet resultSet = statement.executeQuery(
						"select count(*) from USERS where USERNAME = 'admin'")) {
			resultSet.next();
			assertEquals(0, resultSet.getInt(1), "migrations must not provision admin/admin");
		}
	}

	@Test
	void registrySchemaObjectsExist() throws SQLException {
		// One representative table per schema plus the agency view: their queryability
		// proves V5 created all four schemas and the objects within them.
		assertEquals(0, count("select count(*) from REGISTRY.PERSON"));
		assertEquals(0, count("select count(*) from REGISTRY.BIRTH_RECORD"));
		assertEquals(0, count("select count(*) from GDPR.CONSENT"));
		assertEquals(0, count("select count(*) from AUDIT.AUDIT_LOG"));
		assertEquals(0, count("select count(*) from REGISTRY.V_PERSON_CURRENT"));
	}

	@Test
	void semanticAndStatutorySeedsArePresent() throws SQLException {
		assertEquals(4, count("select count(*) from CODELIST.ADDRESS_TYPE"));
		assertEquals(3, count("select count(*) from CODELIST.SEX"));
		assertEquals(10, count("select count(*) from CODELIST.RELIGION"));
		assertEquals(3, count("select count(*) from GDPR.RETENTION_POLICY"));
		// audit hash chain initialised with the genesis head row
		assertEquals(1, count("select count(*) from AUDIT.CHAIN_HEAD where ID = 1"));
	}

	@Test
	void metaisCodelistSeedWasImported() throws SQLException {
		// Counts pinned to db/import_codelists.py output (V6): a drifted seed fails here.
		assertEquals(252, count("select count(*) from CODELIST.COUNTRY"));
		assertEquals(2930, count("select count(*) from CODELIST.MUNICIPALITY"));
		assertEquals(977, count("select count(*) from CODELIST.REGISTRY_OFFICE"));
		// semantic mapping (V6 UPDATE) linked the sex codelist to its MetaIS codes
		assertEquals(3, count("select count(*) from CODELIST.SEX where METAIS_CODE is not null"));
	}

	@Test
	void foreignKeysAreEnforced() throws SQLException {
		// created_by references USERS(USERNAME) across schemas; a dangling actor must fail.
		try (Connection connection = dataSource.getConnection();
				Statement statement = connection.createStatement()) {
			assertThrows(SQLException.class, () -> statement.execute(
					"insert into REGISTRY.PERSON (PERSON_UID, ANONYMIZED, CREATED_BY) "
							+ "values ('P-00000-00000-0', false, 'no_such_user')"));
		}
	}

	private int count(String sql) throws SQLException {
		try (Connection connection = dataSource.getConnection();
				Statement statement = connection.createStatement();
				ResultSet resultSet = statement.executeQuery(sql)) {
			resultSet.next();
			return resultSet.getInt(1);
		}
	}
}
