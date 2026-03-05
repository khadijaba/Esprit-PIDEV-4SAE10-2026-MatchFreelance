package com.freelancing.contract.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Chat communication score for a freelancer based on their message history in contract chats.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommunicationScoreDTO {

    /** Score 0–100 (50 when no data). */
    private double score;
    /** Number of freelancer messages used to compute the score (0 when no history). */
    private int messageCount;
}
