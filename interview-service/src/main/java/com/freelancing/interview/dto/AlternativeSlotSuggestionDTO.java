package com.freelancing.interview.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
public class AlternativeSlotSuggestionDTO {
    private Instant startAt;
    private Instant endAt;
    private Long slotId;
    private long score;
}

