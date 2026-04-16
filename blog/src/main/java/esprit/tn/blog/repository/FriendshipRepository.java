package esprit.tn.blog.repository;

import esprit.tn.blog.entity.Friendship;
import esprit.tn.blog.entity.FriendshipStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    @Query("SELECT f FROM Friendship f WHERE (f.userId = :uid OR f.friendId = :uid) AND f.status = 'ACCEPTED'")
    List<Friendship> findAcceptedFriends(@Param("uid") Long userId);

    @Query("SELECT f FROM Friendship f WHERE f.friendId = :uid AND f.status = 'PENDING'")
    List<Friendship> findPendingRequestsForUser(@Param("uid") Long userId);

    @Query("SELECT f FROM Friendship f WHERE f.userId = :uid AND f.status = 'PENDING'")
    List<Friendship> findPendingSentByUser(@Param("uid") Long userId);

    @Query("SELECT f FROM Friendship f WHERE ((f.userId = :uid1 AND f.friendId = :uid2) OR (f.userId = :uid2 AND f.friendId = :uid1))")
    Optional<Friendship> findFriendshipBetween(@Param("uid1") Long userId1, @Param("uid2") Long userId2);

    @Query("SELECT f FROM Friendship f WHERE (f.userId = :uid OR f.friendId = :uid) AND f.status = :status")
    List<Friendship> findByUserAndStatus(@Param("uid") Long userId, @Param("status") FriendshipStatus status);
}
