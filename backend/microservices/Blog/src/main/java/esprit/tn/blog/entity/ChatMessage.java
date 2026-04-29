package esprit.tn.blog.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Chat Message entity for Blog microservice
 * User data fields are populated from User microservice (port 9090)
 */
@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // User microservice integration - Sender
    private Long senderId;              // References User.id from User microservice
    private String senderName;          // User.firstName + " " + User.lastName
    @Column(columnDefinition = "TEXT")
    private String senderAvatar;        // User.profilePictureUrl

    // User microservice integration - Receiver
    private Long receiverId;            // References User.id from User microservice
    private String receiverName;        // User.firstName + " " + User.lastName

    @Column(columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private MessageType messageType = MessageType.TEXT;

    @Column(columnDefinition = "TEXT")
    private String imageUrl;

    @Column(columnDefinition = "TEXT")
    private String gifUrl;

    private Long sharedPostId;

    private Long replyToId;
    @Column(columnDefinition = "TEXT")
    private String replyToContent;
    private String replyToSenderName;

    @Column(columnDefinition = "TEXT")
    private String reactions;

    @Column(columnDefinition = "TEXT")
    private String deletedForUsers;

    @Builder.Default
    private Boolean isRead = false;

    private LocalDateTime readAt;

    @Builder.Default
    private Boolean isForwarded = false;
    private String forwardedFromName;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.messageType == null) this.messageType = MessageType.TEXT;
        if (this.isRead == null) this.isRead = false;
        if (this.isForwarded == null) this.isForwarded = false;
    }
}
