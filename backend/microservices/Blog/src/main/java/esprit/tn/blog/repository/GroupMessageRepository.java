package esprit.tn.blog.repository;

import esprit.tn.blog.entity.GroupMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface GroupMessageRepository extends JpaRepository<GroupMessage, Long> {

    // Get messages for a group
    List<GroupMessage> findByGroupIdAndIsDeletedFalseOrderBySentAtAsc(Long groupId);

    // Get recent messages for a group
    @Query("SELECT gm FROM GroupMessage gm WHERE gm.groupId = :groupId AND gm.isDeleted = false " +
           "ORDER BY gm.sentAt DESC")
    List<GroupMessage> findRecentMessages(Long groupId);

    // Get messages after a certain time (for real-time updates)
    List<GroupMessage> findByGroupIdAndSentAtAfterAndIsDeletedFalseOrderBySentAtAsc(
            Long groupId, LocalDateTime after);

    // Get messages by sender
    List<GroupMessage> findBySenderIdAndGroupIdOrderBySentAtDesc(Long senderId, Long groupId);

    // Count messages in group
    Long countByGroupIdAndIsDeletedFalse(Long groupId);

    // Find messages that need AI summary (long messages without summary)
    @Query("SELECT gm FROM GroupMessage gm WHERE gm.groupId = :groupId AND " +
           "LENGTH(gm.content) > :minLength AND gm.hasSummary = false AND gm.isDeleted = false")
    List<GroupMessage> findMessagesNeedingSummary(Long groupId, int minLength);
}
