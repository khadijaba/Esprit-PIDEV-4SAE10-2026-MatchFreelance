package esprit.tn.blog.repository;

import esprit.tn.blog.entity.ForumNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ForumNotificationRepository extends JpaRepository<ForumNotification, Long> {
    List<ForumNotification> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<ForumNotification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(Long userId);
    long countByUserIdAndIsReadFalse(Long userId);
}
