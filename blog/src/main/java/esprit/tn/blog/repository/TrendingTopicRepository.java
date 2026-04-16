package esprit.tn.blog.repository;

import esprit.tn.blog.entity.TrendingTopic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TrendingTopicRepository extends JpaRepository<TrendingTopic, Long> {
    List<TrendingTopic> findByCategory(String category);
    List<TrendingTopic> findByIsPinnedTrue();
    List<TrendingTopic> findAllByOrderByPostCountDesc();
    List<TrendingTopic> findAllByOrderByPostCountDescViewCountDesc();
}
