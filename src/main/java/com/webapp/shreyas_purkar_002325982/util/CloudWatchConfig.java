package com.webapp.shreyas_purkar_002325982.util;

import io.micrometer.cloudwatch2.CloudWatchMeterRegistry;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;

import java.time.Duration;
import java.util.Map;

@Configuration
public class CloudWatchConfig {

    @Value("${management.metrics.export.cloudwatch.region}")
    private String region;

    @Bean
    public CloudWatchAsyncClient cloudWatchAsyncClient() {
        return CloudWatchAsyncClient
                .builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    @Bean
    public MeterRegistry meterRegistry(CloudWatchAsyncClient cloudWatchAsyncClient) {
        io.micrometer.cloudwatch2.CloudWatchConfig cloudWatchConfig = new io.micrometer.cloudwatch2.CloudWatchConfig() {
            private final Map<String, String> configMap = Map.of(
                    "cloudwatch.namespace", "WebAppMetrics",
                    "cloudwatch.step", Duration.ofSeconds(30).toString()
            );

            @Override
            public String get(String key) {
                return configMap.get(key);
            }
        };

        return new CloudWatchMeterRegistry(cloudWatchConfig, Clock.SYSTEM, cloudWatchAsyncClient);
    }
}
