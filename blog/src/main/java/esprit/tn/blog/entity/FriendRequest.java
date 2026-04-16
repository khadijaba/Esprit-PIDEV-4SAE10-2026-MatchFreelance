package esprit.tn.blog.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Friend Request Entity
 * Represents a friend request between two users
 */
@Entity
@Table(name = "friend_requests",
       uniqueConstraints = @UniqueConstraint(columnNames = {"sender_id", "receiver_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FriendRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sender_id", nullable = false)
    private Long senderId; // User ID from User microservice

    @Column(name = "receiver_id", nullable = false)
    private Long receiverId; // User ID from User microservice

    private String senderName; // Cached for display

    private String receiverName; // Cached for display

    private String senderAvatar; // Cached for display

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private RequestStatus status = RequestStatus.PENDING;

    @Column(nullable = false)
    private LocalDateTime sentAt;

    private LocalDateTime respondedAt;

    @Column(length = 500)
    private String message; // Optional message with friend request

    @PrePersist
    protected void onCreate() {
        sentAt = LocalDateTime.now();
    }

    public enum RequestStatus {
        PENDING,
        ACCEPTED,
        REJECTED,
        CANCELLED
    }
}
