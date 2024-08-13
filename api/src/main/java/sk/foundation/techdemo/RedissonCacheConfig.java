package sk.foundation.techdemo;

import java.util.HashMap;
import java.util.Map;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.spring.cache.CacheConfig;
import org.redisson.spring.cache.RedissonSpringCacheManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
@ConditionalOnProperty(value = "caching.enabled", havingValue = "true")
public class RedissonCacheConfig {

	@Value("${caching.redis.host}")
	private String redisHost;

	@Value("${caching.redis.port}")
	private String redisPort;

	@Bean
	public RedissonClient redissonClient() {
		Config config = new Config();
		config.useSingleServer()
				.setAddress("redis://" + redisHost + ":" + redisPort);
		return Redisson.create(config);
	}

	@Bean
	public RedissonSpringCacheManager cacheManager(RedissonClient redissonClient) {
		Map<String, CacheConfig> config = new HashMap<>();

		// create "testMap" spring cache with ttl = 24 minutes and maxIdleTime = 12 minutes
		config.put("testMap", new CacheConfig((long) 24 * 60 * 1000,
				(long) 12 * 60 * 1000));
		return new RedissonSpringCacheManager(redissonClient, config);
	}

}
