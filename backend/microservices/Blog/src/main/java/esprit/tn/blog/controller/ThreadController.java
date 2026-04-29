package esprit.tn.blog.controller;

import esprit.tn.blog.dto.ThreadRequest;
import esprit.tn.blog.entity.Comment;
import esprit.tn.blog.entity.Thread;
import esprit.tn.blog.service.ThreadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/threads")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")

public class ThreadController {

    private final ThreadService threadService;

    @GetMapping
    public List<Thread> getAllThreads(@RequestParam(required = false) String category) {
        if (category != null && !category.equals("all")) {
            return threadService.getThreadsByCategory(category);
        }
        return threadService.getAllThreads();
    }

    @PostMapping
    public Thread createThread(@RequestBody ThreadRequest threadRequest) {
        log.info("Received request to create thread: {}", threadRequest.getTitle());
        log.debug("Full thread content: {}", threadRequest.getContent());
        return threadService.createThread(threadRequest);
    }

    @GetMapping("/{id}/comments")
    public List<Comment> getComments(@PathVariable Long id) {
        return threadService.getCommentsByThread(id);
    }

    @PostMapping("/{id}/comments")
    public Comment addComment(@PathVariable Long id, @RequestBody Comment comment) {
        log.info("Received request to add comment to thread {}: {}", id, comment.getContent());
        return threadService.addComment(id, comment);
    }

    @PostMapping("/{id}/like")
    public Thread toggleLike(@PathVariable Long id, @RequestParam boolean increment) {
        return threadService.toggleLike(id, increment);
    }

    @PostMapping("/{id}/retweet")
    public Thread toggleRetweet(@PathVariable Long id, @RequestParam boolean increment) {
        return threadService.toggleRetweet(id, increment);
    }
}
