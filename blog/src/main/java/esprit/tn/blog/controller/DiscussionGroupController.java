package esprit.tn.blog.controller;

import esprit.tn.blog.dto.DiscussionGroupRequest;
import esprit.tn.blog.entity.DiscussionGroup;
import esprit.tn.blog.service.DiscussionGroupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class DiscussionGroupController {

    private final DiscussionGroupService groupService;

    /**
     * Create a new discussion group
     */
    @PostMapping
    public ResponseEntity<DiscussionGroup> createGroup(@RequestBody DiscussionGroupRequest request) {
        log.info("Creating discussion group: {}", request.getName());
        DiscussionGroup group = groupService.createGroup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(group);
    }

    /**
     * Get all active groups
     */
    @GetMapping
    public ResponseEntity<List<DiscussionGroup>> getAllGroups() {
        List<DiscussionGroup> groups = groupService.getAllGroups();
        return ResponseEntity.ok(groups);
    }

    /**
     * Get public groups
     */
    @GetMapping("/public")
    public ResponseEntity<List<DiscussionGroup>> getPublicGroups() {
        List<DiscussionGroup> groups = groupService.getPublicGroups();
        return ResponseEntity.ok(groups);
    }

    /**
     * Get group by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<DiscussionGroup> getGroupById(@PathVariable Long id) {
        DiscussionGroup group = groupService.getGroupById(id);
        return ResponseEntity.ok(group);
    }

    /**
     * Get groups by topic
     */
    @GetMapping("/topic/{topic}")
    public ResponseEntity<List<DiscussionGroup>> getGroupsByTopic(@PathVariable String topic) {
        List<DiscussionGroup> groups = groupService.getGroupsByTopic(topic);
        return ResponseEntity.ok(groups);
    }

    /**
     * Get groups created by user
     */
    @GetMapping("/creator/{creatorId}")
    public ResponseEntity<List<DiscussionGroup>> getGroupsByCreator(@PathVariable Long creatorId) {
        List<DiscussionGroup> groups = groupService.getGroupsByCreator(creatorId);
        return ResponseEntity.ok(groups);
    }

    /**
     * Get user's groups (where user is a member)
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<DiscussionGroup>> getUserGroups(@PathVariable Long userId) {
        List<DiscussionGroup> groups = groupService.getUserGroups(userId);
        return ResponseEntity.ok(groups);
    }

    /**
     * Update group
     */
    @PutMapping("/{id}")
    public ResponseEntity<DiscussionGroup> updateGroup(
            @PathVariable Long id,
            @RequestBody DiscussionGroupRequest request) {
        log.info("Updating group: {}", id);
        DiscussionGroup group = groupService.updateGroup(id, request);
        return ResponseEntity.ok(group);
    }

    /**
     * Delete group
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGroup(@PathVariable Long id) {
        log.info("Deleting group: {}", id);
        groupService.deleteGroup(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Search groups by name
     */
    @GetMapping("/search")
    public ResponseEntity<List<DiscussionGroup>> searchGroups(@RequestParam String q) {
        List<DiscussionGroup> groups = groupService.searchGroups(q);
        return ResponseEntity.ok(groups);
    }

    /**
     * Get popular groups
     */
    @GetMapping("/popular")
    public ResponseEntity<List<DiscussionGroup>> getPopularGroups() {
        List<DiscussionGroup> groups = groupService.getPopularGroups();
        return ResponseEntity.ok(groups);
    }
}
