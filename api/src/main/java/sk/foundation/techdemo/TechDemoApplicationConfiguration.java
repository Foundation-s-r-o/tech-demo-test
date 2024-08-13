package sk.foundation.techdemo;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EntityScan("sk.foundation.techdemo.*")
@EnableJpaRepositories(basePackages = {
		"com.vladmihalcea.spring.repository",
		"sk.foundation.techdemo"
})
public class TechDemoApplicationConfiguration {

}
