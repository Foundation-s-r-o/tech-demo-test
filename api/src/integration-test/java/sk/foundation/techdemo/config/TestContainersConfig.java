package sk.foundation.techdemo.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration
public class TestContainersConfig {

    @Bean
    public MariaDBContainer<?> mariadbContainer() {
        return new MariaDBContainer<>(DockerImageName.parse("mariadb:11.2"))
                .withDatabaseName("tech-demo-it")
                .withUsername("root")
                .withPassword("test")
                .withReuse(true);
    }
}