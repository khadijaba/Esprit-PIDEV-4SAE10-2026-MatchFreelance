package esprit.tn.blog.controller;

import esprit.tn.blog.dto.GroupMessageRequest;
import esprit.tn.blog.entity.GroupMessage;
import esprit.tn.blog.service.GroupMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class GroupMessageController {

    private final GroupMessageService messageService;

    /**
     * Send a message to a group
     */
    @PostMapping("/{groupId}/messages")
    public ResponseEntity<GroupMessage> sendMessage(
            @PathVariable Long groupId,
            @RequestBody GroupMessageRequest request) {
        log.info("Sending message to group: {}", groupId);
        request.setGroupId(groupId);
        GroupMessage message = messageService.sendMessage(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(message);
    }

    /**
     * Get messages for a group
     */
    @GetMapping("/{groupId}/messages")
    public ResponseEntity<List<GroupMessage>> getGroupMessages(@PathVariable Long groupId) {
        List<GroupMessage> messages = messageService.getGroupMessages(groupId);
        return ResponseEntity.ok(messages);
    }

    /**
     * Get recent messages for a group
     */
    @GetMapping("/{groupId}/messages/recent")
    public ResponseEntity<List<GroupMessage>> getRecentMessages(
            @PathVariable Long groupId,
            @RequestParam(defaultValue = "50") int limit) {
        List<GroupMessage> messages = messageService.getRecentMessages(groupId, limit);
        return ResponseEntity.ok(messages);
    }

    /**
     * Get messages after a certain time (for real-time updates)
     */
    @GetMapping("/{groupId}/messages/after")
    public ResponseEntity<List<GroupMessage>> getMessagesAfter(
            @PathVariable Long groupId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime after) {
        List<GroupMessage> messages = messageService.getMessagesAfter(groupId, after);
        return ResponseEntity.ok(messages);
    }

    /**
     * Get message by ID
     */
    @GetMapping("/messages/{messageId}")
    public ResponseEntity<GroupMessage> getMessageById(@PathVariable Long messageId) {
        GroupMessage message = messageService.getMessageById(messageId);
        return ResponseEntity.ok(message);
    }

    /**
     * Edit a message
     */
    @PutMapping("/messages/{messageId}")
    public ResponseEntity<GroupMessage> editMessage(
            @PathVariable Long messageId,
            @RequestBody Map<String, String> body) {
        String newContent = body.get("content");
        GroupMessage message = messageService.editMessage(messageId, newContent);
        return ResponseEntity.ok(message);
    }

    /**
     * Delete a message
     */
    @DeleteMapping("/messages/{messageId}")
    public ResponseEntity<Void> deleteMessage(@PathVariable Long messageId) {
        messageService.deleteMessage(messageId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Add reaction to a message
     */
    @PostMapping("/messages/{messageId}/reaction")
    public ResponseEntity<GroupMessage> addReaction(
            @PathVariable Long messageId,
            @RequestBody Map<String, Object> body) {
        Long userId = Long.valueOf(body.get("userId").toString());
        String emoji = body.get("emoji").toString();
        GroupMessage message = messageService.addReaction(messageId, userId, emoji);
        return ResponseEntity.ok(message);
    }

    /**
     * Generate AI summary for a message
     */
    @PostMapping("/messages/{messageId}/summary")
    public ResponseEntity<Map<String, String>> generateSummary(@PathVariable Long messageId) {
        String summary = messageService.generateSummaryForMessage(messageId);
        return ResponseEntity.ok(Map.of("summary", summary));
    }

    /**
     * Batch generate summaries for all long messages in a group
     */
    @PostMapping("/{groupId}/messages/generate-summaries")
    public ResponseEntity<Map<String, String>> generateSummariesForGroup(@PathVariable Long groupId) {
        messageService.generateSummariesForGroup(groupId);
        return ResponseEntity.ok(Map.of("message", "Summaries generation started"));
    }

    /**
     * Count messages in group
     */
    @GetMapping("/{groupId}/messages/count")
    public ResponseEntity<Map<String, Long>> countMessages(@PathVariable Long groupId) {
        Long count = messageService.countGroupMessages(groupId);
        return ResponseEntity.ok(Map.of("count", count));
    }
}
