package com.freelancing.candidature.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CommunicationScorePayload {
    private double score;
    private int messageCount;
}
