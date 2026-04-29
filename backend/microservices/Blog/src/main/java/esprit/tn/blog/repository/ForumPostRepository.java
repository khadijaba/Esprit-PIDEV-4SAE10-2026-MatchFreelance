package esprit.tn.blog.repository;

import esprit.tn.blog.entity.ForumPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ForumPostRepository extends JpaRepository<ForumPost, Long> {
    List<ForumPost> findByTopicId(Long topicId);
    List<ForumPost> findByUserId(Long userId);
    List<ForumPost> findByParentPostId(Long parentPostId);
    List<ForumPost> findByParentPostIdIsNullOrderByCreatedAtDesc();
}
