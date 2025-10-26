/*
package org.example.runningapp.config.database;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.util.StringUtils;

@Configuration
public class RedisConfig {

	@Value("${spring.data.redis.host:localhost}")  // 경로 수정
	private String redisHost;

	@Value("${spring.data.redis.port:6379}")       // 경로 수정
	private int redisPort;

	@Value("${spring.data.redis.password:}")       // 비밀번호 추가
	private String redisPassword;

	@Bean
	public RedisConnectionFactory redisConnectionFactory() {
		// 보안 연결을 위해 RedisStandaloneConfiguration 사용
		RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
		redisConfig.setHostName(redisHost);
		redisConfig.setPort(redisPort);

		// 비밀번호가 있는 경우에만 설정
		if (StringUtils.hasText(redisPassword)) {
			redisConfig.setPassword(redisPassword);
		}

		return new LettuceConnectionFactory(redisConfig);
	}

	@Bean
	public RedisTemplate<String, String> redisTemplate() {
		RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();
		redisTemplate.setConnectionFactory(redisConnectionFactory());

		// 보안을 위해 직렬화/역직렬화를 명시적으로 설정
		redisTemplate.setKeySerializer(new StringRedisSerializer());
		redisTemplate.setValueSerializer(new StringRedisSerializer());
		redisTemplate.setHashKeySerializer(new StringRedisSerializer());
		redisTemplate.setHashValueSerializer(new StringRedisSerializer());

		// 기본 직렬화기 설정
		redisTemplate.setDefaultSerializer(new StringRedisSerializer());
		redisTemplate.setEnableDefaultSerializer(true);

		return redisTemplate;
	}
}
*/