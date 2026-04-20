package esprit.tn.blog.controller;

import esprit.tn.blog.entity.ChatMessage;
import esprit.tn.blog.service.ChatMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/forums")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ChatMessageController {

    private final ChatMessageService chatMessageService;

    @PostMapping("/send-message")
    public ResponseEntity<?> sendMessage(@RequestBody ChatMessage message) {
        try {
            return ResponseEntity.ok(chatMessageService.sendMessage(message));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Unknown error"));
        }
    }

    @GetMapping("/get-conversation/{userId1}/{userId2}")
    public ResponseEntity<List<ChatMessage>> getConversation(@PathVariable Long userId1, @PathVariable Long userId2) {
        return ResponseEntity.ok(chatMessageService.getConversation(userId1, userId2));
    }

    @GetMapping("/get-unread-messages/{userId}")
    public ResponseEntity<List<ChatMessage>> getUnreadMessages(@PathVariable Long userId) {
        return ResponseEntity.ok(chatMessageService.getUnreadMessages(userId));
    }

    @GetMapping("/get-unread-message-count/{senderId}/{receiverId}")
    public ResponseEntity<Map<String, Long>> getUnreadCount(@PathVariable Long senderId, @PathVariable Long receiverId) {
        Long count = chatMessageService.getUnreadCount(senderId, receiverId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    @PutMapping("/mark-conversation-read/{senderId}/{receiverId}")
    public ResponseEntity<Void> markConversationRead(@PathVariable Long senderId, @PathVariable Long receiverId) {
        chatMessageService.markConversationRead(senderId, receiverId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/get-last-message/{userId1}/{userId2}")
    public ResponseEntity<ChatMessage> getLastMessage(@PathVariable Long userId1, @PathVariable Long userId2) {
        ChatMessage msg = chatMessageService.getLastMessage(userId1, userId2);
        return msg != null ? ResponseEntity.ok(msg) : ResponseEntity.noContent().build();
    }

    @GetMapping("/get-all-messages/{userId}")
    public ResponseEntity<List<ChatMessage>> getAllMessages(@PathVariable Long userId) {
        return ResponseEntity.ok(chatMessageService.getAllMessagesForUser(userId));
    }

    @DeleteMapping("/delete-message/{id}")
    public ResponseEntity<Void> deleteMessage(@PathVariable Long id) {
        chatMessageService.deleteMessage(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/delete-message-for-user/{messageId}/{userId}")
    public ResponseEntity<ChatMessage> deleteMessageForUser(@PathVariable Long messageId, @PathVariable Long userId) {
        ChatMessage updated = chatMessageService.deleteMessageForUser(messageId, userId);
        if (updated == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/toggle-reaction/{messageId}/{userId}")
    public ResponseEntity<ChatMessage> toggleReaction(@PathVariable Long messageId, @PathVariable Long userId, @RequestParam String emoji) {
        ChatMessage updated = chatMessageService.toggleReaction(messageId, userId, emoji);
        if (updated == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(updated);
    }
}
