package esprit.tn.blog.service;

import esprit.tn.blog.dto.ThreadRequest;
import esprit.tn.blog.entity.Comment;
import esprit.tn.blog.entity.Thread;
import esprit.tn.blog.exception.InappropriateContentException;
import esprit.tn.blog.repository.CommentRepository;
import esprit.tn.blog.repository.ThreadRepository;
import esprit.tn.blog.service.ModerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Thread Service for Blog microservice
 * User data should be provided from User microservice (port 9090)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ThreadService {

    private final ThreadRepository threadRepository;
    private final CommentRepository commentRepository;
    private final ModerationService moderationService;

    public List<Thread> getAllThreads() {
        return threadRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<Thread> getThreadsByCategory(String category) {
        return threadRepository.findByCategoryOrderByCreatedAtDesc(category);
    }

    public Thread createThread(ThreadRequest request) {
        if (moderationService.isToxic(request.getContent())) {
            log.warn("BLOCKED: Inappropriate content detected in thread content: {}", request.getContent());
            throw new InappropriateContentException("Content contains inappropriate language");
        }

        // User data should be provided from User microservice
        // For testing purposes, allow null authorId and use a default value
        Long authorId = request.getAuthorId();
        if (authorId == null) {
            authorId = 1L; // Default for testing
        }

        List<String> tags = request.getTags() != null ? request.getTags() : new ArrayList<>();

        Thread thread = Thread.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .author(request.getAuthor())
                .authorId(authorId)
                .authorName(request.getAuthor())
                .authorRole(request.getAuthorRole())
                .authorAvatar(request.getAuthorAvatar())
                .category(request.getCategory())
                .createdAt(LocalDateTime.now())
                .postCount(0)
                .likes(0)
                .retweets(0)
                .build();

        // Set tags after building - FIXED: use setTags() not setTagsList()
        thread.setTags(tags);

        return threadRepository.save(thread);
    }

    @Transactional
    public Comment addComment(Long threadId, Comment comment) {
        if (moderationService.isToxic(comment.getContent())) {
            log.warn("BLOCKED: Inappropriate content detected in comment for thread {}", threadId);
            throw new InappropriateContentException("Comment contains inappropriate language");
        }

        Thread thread = threadRepository.findById(threadId)
                .orElseThrow(() -> new RuntimeException("Thread not found"));

        // User data should be provided from User microservice
        if (comment.getAuthorId() == null) {
            throw new IllegalArgumentException("Author ID is required - must be provided from User microservice");
        }
        
        comment.setThread(thread);
        comment.setCreatedAt(LocalDateTime.now());
        Comment savedComment = commentRepository.save(comment);
        
        thread.setPostCount(thread.getPostCount() + 1);
        threadRepository.save(thread);
        
        return savedComment;
    }

    public List<Comment> getCommentsByThread(Long threadId) {
        return commentRepository.findByThreadIdOrderByCreatedAtAsc(threadId);
    }

    @Transactional
    public Thread toggleLike(Long threadId, boolean increment) {
        Thread thread = threadRepository.findById(threadId)
                .orElseThrow(() -> new RuntimeException("Thread not found"));
        
        if (increment) {
            thread.setLikes(thread.getLikes() + 1);
        } else {
            thread.setLikes(Math.max(0, thread.getLikes() - 1));
        }
        
        return threadRepository.save(thread);
    }

    @Transactional
    public Thread toggleRetweet(Long threadId, boolean increment) {
        Thread thread = threadRepository.findById(threadId)
                .orElseThrow(() -> new RuntimeException("Thread not found"));
        
        if (increment) {
            thread.setRetweets(thread.getRetweets() + 1);
        } else {
            thread.setRetweets(Math.max(0, thread.getRetweets() - 1));
        }
        
        return threadRepository.save(thread);
    }
}
