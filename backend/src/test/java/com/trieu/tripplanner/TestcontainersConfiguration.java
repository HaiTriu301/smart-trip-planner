package com.trieu.tripplanner;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Real MySQL and real Redis in Docker for tests; same versions as docker-compose.yml.
 * {@link ServiceConnection} wires the datasource and the Redis connection, so tests never touch the local dev
 * database nor a Redis that happens to run on the developer's machine.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

	static final int REDIS_PORT = 6379;

	@Bean
	@ServiceConnection
	MySQLContainer mysqlContainer() {
		return new MySQLContainer(DockerImageName.parse("mysql:8.0"));
	}

	// A generic container says nothing about what runs inside: the name tells Spring Boot it is Redis
	@Bean
	@ServiceConnection(name = "redis")
	GenericContainer<?> redisContainer() {
		return new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(REDIS_PORT);
	}

}
