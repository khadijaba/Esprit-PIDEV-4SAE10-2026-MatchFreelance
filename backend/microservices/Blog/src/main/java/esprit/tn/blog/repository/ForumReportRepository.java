package esprit.tn.blog.repository;

import esprit.tn.blog.entity.ForumReport;
import esprit.tn.blog.entity.ForumReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ForumReportRepository extends JpaRepository<ForumReport, Long> {
    List<ForumReport> findByStatus(ForumReportStatus status);
    List<ForumReport> findByPostId(Long postId);
}
