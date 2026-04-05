package com.freelancing.interview.dto;

import com.freelancing.interview.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponseDTO {
    private Long id;
    private Long userId;
    private Long interviewId;
    private NotificationType type;
    private String message;
    private Instant readAt;
    private Instant createdAt;
}
