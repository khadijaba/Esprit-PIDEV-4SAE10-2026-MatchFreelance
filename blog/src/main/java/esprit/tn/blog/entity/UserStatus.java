package esprit.tn.blog.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class UserStatus {

    @Id
    private Long userId;

    private String userName;
    @Column(columnDefinition = "TEXT")
    private String userAvatar;

    @Builder.Default
    private Boolean isOnline = false;

    private LocalDateTime lastSeen;

    private Long typingToUserId;
    private LocalDateTime typingStartedAt;

    @PrePersist
    protected void onCreate() { this.lastSeen = LocalDateTime.now(); }
}
