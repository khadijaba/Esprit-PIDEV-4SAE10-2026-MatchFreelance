package esprit.tn.blog.service;

import esprit.tn.blog.entity.UserStatus;
import esprit.tn.blog.repository.UserStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserStatusService {

    private final UserStatusRepository userStatusRepository;

    public UserStatus setOnline(UserStatus status) {
        Optional<UserStatus> existing = userStatusRepository.findById(status.getUserId());
        if (existing.isPresent()) {
            UserStatus s = existing.get();
            s.setIsOnline(true);
            s.setLastSeen(LocalDateTime.now());
            if (status.getUserName() != null) s.setUserName(status.getUserName());
            if (status.getUserAvatar() != null) s.setUserAvatar(status.getUserAvatar());
            return userStatusRepository.save(s);
        }
        status.setIsOnline(true);
        status.setLastSeen(LocalDateTime.now());
        return userStatusRepository.save(status);
    }

    public UserStatus setOffline(Long userId) {
        Optional<UserStatus> existing = userStatusRepository.findById(userId);
        if (existing.isPresent()) {
            UserStatus s = existing.get();
            s.setIsOnline(false);
            s.setLastSeen(LocalDateTime.now());
            return userStatusRepository.save(s);
        }
        return null;
    }

    public UserStatus getStatus(Long userId) {
        return userStatusRepository.findById(userId).orElse(null);
    }

    public List<UserStatus> getStatuses(List<Long> userIds) {
        return userStatusRepository.findByUserIds(userIds);
    }

    public List<UserStatus> getOnlineUsers() {
        return userStatusRepository.findOnlineUsers();
    }

    public UserStatus setTyping(Long userId, Long typingToUserId) {
        Optional<UserStatus> existing = userStatusRepository.findById(userId);
        if (existing.isPresent()) {
            UserStatus s = existing.get();
            s.setTypingToUserId(typingToUserId);
            s.setTypingStartedAt(LocalDateTime.now());
            return userStatusRepository.save(s);
        }
        return null;
    }

    public UserStatus clearTyping(Long userId) {
        Optional<UserStatus> existing = userStatusRepository.findById(userId);
        if (existing.isPresent()) {
            UserStatus s = existing.get();
            s.setTypingToUserId(null);
            s.setTypingStartedAt(null);
            return userStatusRepository.save(s);
        }
        return null;
    }

    public boolean isTypingTo(Long typerId, Long receiverId) {
        Optional<UserStatus> existing = userStatusRepository.findById(typerId);
        if (existing.isPresent()) {
            UserStatus s = existing.get();
            if (s.getTypingToUserId() != null && s.getTypingToUserId().equals(receiverId)) {
                if (s.getTypingStartedAt() != null && 
                    s.getTypingStartedAt().isAfter(LocalDateTime.now().minusSeconds(5))) {
                    return true;
                }
                s.setTypingToUserId(null);
                s.setTypingStartedAt(null);
                userStatusRepository.save(s);
            }
        }
        return false;
    }
}
