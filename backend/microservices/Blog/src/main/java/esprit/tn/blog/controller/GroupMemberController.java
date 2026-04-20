package esprit.tn.blog.controller;

import esprit.tn.blog.entity.GroupMember;
import esprit.tn.blog.service.GroupMemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class GroupMemberController {

    private final GroupMemberService memberService;

    /**
     * Add member to group
     */
    @PostMapping("/{groupId}/members")
    public ResponseEntity<GroupMember> addMember(
            @PathVariable Long groupId,
            @RequestBody Map<String, Object> body) {
        Long userId = Long.valueOf(body.get("userId").toString());
        String userName = body.get("userName").toString();
        String userAvatar = body.getOrDefault("userAvatar", "").toString();

        log.info("Adding user {} to group {}", userId, groupId);
        GroupMember member = memberService.addMember(groupId, userId, userName, userAvatar);
        return ResponseEntity.status(HttpStatus.CREATED).body(member);
    }

    /**
     * Get group members
     */
    @GetMapping("/{groupId}/members")
    public ResponseEntity<List<GroupMember>> getGroupMembers(@PathVariable Long groupId) {
        List<GroupMember> members = memberService.getGroupMembers(groupId);
        return ResponseEntity.ok(members);
    }

    /**
     * Remove member from group
     */
    @DeleteMapping("/{groupId}/members/{userId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable Long groupId,
            @PathVariable Long userId) {
        log.info("Removing user {} from group {}", userId, groupId);
        memberService.removeMember(groupId, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Check if user is member of group
     */
    @GetMapping("/{groupId}/members/{userId}/check")
    public ResponseEntity<Map<String, Boolean>> isMember(
            @PathVariable Long groupId,
            @PathVariable Long userId) {
        boolean isMember = memberService.isMember(groupId, userId);
        return ResponseEntity.ok(Map.of("isMember", isMember));
    }

    /**
     * Check if user is admin of group
     */
    @GetMapping("/{groupId}/members/{userId}/is-admin")
    public ResponseEntity<Map<String, Boolean>> isAdmin(
            @PathVariable Long groupId,
            @PathVariable Long userId) {
        boolean isAdmin = memberService.isAdmin(groupId, userId);
        return ResponseEntity.ok(Map.of("isAdmin", isAdmin));
    }

    /**
     * Promote member to admin
     */
    @PutMapping("/{groupId}/members/{userId}/promote")
    public ResponseEntity<GroupMember> promoteToAdmin(
            @PathVariable Long groupId,
            @PathVariable Long userId) {
        log.info("Promoting user {} to admin in group {}", userId, groupId);
        GroupMember member = memberService.promoteToAdmin(groupId, userId);
        return ResponseEntity.ok(member);
    }

    /**
     * Demote admin to member
     */
    @PutMapping("/{groupId}/members/{userId}/demote")
    public ResponseEntity<GroupMember> demoteToMember(
            @PathVariable Long groupId,
            @PathVariable Long userId) {
        log.info("Demoting user {} to member in group {}", userId, groupId);
        GroupMember member = memberService.demoteToMember(groupId, userId);
        return ResponseEntity.ok(member);
    }

    /**
     * Update last read timestamp
     */
    @PutMapping("/{groupId}/members/{userId}/read")
    public ResponseEntity<Void> updateLastRead(
            @PathVariable Long groupId,
            @PathVariable Long userId) {
        memberService.updateLastRead(groupId, userId);
        return ResponseEntity.ok().build();
    }

    /**
     * Mute/unmute group for user
     */
    @PutMapping("/{groupId}/members/{userId}/mute")
    public ResponseEntity<GroupMember> toggleMute(
            @PathVariable Long groupId,
            @PathVariable Long userId) {
        GroupMember member = memberService.toggleMute(groupId, userId);
        return ResponseEntity.ok(member);
    }

    /**
     * Get group admins
     */
    @GetMapping("/{groupId}/admins")
    public ResponseEntity<List<GroupMember>> getGroupAdmins(@PathVariable Long groupId) {
        List<GroupMember> admins = memberService.getGroupAdmins(groupId);
        return ResponseEntity.ok(admins);
    }

    /**
     * Count group members
     */
    @GetMapping("/{groupId}/members/count")
    public ResponseEntity<Map<String, Long>> countMembers(@PathVariable Long groupId) {
        Long count = memberService.countGroupMembers(groupId);
        return ResponseEntity.ok(Map.of("count", count));
    }
}
