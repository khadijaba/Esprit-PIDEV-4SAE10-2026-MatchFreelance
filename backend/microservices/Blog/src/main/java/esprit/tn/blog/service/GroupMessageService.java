package esprit.tn.blog.service;

import esprit.tn.blog.dto.GroupMessageRequest;
import esprit.tn.blog.entity.DiscussionGroup;
import esprit.tn.blog.entity.GroupMessage;
import esprit.tn.blog.repository.GroupMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroupMessageService {

    private final GroupMessageRepository messageRepository;
    private final DiscussionGroupService groupService;
    private final PostSummaryService summaryService;

    /**
     * Send a message to a group
     */
    @Transactional
    public GroupMessage sendMessage(GroupMessageRequest request) {
        log.info("Sending message to group: {}", request.getGroupId());
        if (request.getGroupId() == null) {
            throw new IllegalArgumentException("groupId is required");
        }

        DiscussionGroup group = groupService.getGroupById(request.getGroupId());

        GroupMessage message = GroupMessage.builder()
                .group(group)
                .groupId(request.getGroupId())
                .senderId(request.getSenderId())
                .senderName(request.getSenderName())
                .senderAvatar(request.getSenderAvatar())
                .content(request.getContent())
                .type(request.getType() != null ? request.getType() : GroupMessage.MessageType.TEXT)
                .mediaUrl(request.getMediaUrl())
                .gifUrl(request.getGifUrl())
                .replyToMessageId(request.getReplyToMessageId())
                .build();

        // Generate AI summary for long messages
        if (summaryService.needsSummary(request.getContent())) {
            try {
                String summary = summaryService.generateSummary(request.getContent());
                message.setAiSummary(summary);
                message.setHasSummary(true);
                log.info("AI summary generated for message");
            } catch (Exception e) {
                log.warn("Failed to generate AI summary: {}", e.getMessage());
            }
        }

        GroupMessage savedMessage = messageRepository.save(message);

        // Update group activity and message count
        groupService.incrementMessageCount(request.getGroupId());

        return savedMessage;
    }

    /**
     * Get messages for a group
     */
    public List<GroupMessage> getGroupMessages(Long groupId) {
        return messageRepository.findByGroupIdAndIsDeletedFalseOrderBySentAtAsc(groupId);
    }

    /**
     * Get recent messages for a group
     */
    public List<GroupMessage> getRecentMessages(Long groupId, int limit) {
        List<GroupMessage> messages = messageRepository.findRecentMessages(groupId);
        return messages.stream().limit(limit).toList();
    }

    /**
     * Get messages after a certain time (for real-time updates)
     */
    public List<GroupMessage> getMessagesAfter(Long groupId, LocalDateTime after) {
        return messageRepository.findByGroupIdAndSentAtAfterAndIsDeletedFalseOrderBySentAtAsc(groupId, after);
    }

    /**
     * Edit a message
     */
    @Transactional
    public GroupMessage editMessage(Long messageId, String newContent) {
        GroupMessage message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        message.setContent(newContent);
        message.setIsEdited(true);
        message.setEditedAt(LocalDateTime.now());

        // Regenerate summary if needed
        if (summaryService.needsSummary(newContent)) {
            try {
                String summary = summaryService.generateSummary(newContent);
                message.setAiSummary(summary);
                message.setHasSummary(true);
            } catch (Exception e) {
                log.warn("Failed to regenerate AI summary: {}", e.getMessage());
            }
        } else {
            message.setAiSummary(null);
            message.setHasSummary(false);
        }

        return messageRepository.save(message);
    }

    /**
     * Delete a message
     */
    @Transactional
    public void deleteMessage(Long messageId) {
        GroupMessage message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        message.setIsDeleted(true);
        messageRepository.save(message);
    }

    /**
     * Add reaction to a message
     */
    @Transactional
    public GroupMessage addReaction(Long messageId, Long userId, String emoji) {
        GroupMessage message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        // Simple implementation: store as JSON string
        // In production, consider using a separate Reaction entity
        String currentReactions = message.getReactions();
        if (currentReactions == null) {
            currentReactions = "";
        }

        // Add emoji reaction (simplified)
        message.setReactions(currentReactions + emoji);

        return messageRepository.save(message);
    }

    /**
     * Generate summary for existing message
     */
    @Transactional
    public String generateSummaryForMessage(Long messageId) {
        GroupMessage message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        if (!summaryService.needsSummary(message.getContent())) {
            return "Message is too short for summary";
        }

        String summary = summaryService.generateSummary(message.getContent());
        message.setAiSummary(summary);
        message.setHasSummary(true);
        messageRepository.save(message);

        return summary;
    }

    /**
     * Batch generate summaries for long messages in a group
     */
    @Transactional
    public void generateSummariesForGroup(Long groupId) {
        List<GroupMessage> messages = messageRepository.findMessagesNeedingSummary(groupId, 500);

        log.info("Generating summaries for {} messages in group {}", messages.size(), groupId);

        for (GroupMessage message : messages) {
            try {
                String summary = summaryService.generateSummary(message.getContent());
                message.setAiSummary(summary);
                message.setHasSummary(true);
                messageRepository.save(message);
            } catch (Exception e) {
                log.error("Failed to generate summary for message {}: {}", message.getId(), e.getMessage());
            }
        }
    }

    /**
     * Get message by ID
     */
    public GroupMessage getMessageById(Long messageId) {
        return messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));
    }

    /**
     * Count messages in group
     */
    public Long countGroupMessages(Long groupId) {
        return messageRepository.countByGroupIdAndIsDeletedFalse(groupId);
    }
}
