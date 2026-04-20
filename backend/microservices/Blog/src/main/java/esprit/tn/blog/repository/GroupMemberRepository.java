package esprit.tn.blog.repository;

import esprit.tn.blog.entity.GroupMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {

    // Find member by group and user
    Optional<GroupMember> findByGroupIdAndUserId(Long groupId, Long userId);

    // Check if user is member of group
    boolean existsByGroupIdAndUserId(Long groupId, Long userId);

    // Get all members of a group
    List<GroupMember> findByGroupIdAndIsActiveTrue(Long groupId);

    // Get all groups a user is member of
    List<GroupMember> findByUserIdAndIsActiveTrueOrderByJoinedAtDesc(Long userId);

    // Get group admins
    List<GroupMember> findByGroupIdAndRoleAndIsActiveTrue(
            Long groupId, GroupMember.MemberRole role);

    // Count active members in group
    Long countByGroupIdAndIsActiveTrue(Long groupId);

    // Find members with unread messages
    @Query("SELECT gm FROM GroupMember gm WHERE gm.groupId = :groupId AND " +
           "gm.lastReadAt < (SELECT MAX(msg.sentAt) FROM GroupMessage msg WHERE msg.groupId = :groupId)")
    List<GroupMember> findMembersWithUnreadMessages(Long groupId);
}
