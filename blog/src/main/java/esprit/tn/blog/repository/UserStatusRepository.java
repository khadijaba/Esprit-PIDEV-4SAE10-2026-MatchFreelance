package esprit.tn.blog.repository;

import esprit.tn.blog.entity.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface UserStatusRepository extends JpaRepository<UserStatus, Long> {

    @Query("SELECT s FROM UserStatus s WHERE s.userId IN :ids")
    List<UserStatus> findByUserIds(@Param("ids") List<Long> userIds);

    @Query("SELECT s FROM UserStatus s WHERE s.isOnline = true")
    List<UserStatus> findOnlineUsers();
}
