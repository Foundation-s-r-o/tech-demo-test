package sk.foundation.techdemo.infrastructure.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.Arguments;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import sk.foundation.techdemo.TechDemoTestConfiguration;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TechDemoTestConfiguration.class)
@ActiveProfiles({ "tc-flyway" })
class FlywayMigrationIT {

	static final MySQLContainer<?> MYSQL_CONTAINER;

	static {
		MYSQL_CONTAINER = new MySQLContainer<>(DockerImageName.parse("mysql:8.0.30"))
				.withEnv("MYSQL_ROOT_PASSWORD", "test")
				.withEnv("MYSQL_ROOT_HOST", "%")
				.withDatabaseName("test")
				.withUsername("root")
				.withPassword("test")
				.withReuse(true);

		MYSQL_CONTAINER.start();
	}

	@DynamicPropertySource
	static void datasourceConfig(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", MYSQL_CONTAINER::getJdbcUrl);
		registry.add("spring.datasource.password", MYSQL_CONTAINER::getPassword);
		registry.add("spring.datasource.username", MYSQL_CONTAINER::getUsername);
	}

	static Stream<Arguments> flywayMigrationData() {
		return Stream.of(Arguments.of("test", 1));
	}

	@Test
	void schemaMigrationsWereAppliedSuccessfully()
			throws SQLException {
		int resultSetInt;
		ResultSet resultSet = performQuery(
				MYSQL_CONTAINER,
				"select count(*) from test.flyway_schema_history where success = 1");

		resultSetInt = resultSet.getInt(1);
		assertEquals(1, resultSetInt);

		assertTrue(true);
	}

	protected ResultSet performQuery(JdbcDatabaseContainer<?> container, String sql) throws SQLException {
		DataSource ds = getDataSource(container);
		ResultSet resultSet;
		Statement statement = ds.getConnection().createStatement();
		statement.execute(sql);
		resultSet = statement.getResultSet();

		resultSet.next();
		return resultSet;
	}

	protected DataSource getDataSource(JdbcDatabaseContainer<?> container) {
		HikariConfig hikariConfig = new HikariConfig();
		hikariConfig.setJdbcUrl(container.getJdbcUrl());
		hikariConfig.setUsername(container.getUsername());
		hikariConfig.setPassword(container.getPassword());
		hikariConfig.setDriverClassName(container.getDriverClassName());
		return new HikariDataSource(hikariConfig);
	}
}
