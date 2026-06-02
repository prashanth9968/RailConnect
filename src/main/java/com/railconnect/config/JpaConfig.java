package com.railconnect.config;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;
@Configuration
@EnableJpaAuditing
@EnableJpaRepositories(basePackages = "com.railconnect.repository")
@EnableTransactionManagement
public class JpaConfig {}
