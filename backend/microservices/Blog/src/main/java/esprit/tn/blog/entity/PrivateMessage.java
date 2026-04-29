package esprit.tn.blog.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Private Message Entity
 * Represents a direct message between two users
 */
@Entity
@Table(name = "private_messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrivateMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long senderId; // User ID from User microservice

    @Column(nullable = false)
    private Long receiverId; // User ID from User microservice

    private String senderName; // Cached for display

    private String receiverName; // Cached for display

    private String senderAvatar; // Cached for display

    @Column(length = 5000, nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private MessageType type = MessageType.TEXT;

    private String mediaUrl; // For images, GIFs, files

    private String gifUrl; // Specific field for GIF URLs

    @Column(nullable = false)
    private LocalDateTime sentAt;

    private LocalDateTime readAt;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isDeletedBySender = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isDeletedByReceiver = false;

    // Reply support
    private Long replyToMessageId;

    // Reactions
    private String reaction; // Single emoji reaction

    @PrePersist
    protected void onCreate() {
        sentAt = LocalDateTime.now();
    }

    public enum MessageType {
        TEXT,
        IMAGE,
        GIF,
        FILE,
        EMOJI
    }
}
