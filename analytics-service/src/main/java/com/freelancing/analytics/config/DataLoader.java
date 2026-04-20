package com.freelancing.analytics.config;

import com.freelancing.analytics.entity.PlatformMetric;
import com.freelancing.analytics.repository.MetricRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Configuration
@RequiredArgsConstructor
public class DataLoader {

    private final MetricRepository metricRepository;

    @Bean
    ApplicationRunner seedMetrics() {
        return args -> metricRepository
                .count()
                .flatMap(c -> {
                    if (c > 0) {
                        return Mono.empty();
                    }
                    return metricRepository
                            .saveAll(Flux.fromIterable(List.of(
                                    new PlatformMetric(
                                            null,
                                            "demo.projects_index",
                                            BigDecimal.ONE,
                                            Instant.now()),
                                    new PlatformMetric(
                                            null,
                                            "demo.candidatures_index",
                                            BigDecimal.valueOf(2),
                                            Instant.now()))))
                            .then();
                })
                .block();
    }
}
