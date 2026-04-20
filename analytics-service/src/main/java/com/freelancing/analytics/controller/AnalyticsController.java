package com.freelancing.analytics.controller;

import com.freelancing.analytics.entity.PlatformMetric;
import com.freelancing.analytics.repository.MetricRepository;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final MetricRepository metricRepository;

    @Value("${matchfreelance.config-source:local}")
    private String configSource;

    @GetMapping("/info")
    public Mono<Map<String, Object>> info() {
        return Mono.just(
                Map.of(
                        "service", "analytics-service",
                        "stack", "Spring WebFlux + R2DBC",
                        "database", "PostgreSQL",
                        "configSource", configSource));
    }

    @GetMapping("/metrics")
    public Flux<PlatformMetric> metrics() {
        return metricRepository.findAll();
    }
}
