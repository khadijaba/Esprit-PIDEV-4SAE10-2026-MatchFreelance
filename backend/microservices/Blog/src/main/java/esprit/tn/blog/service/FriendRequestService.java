package esprit.tn.blog.service;

import esprit.tn.blog.dto.FriendRequestDto;
import esprit.tn.blog.entity.FriendRequest;
import esprit.tn.blog.entity.Friendship;
import esprit.tn.blog.repository.FriendRequestRepository;
import esprit.tn.blog.repository.FriendshipRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FriendRequestService {

    private final FriendRequestRepository requestRepository;
    private final FriendshipRepository friendshipRepository;

    /**
     * Send a friend request
     */
    @Transactional
    public FriendRequest sendFriendRequest(FriendRequestDto dto) {
        log.info("Sending friend request from {} to {}", dto.getSenderId(), dto.getReceiverId());

        // Check if already friends
        Optional<Friendship> existingFriendship = friendshipRepository.findFriendshipBetween(
                dto.getSenderId(), dto.getReceiverId());
        if (existingFriendship.isPresent() && 
            existingFriendship.get().getStatus() == esprit.tn.blog.entity.FriendshipStatus.ACCEPTED) {
            throw new RuntimeException("Users are already friends");
        }

        // Check if pending request exists
        if (requestRepository.existsPendingRequestBetweenUsers(dto.getSenderId(), dto.getReceiverId())) {
            throw new RuntimeException("Friend request already pending");
        }

        FriendRequest request = FriendRequest.builder()
                .senderId(dto.getSenderId())
                .receiverId(dto.getReceiverId())
                .senderName(dto.getSenderName())
                .receiverName(dto.getReceiverName())
                .senderAvatar(dto.getSenderAvatar())
                .message(dto.getMessage())
                .status(FriendRequest.RequestStatus.PENDING)
                .build();

        return requestRepository.save(request);
    }

    /**
     * Accept friend request
     */
    @Transactional
    public void acceptFriendRequest(Long requestId) {
        FriendRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Friend request not found"));

        if (request.getStatus() != FriendRequest.RequestStatus.PENDING) {
            throw new RuntimeException("Friend request is not pending");
        }

        // Update request status
        request.setStatus(FriendRequest.RequestStatus.ACCEPTED);
        request.setRespondedAt(LocalDateTime.now());
        requestRepository.save(request);

        // Create friendship (bidirectional)
        createFriendship(request.getSenderId(), request.getReceiverId(), 
                        request.getSenderName(), request.getReceiverName());

        log.info("Friend request accepted: {} and {} are now friends", 
                request.getSenderId(), request.getReceiverId());
    }

    /**
     * Reject friend request
     */
    @Transactional
    public void rejectFriendRequest(Long requestId) {
        FriendRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Friend request not found"));

        if (request.getStatus() != FriendRequest.RequestStatus.PENDING) {
            throw new RuntimeException("Friend request is not pending");
        }

        request.setStatus(FriendRequest.RequestStatus.REJECTED);
        request.setRespondedAt(LocalDateTime.now());
        requestRepository.save(request);

        log.info("Friend request rejected: {}", requestId);
    }

    /**
     * Cancel friend request
     */
    @Transactional
    public void cancelFriendRequest(Long requestId) {
        FriendRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Friend request not found"));

        if (request.getStatus() != FriendRequest.RequestStatus.PENDING) {
            throw new RuntimeException("Friend request is not pending");
        }

        request.setStatus(FriendRequest.RequestStatus.CANCELLED);
        request.setRespondedAt(LocalDateTime.now());
        requestRepository.save(request);

        log.info("Friend request cancelled: {}", requestId);
    }

    /**
     * Get pending requests received by user
     */
    public List<FriendRequest> getPendingRequestsReceived(Long userId) {
        return requestRepository.findByReceiverIdAndStatusOrderBySentAtDesc(
                userId, FriendRequest.RequestStatus.PENDING);
    }

    /**
     * Get pending requests sent by user
     */
    public List<FriendRequest> getPendingRequestsSent(Long userId) {
        return requestRepository.findBySenderIdAndStatusOrderBySentAtDesc(
                userId, FriendRequest.RequestStatus.PENDING);
    }

    /**
     * Count pending requests for user
     */
    public Long countPendingRequests(Long userId) {
        return requestRepository.countByReceiverIdAndStatus(
                userId, FriendRequest.RequestStatus.PENDING);
    }

    /**
     * Create friendship (helper method)
     */
    private void createFriendship(Long userId1, Long userId2, String userName1, String userName2) {
        // Create friendship from user1 to user2
        Friendship friendship1 = Friendship.builder()
                .userId(userId1)
                .friendId(userId2)
                .friendName(userName2)
                .status(esprit.tn.blog.entity.FriendshipStatus.ACCEPTED)
                .build();

        // Create friendship from user2 to user1
        Friendship friendship2 = Friendship.builder()
                .userId(userId2)
                .friendId(userId1)
                .friendName(userName1)
                .status(esprit.tn.blog.entity.FriendshipStatus.ACCEPTED)
                .build();

        friendshipRepository.save(friendship1);
        friendshipRepository.save(friendship2);
    }
}
