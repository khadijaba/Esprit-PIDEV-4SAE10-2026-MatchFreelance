package esprit.tn.blog.repository;

import esprit.tn.blog.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByThreadIdOrderByCreatedAtAsc(Long threadId);
}
