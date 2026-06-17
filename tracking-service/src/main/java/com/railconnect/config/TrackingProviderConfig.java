package com.railconnect.config;

import com.railconnect.service.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class TrackingProviderConfig {

    @Bean
    @Primary
    @ConditionalOnProperty(name = "tracking.provider", havingValue = "simulated", matchIfMissing = true)
    public TrackingProvider simulatedTrackingProviderBean(SimulatedTrackingProvider provider) {
        return provider;
    }

    @Bean
    @Primary
    @ConditionalOnProperty(name = "tracking.provider", havingValue = "external")
    public TrackingProvider externalTrackingProviderBean(ExternalTrackingProvider provider) {
        return provider;
    }
}
