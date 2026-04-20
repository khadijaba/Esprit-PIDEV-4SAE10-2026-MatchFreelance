package com.freelancing.analytics.repository;

import com.freelancing.analytics.entity.PlatformMetric;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface MetricRepository extends ReactiveCrudRepository<PlatformMetric, Long> {
}
