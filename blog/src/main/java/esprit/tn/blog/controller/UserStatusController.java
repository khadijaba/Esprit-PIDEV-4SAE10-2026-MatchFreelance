package esprit.tn.blog.controller;

import esprit.tn.blog.entity.UserStatus;
import esprit.tn.blog.service.UserStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/forums")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UserStatusController {

    private final UserStatusService userStatusService;

    @PostMapping("/user-heartbeat")
    public ResponseEntity<UserStatus> heartbeat(@RequestBody UserStatus status) {
        return ResponseEntity.ok(userStatusService.setOnline(status));
    }

    @PutMapping("/user-offline/{userId}")
    public ResponseEntity<UserStatus> setOffline(@PathVariable Long userId) {
        UserStatus s = userStatusService.setOffline(userId);
        return s != null ? ResponseEntity.ok(s) : ResponseEntity.notFound().build();
    }

    @GetMapping("/user-status/{userId}")
    public ResponseEntity<UserStatus> getStatus(@PathVariable Long userId) {
        UserStatus s = userStatusService.getStatus(userId);
        return s != null ? ResponseEntity.ok(s) : ResponseEntity.notFound().build();
    }

    @PostMapping("/user-statuses")
    public ResponseEntity<List<UserStatus>> getStatuses(@RequestBody List<Long> userIds) {
        return ResponseEntity.ok(userStatusService.getStatuses(userIds));
    }

    @GetMapping("/online-users")
    public ResponseEntity<List<UserStatus>> getOnlineUsers() {
        return ResponseEntity.ok(userStatusService.getOnlineUsers());
    }

    @PutMapping("/set-typing/{userId}/{typingToUserId}")
    public ResponseEntity<UserStatus> setTyping(@PathVariable Long userId, @PathVariable Long typingToUserId) {
        UserStatus s = userStatusService.setTyping(userId, typingToUserId);
        return s != null ? ResponseEntity.ok(s) : ResponseEntity.notFound().build();
    }

    @PutMapping("/clear-typing/{userId}")
    public ResponseEntity<UserStatus> clearTyping(@PathVariable Long userId) {
        UserStatus s = userStatusService.clearTyping(userId);
        return s != null ? ResponseEntity.ok(s) : ResponseEntity.notFound().build();
    }

    @GetMapping("/is-typing/{typerId}/{receiverId}")
    public ResponseEntity<Map<String, Boolean>> isTyping(@PathVariable Long typerId, @PathVariable Long receiverId) {
        boolean typing = userStatusService.isTypingTo(typerId, receiverId);
        return ResponseEntity.ok(Map.of("typing", typing));
    }
}
