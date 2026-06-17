package com.railconnect.config;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.transaction.annotation.EnableTransactionManagement;
@Configuration

@EnableTransactionManagement
public class JpaConfig {}
