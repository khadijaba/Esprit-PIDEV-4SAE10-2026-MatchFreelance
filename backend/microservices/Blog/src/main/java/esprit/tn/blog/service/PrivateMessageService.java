package esprit.tn.blog.service;

import esprit.tn.blog.dto.PrivateMessageRequest;
import esprit.tn.blog.entity.PrivateMessage;
import esprit.tn.blog.repository.PrivateMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PrivateMessageService {

    private final PrivateMessageRepository messageRepository;

    /**
     * Send a private message
     */
    @Transactional
    public PrivateMessage sendMessage(PrivateMessageRequest request) {
        log.info("Sending private message from {} to {}", request.getSenderId(), request.getReceiverId());

        PrivateMessage message = PrivateMessage.builder()
                .senderId(request.getSenderId())
                .receiverId(request.getReceiverId())
                .senderName(request.getSenderName())
                .receiverName(request.getReceiverName())
                .senderAvatar(request.getSenderAvatar())
                .content(request.getContent())
                .type(request.getType() != null ? request.getType() : PrivateMessage.MessageType.TEXT)
                .mediaUrl(request.getMediaUrl())
                .gifUrl(request.getGifUrl())
                .replyToMessageId(request.getReplyToMessageId())
                .build();

        return messageRepository.save(message);
    }

    /**
     * Get conversation between two users
     */
    public List<PrivateMessage> getConversation(Long userId1, Long userId2) {
        return messageRepository.findConversation(userId1, userId2);
    }

    /**
     * Get all conversations for a user
     */
    public List<PrivateMessage> getUserConversations(Long userId) {
        return messageRepository.findUserConversations(userId);
    }

    /**
     * Get unread messages for a user
     */
    public List<PrivateMessage> getUnreadMessages(Long userId) {
        return messageRepository.findByReceiverIdAndIsReadFalseOrderBySentAtDesc(userId);
    }

    /**
     * Count unread messages
     */
    public Long countUnreadMessages(Long userId) {
        return messageRepository.countByReceiverIdAndIsReadFalse(userId);
    }

    /**
     * Mark message as read
     */
    @Transactional
    public void markAsRead(Long messageId) {
        PrivateMessage message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        message.setIsRead(true);
        message.setReadAt(LocalDateTime.now());
        messageRepository.save(message);
    }

    /**
     * Mark all messages in conversation as read
     */
    @Transactional
    public void markConversationAsRead(Long userId, Long otherUserId) {
        List<PrivateMessage> messages = messageRepository.findConversation(userId, otherUserId);

        for (PrivateMessage message : messages) {
            if (message.getReceiverId().equals(userId) && !message.getIsRead()) {
                message.setIsRead(true);
                message.setReadAt(LocalDateTime.now());
                messageRepository.save(message);
            }
        }
    }

    /**
     * Delete message for sender
     */
    @Transactional
    public void deleteForSender(Long messageId, Long senderId) {
        PrivateMessage message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        if (!message.getSenderId().equals(senderId)) {
            throw new RuntimeException("Unauthorized to delete this message");
        }

        message.setIsDeletedBySender(true);

        // If deleted by both, mark as fully deleted
        if (message.getIsDeletedByReceiver()) {
            message.setIsDeleted(true);
        }

        messageRepository.save(message);
    }

    /**
     * Delete message for receiver
     */
    @Transactional
    public void deleteForReceiver(Long messageId, Long receiverId) {
        PrivateMessage message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        if (!message.getReceiverId().equals(receiverId)) {
            throw new RuntimeException("Unauthorized to delete this message");
        }

        message.setIsDeletedByReceiver(true);

        // If deleted by both, mark as fully deleted
        if (message.getIsDeletedBySender()) {
            message.setIsDeleted(true);
        }

        messageRepository.save(message);
    }

    /**
     * Add reaction to message
     */
    @Transactional
    public PrivateMessage addReaction(Long messageId, String emoji) {
        PrivateMessage message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        message.setReaction(emoji);
        return messageRepository.save(message);
    }

    /**
     * Remove reaction from message
     */
    @Transactional
    public PrivateMessage removeReaction(Long messageId) {
        PrivateMessage message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        message.setReaction(null);
        return messageRepository.save(message);
    }
}
