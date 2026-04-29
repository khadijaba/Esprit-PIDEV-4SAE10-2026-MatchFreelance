package esprit.tn.blog.service;

import esprit.tn.blog.entity.ForumNotification;
import esprit.tn.blog.repository.ForumNotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ForumNotificationService {

    private final ForumNotificationRepository notificationRepository;

    public ForumNotification createNotification(ForumNotification notification) {
        return notificationRepository.save(notification);
    }

    public List<ForumNotification> getNotificationsByUser(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<ForumNotification> getUnreadByUser(Long userId) {
        return notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
    }

    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    public ForumNotification markAsRead(Long id) {
        ForumNotification n = notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification not found with id: " + id));
        n.setIsRead(true);
        return notificationRepository.save(n);
    }

    public void markAllAsRead(Long userId) {
        List<ForumNotification> unread = notificationRepository
                .findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
        unread.forEach(n -> n.setIsRead(true));
        notificationRepository.saveAll(unread);
    }

    public void deleteNotification(Long id) {
        notificationRepository.deleteById(id);
    }
}
