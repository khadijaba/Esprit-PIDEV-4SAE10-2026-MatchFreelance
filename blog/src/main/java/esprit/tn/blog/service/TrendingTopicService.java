package esprit.tn.blog.service;

import esprit.tn.blog.entity.TrendingTopic;
import esprit.tn.blog.repository.TrendingTopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TrendingTopicService {

    private final TrendingTopicRepository trendingTopicRepository;

    public TrendingTopic createTopic(TrendingTopic topic) {
        return trendingTopicRepository.save(topic);
    }

    public List<TrendingTopic> getAllTopics() {
        return trendingTopicRepository.findAllByOrderByPostCountDescViewCountDesc();
    }

    public TrendingTopic getTopicById(Long id) {
        return trendingTopicRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Topic not found with id: " + id));
    }

    public List<TrendingTopic> getTopicsByCategory(String category) {
        return trendingTopicRepository.findByCategory(category);
    }

    public List<TrendingTopic> getPinnedTopics() {
        return trendingTopicRepository.findByIsPinnedTrue();
    }

    public List<TrendingTopic> getTrendingTopics() {
        return trendingTopicRepository.findAllByOrderByPostCountDescViewCountDesc();
    }

    public TrendingTopic updateTopic(Long id, TrendingTopic updatedTopic) {
        TrendingTopic existing = getTopicById(id);
        existing.setCategory(updatedTopic.getCategory());
        existing.setTitle(updatedTopic.getTitle());
        existing.setIsPinned(updatedTopic.getIsPinned());
        return trendingTopicRepository.save(existing);
    }

    public void deleteTopic(Long id) {
        if (!trendingTopicRepository.existsById(id))
            throw new RuntimeException("Topic not found with id: " + id);
        trendingTopicRepository.deleteById(id);
    }

    public TrendingTopic incrementViewCount(Long id) {
        TrendingTopic topic = getTopicById(id);
        topic.setViewCount(topic.getViewCount() + 1);
        return trendingTopicRepository.save(topic);
    }

    public TrendingTopic incrementPostCount(Long id) {
        TrendingTopic topic = getTopicById(id);
        topic.setPostCount(topic.getPostCount() + 1);
        return trendingTopicRepository.save(topic);
    }
}
