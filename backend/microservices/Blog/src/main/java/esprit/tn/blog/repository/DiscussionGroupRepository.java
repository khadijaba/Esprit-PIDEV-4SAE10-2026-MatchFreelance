package esprit.tn.blog.repository;

import esprit.tn.blog.entity.DiscussionGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DiscussionGroupRepository extends JpaRepository<DiscussionGroup, Long> {

    // Find all active groups
    List<DiscussionGroup> findByIsActiveTrueOrderByLastActivityAtDesc();

    // Find groups by topic
    List<DiscussionGroup> findByTopicAndIsActiveTrueOrderByLastActivityAtDesc(String topic);

    // Find groups created by user
    List<DiscussionGroup> findByCreatorIdOrderByCreatedAtDesc(Long creatorId);

    // Find public groups
    List<DiscussionGroup> findByIsPrivateFalseAndIsActiveTrueOrderByLastActivityAtDesc();

    // Search groups by name
    @Query("SELECT g FROM DiscussionGroup g WHERE " +
           "LOWER(g.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) AND " +
           "g.isActive = true ORDER BY g.lastActivityAt DESC")
    List<DiscussionGroup> searchByName(String searchTerm);

    // Find popular groups (by member count)
    List<DiscussionGroup> findTop10ByIsActiveTrueOrderByMemberCountDesc();

    // Find groups by IDs
    List<DiscussionGroup> findByIdInAndIsActiveTrueOrderByLastActivityAtDesc(List<Long> ids);
}
