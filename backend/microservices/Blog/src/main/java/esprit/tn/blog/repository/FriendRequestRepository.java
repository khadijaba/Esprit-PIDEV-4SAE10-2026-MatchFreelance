package esprit.tn.blog.repository;

import esprit.tn.blog.entity.FriendRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendRequestRepository extends JpaRepository<FriendRequest, Long> {

    // Find pending requests received by user
    List<FriendRequest> findByReceiverIdAndStatusOrderBySentAtDesc(
            Long receiverId, FriendRequest.RequestStatus status);

    // Find pending requests sent by user
    List<FriendRequest> findBySenderIdAndStatusOrderBySentAtDesc(
            Long senderId, FriendRequest.RequestStatus status);

    // Find request between two users
    @Query("SELECT fr FROM FriendRequest fr WHERE " +
           "((fr.senderId = :userId1 AND fr.receiverId = :userId2) OR " +
           "(fr.senderId = :userId2 AND fr.receiverId = :userId1)) AND " +
           "fr.status = :status")
    Optional<FriendRequest> findRequestBetweenUsers(
            Long userId1, Long userId2, FriendRequest.RequestStatus status);

    // Check if friend request exists
    @Query("SELECT CASE WHEN COUNT(fr) > 0 THEN true ELSE false END FROM FriendRequest fr WHERE " +
           "((fr.senderId = :userId1 AND fr.receiverId = :userId2) OR " +
           "(fr.senderId = :userId2 AND fr.receiverId = :userId1)) AND " +
           "fr.status = 'PENDING'")
    boolean existsPendingRequestBetweenUsers(Long userId1, Long userId2);

    // Count pending requests for user
    Long countByReceiverIdAndStatus(Long receiverId, FriendRequest.RequestStatus status);
}
