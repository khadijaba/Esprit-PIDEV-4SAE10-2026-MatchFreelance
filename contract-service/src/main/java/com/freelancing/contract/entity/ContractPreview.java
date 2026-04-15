package com.freelancing.contract.entity;

import com.freelancing.contract.enums.PreviewStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Entity
@Table(name = "contract_previews")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContractPreview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "contract_id", nullable = false)
    private Long contractId;

    @Lob
    @Column(name = "html_content", columnDefinition = "LONGTEXT")
    private String htmlContent;

    @Column(name = "screenshot_url")
    private String screenshotUrl;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "generated_at", nullable = false)
    private Date generatedAt;

    @Column(name = "version")
    private Integer version = 1;

    @Column(name = "client_feedback", length = 2000)
    private String clientFeedback;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private PreviewStatus status = PreviewStatus.DRAFT;

    @Column(name = "design_style", length = 50)
    private String designStyle = "modern";

    @Column(name = "features_count")
    private Integer featuresCount;

    @PrePersist
    protected void onCreate() {
        if (generatedAt == null) {
            generatedAt = new Date();
        }
        if (version == null) {
            version = 1;
        }
        if (status == null) {
            status = PreviewStatus.DRAFT;
        }
    }
}
