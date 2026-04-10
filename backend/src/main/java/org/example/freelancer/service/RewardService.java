package org.example.freelancer.service;

import org.example.freelancer.entity.Reward;
import org.example.freelancer.entity.RewardStatus;
import org.example.freelancer.repository.RewardRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class RewardService {

    private final RewardRepository rewardRepository;

    public RewardService(RewardRepository rewardRepository) {
        this.rewardRepository = rewardRepository;
    }

    public Reward award(Reward reward) {
        return rewardRepository.save(reward);
    }

    public Optional<Reward> getById(Long id) {
        return rewardRepository.findById(id);
    }

    public List<Reward> getByRecipient(Long recipientId) {
        return rewardRepository.findByRecipientId(recipientId);
    }

    public List<Reward> getVisibleByRecipient(Long recipientId) {
        return rewardRepository.findByRecipientIdAndVisibleOnProfileTrue(recipientId);
    }

    public List<Reward> getByEvent(Long eventId) {
        return rewardRepository.findByEventId(eventId);
    }

    public Reward revoke(Long id) {
        return rewardRepository.findById(id).map(reward -> {
            reward.setStatus(RewardStatus.REVOKED);
            return rewardRepository.save(reward);
        }).orElseThrow(() -> new RuntimeException("Reward not found with id " + id));
    }

    public void delete(Long id) {
        rewardRepository.deleteById(id);
    }
}
