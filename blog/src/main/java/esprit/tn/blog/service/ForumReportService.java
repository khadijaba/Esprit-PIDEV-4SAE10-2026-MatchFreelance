package esprit.tn.blog.service;

import esprit.tn.blog.entity.ForumReport;
import esprit.tn.blog.entity.ForumReportStatus;
import esprit.tn.blog.repository.ForumPostRepository;
import esprit.tn.blog.repository.ForumReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ForumReportService {

    private final ForumReportRepository forumReportRepository;
    private final ForumPostRepository forumPostRepository;

    public ForumReport createReport(ForumReport report) {
        log.info("[FORUM-REPORT] Creating report - reporterEmail: {}, reporterName: {}", 
                 report.getReporterEmail(), report.getReporterName());
        return forumReportRepository.save(report);
    }

    public List<ForumReport> getAllReports() {
        return forumReportRepository.findAll();
    }

    public ForumReport getReportById(Long id) {
        return forumReportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Report not found with id: " + id));
    }

    public List<ForumReport> getReportsByStatus(String status) {
        return forumReportRepository.findByStatus(ForumReportStatus.valueOf(status));
    }

    public List<ForumReport> getReportsByPostId(Long postId) {
        return forumReportRepository.findByPostId(postId);
    }

    public ForumReport updateReportStatus(Long id, String status, String adminNote) {
        ForumReport existing = getReportById(id);
        existing.setStatus(ForumReportStatus.valueOf(status));
        if (adminNote != null) {
            existing.setAdminNote(adminNote);
        }
        return forumReportRepository.save(existing);
    }

    public void deleteReport(Long id) {
        if (!forumReportRepository.existsById(id))
            throw new RuntimeException("Report not found with id: " + id);
        forumReportRepository.deleteById(id);
    }

    public void deleteReportedPost(Long postId) {
        if (!forumPostRepository.existsById(postId))
            throw new RuntimeException("Post not found with id: " + postId);
        forumPostRepository.deleteById(postId);
    }
}
