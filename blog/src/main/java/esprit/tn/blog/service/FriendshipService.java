package esprit.tn.blog.service;

import esprit.tn.blog.entity.Friendship;
import esprit.tn.blog.entity.FriendshipStatus;
import esprit.tn.blog.repository.FriendshipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FriendshipService {

    private final FriendshipRepository friendshipRepository;

    public Friendship sendRequest(Friendship friendship) {
        Optional<Friendship> existing = friendshipRepository.findFriendshipBetween(
                friendship.getUserId(), friendship.getFriendId());
        if (existing.isPresent()) {
            Friendship f = existing.get();
            if (f.getStatus() == FriendshipStatus.REJECTED) {
                f.setStatus(FriendshipStatus.PENDING);
                f.setUserId(friendship.getUserId());
                f.setUserName(friendship.getUserName());
                f.setUserAvatar(friendship.getUserAvatar());
                f.setFriendId(friendship.getFriendId());
                f.setFriendName(friendship.getFriendName());
                f.setFriendAvatar(friendship.getFriendAvatar());
                return friendshipRepository.save(f);
            }
            return f;
        }
        friendship.setStatus(FriendshipStatus.PENDING);
        return friendshipRepository.save(friendship);
    }

    public Friendship acceptRequest(Long friendshipId) {
        Friendship f = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new RuntimeException("Friendship not found"));
        f.setStatus(FriendshipStatus.ACCEPTED);
        return friendshipRepository.save(f);
    }

    public Friendship rejectRequest(Long friendshipId) {
        Friendship f = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new RuntimeException("Friendship not found"));
        f.setStatus(FriendshipStatus.REJECTED);
        return friendshipRepository.save(f);
    }

    public void removeFriend(Long friendshipId) {
        friendshipRepository.deleteById(friendshipId);
    }

    public List<Friendship> getAcceptedFriends(Long userId) {
        return friendshipRepository.findAcceptedFriends(userId);
    }

    public List<Friendship> getPendingRequests(Long userId) {
        return friendshipRepository.findPendingRequestsForUser(userId);
    }

    public List<Friendship> getSentRequests(Long userId) {
        return friendshipRepository.findPendingSentByUser(userId);
    }

    public Optional<Friendship> getFriendshipBetween(Long userId1, Long userId2) {
        return friendshipRepository.findFriendshipBetween(userId1, userId2);
    }

    public Friendship blockUser(Long friendshipId) {
        Friendship f = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new RuntimeException("Friendship not found"));
        f.setStatus(FriendshipStatus.BLOCKED);
        return friendshipRepository.save(f);
    }

    public Friendship unblockUser(Long friendshipId) {
        Friendship f = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new RuntimeException("Friendship not found"));
        f.setStatus(FriendshipStatus.ACCEPTED);
        return friendshipRepository.save(f);
    }

    public List<Friendship> getBlockedUsers(Long userId) {
        return friendshipRepository.findByUserAndStatus(userId, FriendshipStatus.BLOCKED);
    }

    public int getMutualFriendsCount(Long userId1, Long userId2) {
        List<Friendship> friends1 = friendshipRepository.findAcceptedFriends(userId1);
        List<Friendship> friends2 = friendshipRepository.findAcceptedFriends(userId2);
        java.util.Set<Long> friendIds1 = new java.util.HashSet<>();
        for (Friendship f : friends1) {
            friendIds1.add(f.getUserId().equals(userId1) ? f.getFriendId() : f.getUserId());
        }
        int count = 0;
        for (Friendship f : friends2) {
            Long fId = f.getUserId().equals(userId2) ? f.getFriendId() : f.getUserId();
            if (friendIds1.contains(fId)) count++;
        }
        return count;
    }
}
