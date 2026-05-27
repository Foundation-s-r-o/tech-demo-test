package sk.foundation.techdemo;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@TestConfiguration
@EnableJpaRepositories(basePackages = "sk.foundation.techdemo")
public class TechDemoTestConfiguration {
}
