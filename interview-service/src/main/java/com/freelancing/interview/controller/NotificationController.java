package com.freelancing.interview.controller;

import com.freelancing.interview.dto.NotificationResponseDTO;
import com.freelancing.interview.service.NotificationService;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Validated
@CrossOrigin(origins = "*")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/users/{userId}")
    public ResponseEntity<Page<NotificationResponseDTO>> listByUser(
            @PathVariable @Min(1) Long userId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(notificationService.listByUser(userId, pageable));
    }

    @PostMapping("/{notificationId}/read")
    public ResponseEntity<NotificationResponseDTO> markAsRead(
            @PathVariable @Min(1) Long notificationId,
            @RequestParam @Min(1) Long userId
    ) {
        return ResponseEntity.ok(notificationService.markAsRead(notificationId, userId));
    }

    @PostMapping("/users/{userId}/read-all")
    public ResponseEntity<Map<String, String>> markAllAsRead(@PathVariable @Min(1) Long userId) {
        notificationService.markAllAsReadForUser(userId);
        return ResponseEntity.ok(Map.of("message", "All notifications marked as read"));
    }
}
