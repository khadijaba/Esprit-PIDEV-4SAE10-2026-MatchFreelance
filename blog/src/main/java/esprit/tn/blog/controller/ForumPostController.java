package esprit.tn.blog.controller;

import esprit.tn.blog.entity.ForumPost;
import esprit.tn.blog.service.ForumPostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/forums")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ForumPostController {

    private final ForumPostService forumPostService;

    @PostMapping("/create-forum")
    public ResponseEntity<ForumPost> createPost(@RequestBody ForumPost post) {
        return ResponseEntity.ok(forumPostService.createPost(post));
    }

    @GetMapping("/get-all-forums")
    public ResponseEntity<List<ForumPost>> getAllPosts() {
        return ResponseEntity.ok(forumPostService.getAllPosts());
    }

    @GetMapping("/get-forum-by-id/{id}")
    public ResponseEntity<ForumPost> getPostById(@PathVariable Long id) {
        return ResponseEntity.ok(forumPostService.getPostById(id));
    }

    @GetMapping("/get-top-level-forums")
    public ResponseEntity<List<ForumPost>> getTopLevelPosts() {
        return ResponseEntity.ok(forumPostService.getTopLevelPosts());
    }

    @GetMapping("/get-forums-by-topic/{topicId}")
    public ResponseEntity<List<ForumPost>> getPostsByTopic(@PathVariable Long topicId) {
        return ResponseEntity.ok(forumPostService.getPostsByTopic(topicId));
    }

    @GetMapping("/get-forums-by-user/{userId}")
    public ResponseEntity<List<ForumPost>> getPostsByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(forumPostService.getPostsByUser(userId));
    }

    @GetMapping("/get-replies/{id}")
    public ResponseEntity<List<ForumPost>> getReplies(@PathVariable Long id) {
        return ResponseEntity.ok(forumPostService.getReplies(id));
    }

    @PutMapping("/update-forum/{id}")
    public ResponseEntity<ForumPost> updatePost(@PathVariable Long id, @RequestBody ForumPost post) {
        return ResponseEntity.ok(forumPostService.updatePost(id, post));
    }

    @DeleteMapping("/delete-forum/{id}")
    public ResponseEntity<Void> deletePost(@PathVariable Long id) {
        forumPostService.deletePost(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/like-forum/{id}")
    public ResponseEntity<ForumPost> likePost(@PathVariable Long id) {
        return ResponseEntity.ok(forumPostService.likePost(id));
    }

    @PutMapping("/unlike-forum/{id}")
    public ResponseEntity<ForumPost> unlikePost(@PathVariable Long id) {
        return ResponseEntity.ok(forumPostService.unlikePost(id));
    }

    @PutMapping("/repost-forum/{id}")
    public ResponseEntity<ForumPost> repostPost(@PathVariable Long id) {
        return ResponseEntity.ok(forumPostService.repostPost(id));
    }
}
