package sk.foundation.techdemo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import sk.foundation.techdemo.config.TestContainersConfig;

@SpringBootTest(
		classes = TechDemoApplication.class,
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("it")
@Import(TestContainersConfig.class)
@Testcontainers
public abstract class BaseIT {

	@Autowired
	public MockMvc mockMvc;

	@Container
	static final MariaDBContainer<?> MARIADB_CONTAINER = new MariaDBContainer<>(DockerImageName.parse("mariadb:11.2"))
			.withDatabaseName("tech-demo-it")
			.withUsername("root")
			.withPassword("test")
			.withReuse(true);

	@DynamicPropertySource
	static void configureProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", MARIADB_CONTAINER::getJdbcUrl);
		registry.add("spring.datasource.username", MARIADB_CONTAINER::getUsername);
		registry.add("spring.datasource.password", MARIADB_CONTAINER::getPassword);
		registry.add("spring.datasource.driver-class-name", MARIADB_CONTAINER::getDriverClassName);
	}
}