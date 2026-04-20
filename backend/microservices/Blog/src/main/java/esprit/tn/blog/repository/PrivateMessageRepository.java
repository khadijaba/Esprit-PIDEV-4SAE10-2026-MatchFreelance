package esprit.tn.blog.repository;

import esprit.tn.blog.entity.PrivateMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PrivateMessageRepository extends JpaRepository<PrivateMessage, Long> {

    // Get conversation between two users
    @Query("SELECT pm FROM PrivateMessage pm WHERE " +
           "(pm.senderId = :userId1 AND pm.receiverId = :userId2 AND pm.isDeletedBySender = false) OR " +
           "(pm.senderId = :userId2 AND pm.receiverId = :userId1 AND pm.isDeletedByReceiver = false) " +
           "ORDER BY pm.sentAt ASC")
    List<PrivateMessage> findConversation(Long userId1, Long userId2);

    // Get all conversations for a user (latest message per conversation)
    @Query("SELECT pm FROM PrivateMessage pm WHERE pm.id IN (" +
           "SELECT MAX(pm2.id) FROM PrivateMessage pm2 WHERE " +
           "(pm2.senderId = :userId AND pm2.isDeletedBySender = false) OR " +
           "(pm2.receiverId = :userId AND pm2.isDeletedByReceiver = false) " +
           "GROUP BY CASE WHEN pm2.senderId = :userId THEN pm2.receiverId ELSE pm2.senderId END) " +
           "ORDER BY pm.sentAt DESC")
    List<PrivateMessage> findUserConversations(Long userId);

    // Get unread messages for a user
    List<PrivateMessage> findByReceiverIdAndIsReadFalseOrderBySentAtDesc(Long receiverId);

    // Count unread messages
    Long countByReceiverIdAndIsReadFalse(Long receiverId);

    // Get messages sent by user
    List<PrivateMessage> findBySenderIdOrderBySentAtDesc(Long senderId);

    // Get messages received by user
    List<PrivateMessage> findByReceiverIdOrderBySentAtDesc(Long receiverId);
}
