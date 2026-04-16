package esprit.tn.blog.controller;

import esprit.tn.blog.dto.FriendRequestDto;
import esprit.tn.blog.entity.FriendRequest;
import esprit.tn.blog.service.FriendRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/friends")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class FriendRequestController {

    private final FriendRequestService friendRequestService;

    /**
     * Send a friend request
     */
    @PostMapping("/request")
    public ResponseEntity<FriendRequest> sendFriendRequest(@RequestBody FriendRequestDto dto) {
        log.info("Sending friend request from {} to {}", dto.getSenderId(), dto.getReceiverId());
        FriendRequest request = friendRequestService.sendFriendRequest(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(request);
    }

    /**
     * Get pending requests received by user
     */
    @GetMapping("/requests/received/{userId}")
    public ResponseEntity<List<FriendRequest>> getPendingRequestsReceived(@PathVariable Long userId) {
        List<FriendRequest> requests = friendRequestService.getPendingRequestsReceived(userId);
        return ResponseEntity.ok(requests);
    }

    /**
     * Get pending requests sent by user
     */
    @GetMapping("/requests/sent/{userId}")
    public ResponseEntity<List<FriendRequest>> getPendingRequestsSent(@PathVariable Long userId) {
        List<FriendRequest> requests = friendRequestService.getPendingRequestsSent(userId);
        return ResponseEntity.ok(requests);
    }

    /**
     * Accept friend request
     */
    @PostMapping("/requests/{requestId}/accept")
    public ResponseEntity<Map<String, String>> acceptFriendRequest(@PathVariable Long requestId) {
        log.info("Accepting friend request: {}", requestId);
        friendRequestService.acceptFriendRequest(requestId);
        return ResponseEntity.ok(Map.of("message", "Friend request accepted"));
    }

    /**
     * Reject friend request
     */
    @PostMapping("/requests/{requestId}/reject")
    public ResponseEntity<Map<String, String>> rejectFriendRequest(@PathVariable Long requestId) {
        log.info("Rejecting friend request: {}", requestId);
        friendRequestService.rejectFriendRequest(requestId);
        return ResponseEntity.ok(Map.of("message", "Friend request rejected"));
    }

    /**
     * Cancel friend request
     */
    @DeleteMapping("/requests/{requestId}")
    public ResponseEntity<Void> cancelFriendRequest(@PathVariable Long requestId) {
        log.info("Cancelling friend request: {}", requestId);
        friendRequestService.cancelFriendRequest(requestId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Count pending requests for user
     */
    @GetMapping("/requests/received/{userId}/count")
    public ResponseEntity<Map<String, Long>> countPendingRequests(@PathVariable Long userId) {
        Long count = friendRequestService.countPendingRequests(userId);
        return ResponseEntity.ok(Map.of("count", count));
    }
}
