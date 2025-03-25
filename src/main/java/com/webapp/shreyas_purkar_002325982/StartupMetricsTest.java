package com.webapp.shreyas_purkar_002325982;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

@Component
public class StartupMetricsTest {

    private final MeterRegistry meterRegistry;

    public StartupMetricsTest(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    public void pushTestMetricOnStartup() {
        meterRegistry.counter("test.startup.metric").increment(5.0);
    }
}
