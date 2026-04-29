package esprit.tn.blog.controller;

import esprit.tn.blog.dto.PrivateMessageRequest;
import esprit.tn.blog.entity.PrivateMessage;
import esprit.tn.blog.service.PrivateMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/messages/private")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class PrivateMessageController {

    private final PrivateMessageService messageService;

    /**
     * Send a private message
     */
    @PostMapping
    public ResponseEntity<PrivateMessage> sendMessage(@RequestBody PrivateMessageRequest request) {
        log.info("Sending private message from {} to {}", request.getSenderId(), request.getReceiverId());
        PrivateMessage message = messageService.sendMessage(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(message);
    }

    /**
     * Get conversation between two users
     */
    @GetMapping("/conversation/{userId1}/{userId2}")
    public ResponseEntity<List<PrivateMessage>> getConversation(
            @PathVariable Long userId1,
            @PathVariable Long userId2) {
        List<PrivateMessage> messages = messageService.getConversation(userId1, userId2);
        return ResponseEntity.ok(messages);
    }

    /**
     * Get all conversations for a user
     */
    @GetMapping("/conversations/{userId}")
    public ResponseEntity<List<PrivateMessage>> getUserConversations(@PathVariable Long userId) {
        List<PrivateMessage> conversations = messageService.getUserConversations(userId);
        return ResponseEntity.ok(conversations);
    }

    /**
     * Get unread messages for a user
     */
    @GetMapping("/unread/{userId}")
    public ResponseEntity<List<PrivateMessage>> getUnreadMessages(@PathVariable Long userId) {
        List<PrivateMessage> messages = messageService.getUnreadMessages(userId);
        return ResponseEntity.ok(messages);
    }

    /**
     * Count unread messages
     */
    @GetMapping("/unread/{userId}/count")
    public ResponseEntity<Map<String, Long>> countUnreadMessages(@PathVariable Long userId) {
        Long count = messageService.countUnreadMessages(userId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * Mark message as read
     */
    @PutMapping("/{messageId}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long messageId) {
        messageService.markAsRead(messageId);
        return ResponseEntity.ok().build();
    }

    /**
     * Mark all messages in conversation as read
     */
    @PutMapping("/conversation/{userId}/{otherUserId}/read")
    public ResponseEntity<Void> markConversationAsRead(
            @PathVariable Long userId,
            @PathVariable Long otherUserId) {
        messageService.markConversationAsRead(userId, otherUserId);
        return ResponseEntity.ok().build();
    }

    /**
     * Delete message for sender
     */
    @DeleteMapping("/{messageId}/sender/{senderId}")
    public ResponseEntity<Void> deleteForSender(
            @PathVariable Long messageId,
            @PathVariable Long senderId) {
        messageService.deleteForSender(messageId, senderId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Delete message for receiver
     */
    @DeleteMapping("/{messageId}/receiver/{receiverId}")
    public ResponseEntity<Void> deleteForReceiver(
            @PathVariable Long messageId,
            @PathVariable Long receiverId) {
        messageService.deleteForReceiver(messageId, receiverId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Add reaction to message
     */
    @PostMapping("/{messageId}/reaction")
    public ResponseEntity<PrivateMessage> addReaction(
            @PathVariable Long messageId,
            @RequestBody Map<String, String> body) {
        String emoji = body.get("emoji");
        PrivateMessage message = messageService.addReaction(messageId, emoji);
        return ResponseEntity.ok(message);
    }

    /**
     * Remove reaction from message
     */
    @DeleteMapping("/{messageId}/reaction")
    public ResponseEntity<PrivateMessage> removeReaction(@PathVariable Long messageId) {
        PrivateMessage message = messageService.removeReaction(messageId);
        return ResponseEntity.ok(message);
    }
}
