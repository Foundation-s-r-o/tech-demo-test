package sk.foundation.techdemo.infrastructure.db;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
