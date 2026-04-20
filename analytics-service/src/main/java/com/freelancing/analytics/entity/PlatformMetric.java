package com.freelancing.analytics.entity;

import java.math.BigDecimal;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("platform_metric")
public class PlatformMetric {

    @Id
    private Long id;

    @Column("metric_key")
    private String metricKey;

    @Column("metric_value")
    private BigDecimal metricValue;

    @Column("updated_at")
    private Instant updatedAt;
}
