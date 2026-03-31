package com.jean202.cardmizer.infra.persistence.jpa;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EntityScan(basePackages = "com.jean202.cardmizer.infra.persistence.jpa")
@EnableJpaRepositories(basePackages = "com.jean202.cardmizer.infra.persistence.jpa")
public class JpaPersistenceConfiguration {
}
