package esprit.tn.blog.service;

import esprit.tn.blog.dto.DiscussionGroupRequest;
import esprit.tn.blog.entity.DiscussionGroup;
import esprit.tn.blog.entity.GroupMember;
import esprit.tn.blog.repository.DiscussionGroupRepository;
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
public class DiscussionGroupService {

    private final DiscussionGroupRepository groupRepository;
    private final GroupMemberRepository memberRepository;

    /**
     * Create a new discussion group
     */
    @Transactional
    public DiscussionGroup createGroup(DiscussionGroupRequest request) {
        log.info("Creating discussion group: {}", request.getName());

        DiscussionGroup group = DiscussionGroup.builder()
                .name(request.getName())
                .description(request.getDescription())
                .topic(request.getTopic())
                .logoUrl(request.getLogoUrl())
                .creatorId(request.getCreatorId())
                .creatorName(request.getCreatorName())
                .isPrivate(request.getIsPrivate() != null ? request.getIsPrivate() : false)
                .allowMemberInvites(request.getAllowMemberInvites() != null ? request.getAllowMemberInvites() : true)
                .allowFileSharing(request.getAllowFileSharing() != null ? request.getAllowFileSharing() : true)
                .allowGifs(request.getAllowGifs() != null ? request.getAllowGifs() : true)
                .allowEmojis(request.getAllowEmojis() != null ? request.getAllowEmojis() : true)
                .memberCount(1)
                .messageCount(0)
                .build();

        DiscussionGroup savedGroup = groupRepository.save(group);

        // Add creator as admin member
        GroupMember creatorMember = GroupMember.builder()
                .group(savedGroup)
                .userId(request.getCreatorId())
                .userName(request.getCreatorName())
                .role(GroupMember.MemberRole.ADMIN)
                .build();

        memberRepository.save(creatorMember);

        log.info("Discussion group created with ID: {}", savedGroup.getId());
        return savedGroup;
    }

    /**
     * Get all active groups
     */
    public List<DiscussionGroup> getAllGroups() {
        return groupRepository.findByIsActiveTrueOrderByLastActivityAtDesc();
    }

    /**
     * Get public groups
     */
    public List<DiscussionGroup> getPublicGroups() {
        return groupRepository.findByIsPrivateFalseAndIsActiveTrueOrderByLastActivityAtDesc();
    }

    /**
     * Get groups by topic
     */
    public List<DiscussionGroup> getGroupsByTopic(String topic) {
        return groupRepository.findByTopicAndIsActiveTrueOrderByLastActivityAtDesc(topic);
    }

    /**
     * Get group by ID
     */
    public DiscussionGroup getGroupById(Long groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found with ID: " + groupId));
    }

    /**
     * Get groups created by user
     */
    public List<DiscussionGroup> getGroupsByCreator(Long creatorId) {
        return groupRepository.findByCreatorIdOrderByCreatedAtDesc(creatorId);
    }

    /**
     * Get user's groups (where user is a member)
     */
    public List<DiscussionGroup> getUserGroups(Long userId) {
        List<GroupMember> memberships = memberRepository.findByUserIdAndIsActiveTrueOrderByJoinedAtDesc(userId);
        return memberships.stream()
                .map(GroupMember::getGroup)
                .filter(DiscussionGroup::getIsActive)
                .toList();
    }

    /**
     * Update group
     */
    @Transactional
    public DiscussionGroup updateGroup(Long groupId, DiscussionGroupRequest request) {
        DiscussionGroup group = getGroupById(groupId);

        if (request.getName() != null) group.setName(request.getName());
        if (request.getDescription() != null) group.setDescription(request.getDescription());
        if (request.getTopic() != null) group.setTopic(request.getTopic());
        if (request.getLogoUrl() != null) group.setLogoUrl(request.getLogoUrl());
        if (request.getIsPrivate() != null) group.setIsPrivate(request.getIsPrivate());
        if (request.getAllowMemberInvites() != null) group.setAllowMemberInvites(request.getAllowMemberInvites());
        if (request.getAllowFileSharing() != null) group.setAllowFileSharing(request.getAllowFileSharing());
        if (request.getAllowGifs() != null) group.setAllowGifs(request.getAllowGifs());
        if (request.getAllowEmojis() != null) group.setAllowEmojis(request.getAllowEmojis());

        return groupRepository.save(group);
    }

    /**
     * Delete group (soft delete)
     */
    @Transactional
    public void deleteGroup(Long groupId) {
        DiscussionGroup group = getGroupById(groupId);
        group.setIsActive(false);
        groupRepository.save(group);
        log.info("Group deleted (soft): {}", groupId);
    }

    /**
     * Update group activity timestamp
     */
    @Transactional
    public void updateGroupActivity(Long groupId) {
        DiscussionGroup group = getGroupById(groupId);
        group.setLastActivityAt(LocalDateTime.now());
        groupRepository.save(group);
    }

    /**
     * Increment message count
     */
    @Transactional
    public void incrementMessageCount(Long groupId) {
        DiscussionGroup group = getGroupById(groupId);
        group.setMessageCount(group.getMessageCount() + 1);
        group.setLastActivityAt(LocalDateTime.now());
        groupRepository.save(group);
    }

    /**
     * Update member count
     */
    @Transactional
    public void updateMemberCount(Long groupId) {
        Long count = memberRepository.countByGroupIdAndIsActiveTrue(groupId);
        DiscussionGroup group = getGroupById(groupId);
        group.setMemberCount(count.intValue());
        groupRepository.save(group);
    }

    /**
     * Search groups by name
     */
    public List<DiscussionGroup> searchGroups(String searchTerm) {
        return groupRepository.searchByName(searchTerm);
    }

    /**
     * Get popular groups
     */
    public List<DiscussionGroup> getPopularGroups() {
        return groupRepository.findTop10ByIsActiveTrueOrderByMemberCountDesc();
    }
}
