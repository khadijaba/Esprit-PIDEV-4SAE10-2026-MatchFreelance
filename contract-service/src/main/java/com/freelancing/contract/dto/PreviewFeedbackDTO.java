package com.freelancing.contract.dto;

import com.freelancing.contract.enums.PreviewStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PreviewFeedbackDTO {
    private String feedback;
    private PreviewStatus status;
}
