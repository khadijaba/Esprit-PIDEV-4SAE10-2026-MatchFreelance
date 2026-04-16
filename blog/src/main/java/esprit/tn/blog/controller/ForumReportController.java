package esprit.tn.blog.controller;

import esprit.tn.blog.entity.ForumReport;
import esprit.tn.blog.service.ForumReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/forums")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ForumReportController {

    private final ForumReportService reportService;

    @PostMapping("/create-report")
    public ResponseEntity<ForumReport> createReport(@RequestBody ForumReport report) {
        return ResponseEntity.ok(reportService.createReport(report));
    }

    @GetMapping("/get-all-reports")
    public ResponseEntity<List<ForumReport>> getAllReports() {
        return ResponseEntity.ok(reportService.getAllReports());
    }

    @GetMapping("/get-report-by-id/{id}")
    public ResponseEntity<ForumReport> getReportById(@PathVariable Long id) {
        return ResponseEntity.ok(reportService.getReportById(id));
    }

    @GetMapping("/get-reports-by-post/{postId}")
    public ResponseEntity<List<ForumReport>> getReportsByPost(@PathVariable Long postId) {
        return ResponseEntity.ok(reportService.getReportsByPostId(postId));
    }

    @GetMapping("/get-reports-by-status/{status}")
    public ResponseEntity<List<ForumReport>> getReportsByStatus(@PathVariable String status) {
        return ResponseEntity.ok(reportService.getReportsByStatus(status));
    }

    @PutMapping("/update-report-status/{id}")
    public ResponseEntity<ForumReport> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String status = body.get("status");
        String adminNote = body.get("adminNote");
        return ResponseEntity.ok(reportService.updateReportStatus(id, status, adminNote));
    }

    @DeleteMapping("/delete-report/{id}")
    public ResponseEntity<Void> deleteReport(@PathVariable Long id) {
        reportService.deleteReport(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/delete-reported-post/{postId}")
    public ResponseEntity<Void> deleteReportedPost(@PathVariable Long postId) {
        reportService.deleteReportedPost(postId);
        return ResponseEntity.noContent().build();
    }
}
