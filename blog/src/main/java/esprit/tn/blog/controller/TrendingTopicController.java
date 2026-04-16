package esprit.tn.blog.controller;

import esprit.tn.blog.entity.TrendingTopic;
import esprit.tn.blog.service.TrendingTopicService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/forums")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TrendingTopicController {

    private final TrendingTopicService trendingTopicService;

    @PostMapping("/create-topic")
    public ResponseEntity<TrendingTopic> createTopic(@RequestBody TrendingTopic topic) {
        return ResponseEntity.ok(trendingTopicService.createTopic(topic));
    }

    @GetMapping("/get-all-topics")
    public ResponseEntity<List<TrendingTopic>> getAllTopics() {
        return ResponseEntity.ok(trendingTopicService.getAllTopics());
    }

    @GetMapping("/get-topic-by-id/{id}")
    public ResponseEntity<TrendingTopic> getTopicById(@PathVariable Long id) {
        return ResponseEntity.ok(trendingTopicService.getTopicById(id));
    }

    @GetMapping("/get-topics-by-category/{category}")
    public ResponseEntity<List<TrendingTopic>> getTopicsByCategory(@PathVariable String category) {
        return ResponseEntity.ok(trendingTopicService.getTopicsByCategory(category));
    }

    @GetMapping("/get-pinned-topics")
    public ResponseEntity<List<TrendingTopic>> getPinnedTopics() {
        return ResponseEntity.ok(trendingTopicService.getPinnedTopics());
    }

    @GetMapping("/get-trending-topics")
    public ResponseEntity<List<TrendingTopic>> getTrendingTopics() {
        return ResponseEntity.ok(trendingTopicService.getTrendingTopics());
    }

    @PutMapping("/update-topic/{id}")
    public ResponseEntity<TrendingTopic> updateTopic(@PathVariable Long id, @RequestBody TrendingTopic topic) {
        return ResponseEntity.ok(trendingTopicService.updateTopic(id, topic));
    }

    @DeleteMapping("/delete-topic/{id}")
    public ResponseEntity<Void> deleteTopic(@PathVariable Long id) {
        trendingTopicService.deleteTopic(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/increment-topic-view-count/{id}")
    public ResponseEntity<TrendingTopic> incrementViewCount(@PathVariable Long id) {
        return ResponseEntity.ok(trendingTopicService.incrementViewCount(id));
    }

    @PutMapping("/increment-topic-post-count/{id}")
    public ResponseEntity<TrendingTopic> incrementPostCount(@PathVariable Long id) {
        return ResponseEntity.ok(trendingTopicService.incrementPostCount(id));
    }
}
