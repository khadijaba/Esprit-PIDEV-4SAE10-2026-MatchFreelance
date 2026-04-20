package esprit.tn.blog.controller;

import esprit.tn.blog.entity.Friendship;
import esprit.tn.blog.service.FriendshipService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/forums")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class FriendshipController {

    private final FriendshipService friendshipService;

    @PostMapping("/send-friend-request")
    public ResponseEntity<?> sendRequest(@RequestBody Friendship friendship) {
        try {
            return ResponseEntity.ok(friendshipService.sendRequest(friendship));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Failed to send request"));
        }
    }

    @PutMapping("/accept-friend-request/{id}")
    public ResponseEntity<?> acceptRequest(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(friendshipService.acceptRequest(id));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Failed to accept request"));
        }
    }

    @PutMapping("/reject-friend-request/{id}")
    public ResponseEntity<?> rejectRequest(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(friendshipService.rejectRequest(id));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Failed to reject request"));
        }
    }

    @DeleteMapping("/remove-friend/{id}")
    public ResponseEntity<?> removeFriend(@PathVariable Long id) {
        try {
            friendshipService.removeFriend(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Failed to remove friend"));
        }
    }

    @GetMapping("/get-friends/{userId}")
    public ResponseEntity<List<Friendship>> getAcceptedFriends(@PathVariable Long userId) {
        return ResponseEntity.ok(friendshipService.getAcceptedFriends(userId));
    }

    @GetMapping("/get-pending-requests/{userId}")
    public ResponseEntity<List<Friendship>> getPendingRequests(@PathVariable Long userId) {
        return ResponseEntity.ok(friendshipService.getPendingRequests(userId));
    }

    @GetMapping("/get-sent-requests/{userId}")
    public ResponseEntity<List<Friendship>> getSentRequests(@PathVariable Long userId) {
        return ResponseEntity.ok(friendshipService.getSentRequests(userId));
    }

    @GetMapping("/get-friendship-status/{userId1}/{userId2}")
    public ResponseEntity<?> getFriendshipStatus(@PathVariable Long userId1, @PathVariable Long userId2) {
        Optional<Friendship> f = friendshipService.getFriendshipBetween(userId1, userId2);
        return f.map(v -> ResponseEntity.ok((Object) v)).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/block-user/{id}")
    public ResponseEntity<?> blockUser(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(friendshipService.blockUser(id));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Failed to block user"));
        }
    }

    @PutMapping("/unblock-user/{id}")
    public ResponseEntity<?> unblockUser(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(friendshipService.unblockUser(id));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Failed to unblock user"));
        }
    }

    @GetMapping("/get-blocked-users/{userId}")
    public ResponseEntity<List<Friendship>> getBlockedUsers(@PathVariable Long userId) {
        return ResponseEntity.ok(friendshipService.getBlockedUsers(userId));
    }

    @GetMapping("/get-mutual-friends-count/{userId1}/{userId2}")
    public ResponseEntity<Map<String, Integer>> getMutualFriendsCount(@PathVariable Long userId1, @PathVariable Long userId2) {
        return ResponseEntity.ok(Map.of("count", friendshipService.getMutualFriendsCount(userId1, userId2)));
    }
}
