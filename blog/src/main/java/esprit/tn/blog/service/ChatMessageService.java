package esprit.tn.blog.service;

import esprit.tn.blog.entity.ChatMessage;
import esprit.tn.blog.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;

    public ChatMessage sendMessage(ChatMessage message) {
        return chatMessageRepository.save(message);
    }

    public List<ChatMessage> getConversation(Long userId1, Long userId2) {
        return chatMessageRepository.findConversation(userId1, userId2);
    }

    public List<ChatMessage> getUnreadMessages(Long userId) {
        return chatMessageRepository.findUnreadMessages(userId);
    }

    public Long getUnreadCount(Long senderId, Long receiverId) {
        return chatMessageRepository.countUnreadFrom(senderId, receiverId);
    }

    public void markConversationRead(Long senderId, Long receiverId) {
        List<ChatMessage> messages = chatMessageRepository.findConversation(senderId, receiverId);
        for (ChatMessage msg : messages) {
            if (msg.getReceiverId().equals(receiverId) && !msg.getIsRead()) {
                msg.setIsRead(true);
                msg.setReadAt(LocalDateTime.now());
                chatMessageRepository.save(msg);
            }
        }
    }

    public ChatMessage getLastMessage(Long userId1, Long userId2) {
        return chatMessageRepository.findLastMessage(userId1, userId2);
    }

    public List<ChatMessage> getAllMessagesForUser(Long userId) {
        return chatMessageRepository.findAllMessagesForUser(userId);
    }

    public void deleteMessage(Long messageId) {
        chatMessageRepository.deleteById(messageId);
    }

    public ChatMessage deleteMessageForUser(Long messageId, Long userId) {
        ChatMessage msg = chatMessageRepository.findById(messageId).orElse(null);
        if (msg == null) return null;
        String deleted = msg.getDeletedForUsers();
        if (deleted == null || deleted.isEmpty()) {
            deleted = userId.toString();
        } else {
            deleted = deleted + "," + userId;
        }
        msg.setDeletedForUsers(deleted);
        return chatMessageRepository.save(msg);
    }

    public ChatMessage toggleReaction(Long messageId, Long userId, String emoji) {
        ChatMessage msg = chatMessageRepository.findById(messageId).orElse(null);
        if (msg == null) return null;
        String key = userId + ":" + emoji;
        String reactions = msg.getReactions();
        if (reactions == null || reactions.isEmpty()) {
            reactions = key;
        } else if (reactions.contains(key)) {
            reactions = reactions.replace(key, "").replace(",,", ",");
            if (reactions.startsWith(",")) reactions = reactions.substring(1);
            if (reactions.endsWith(",")) reactions = reactions.substring(0, reactions.length() - 1);
        } else {
            reactions = reactions + "," + key;
        }
        msg.setReactions(reactions.isEmpty() ? null : reactions);
        return chatMessageRepository.save(msg);
    }
}
