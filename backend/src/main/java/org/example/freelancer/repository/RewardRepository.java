package org.example.freelancer.repository;

import org.example.freelancer.entity.Reward;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RewardRepository extends JpaRepository<Reward, Long> {

    List<Reward> findByRecipientId(Long recipientId);

    List<Reward> findByRecipientIdAndVisibleOnProfileTrue(Long recipientId);

    List<Reward> findByEventId(Long eventId);
}
