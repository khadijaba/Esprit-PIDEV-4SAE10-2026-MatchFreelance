package com.freelancing.contract.dto;

import com.freelancing.contract.enums.PreviewStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreviewResponseDTO {
    private Long previewId;
    private Long contractId;
    private String htmlUrl;
    private String screenshotUrl;
    private Date generatedAt;
    private Integer version;
    private PreviewStatus status;
    private String designStyle;
    private Integer featuresCount;
    private List<String> features;
    private String clientFeedback;
}
