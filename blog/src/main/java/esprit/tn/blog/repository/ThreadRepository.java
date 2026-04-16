package esprit.tn.blog.repository;

import esprit.tn.blog.entity.Thread;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ThreadRepository extends JpaRepository<Thread, Long> {
    List<Thread> findAllByOrderByCreatedAtDesc();
    List<Thread> findByCategoryOrderByCreatedAtDesc(String category);
    List<Thread> findByTagsContainingOrderByCreatedAtDesc(String tag);
}
