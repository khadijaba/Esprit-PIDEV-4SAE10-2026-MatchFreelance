package esprit.tn.blog.service;

import esprit.tn.blog.entity.DiscussionGroup;
import esprit.tn.blog.entity.GroupMember;
import esprit.tn.blog.repository.GroupMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroupMemberService {

    private final GroupMemberRepository memberRepository;
    private final DiscussionGroupService groupService;

    /**
     * Add member to group
     */
    @Transactional
    public GroupMember addMember(Long groupId, Long userId, String userName, String userAvatar) {
        log.info("Adding user {} to group {}", userId, groupId);

        // Check if already a member
        if (memberRepository.existsByGroupIdAndUserId(groupId, userId)) {
            throw new RuntimeException("User is already a member of this group");
        }

        DiscussionGroup group = groupService.getGroupById(groupId);

        GroupMember member = GroupMember.builder()
                .groupId(groupId)
                .userId(userId)
                .userName(userName)
                .userAvatar(userAvatar)
                .role(GroupMember.MemberRole.MEMBER)
                .build();

        GroupMember savedMember = memberRepository.save(member);

        // Update group member count
        groupService.updateMemberCount(groupId);

        return savedMember;
    }

    /**
     * Remove member from group
     */
    @Transactional
    public void removeMember(Long groupId, Long userId) {
        GroupMember member = memberRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        member.setIsActive(false);
        memberRepository.save(member);

        // Update group member count
        groupService.updateMemberCount(groupId);

        log.info("User {} removed from group {}", userId, groupId);
    }

    /**
     * Get group members
     */
    public List<GroupMember> getGroupMembers(Long groupId) {
        return memberRepository.findByGroupIdAndIsActiveTrue(groupId);
    }

    /**
     * Check if user is member of group
     */
    public boolean isMember(Long groupId, Long userId) {
        return memberRepository.existsByGroupIdAndUserId(groupId, userId);
    }

    /**
     * Check if user is admin of group
     */
    public boolean isAdmin(Long groupId, Long userId) {
        return memberRepository.findByGroupIdAndUserId(groupId, userId)
                .map(member -> member.getRole() == GroupMember.MemberRole.ADMIN)
                .orElse(false);
    }

    /**
     * Promote member to admin
     */
    @Transactional
    public GroupMember promoteToAdmin(Long groupId, Long userId) {
        GroupMember member = memberRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        member.setRole(GroupMember.MemberRole.ADMIN);
        return memberRepository.save(member);
    }

    /**
     * Demote admin to member
     */
    @Transactional
    public GroupMember demoteToMember(Long groupId, Long userId) {
        GroupMember member = memberRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        member.setRole(GroupMember.MemberRole.MEMBER);
        return memberRepository.save(member);
    }

    /**
     * Update last read timestamp
     */
    @Transactional
    public void updateLastRead(Long groupId, Long userId) {
        GroupMember member = memberRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        member.setLastReadAt(LocalDateTime.now());
        memberRepository.save(member);
    }

    /**
     * Mute/unmute group for user
     */
    @Transactional
    public GroupMember toggleMute(Long groupId, Long userId) {
        GroupMember member = memberRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        member.setIsMuted(!member.getIsMuted());
        return memberRepository.save(member);
    }

    /**
     * Get group admins
     */
    public List<GroupMember> getGroupAdmins(Long groupId) {
        return memberRepository.findByGroupIdAndRoleAndIsActiveTrue(
                groupId, GroupMember.MemberRole.ADMIN);
    }

    /**
     * Count group members
     */
    public Long countGroupMembers(Long groupId) {
        return memberRepository.countByGroupIdAndIsActiveTrue(groupId);
    }
}
