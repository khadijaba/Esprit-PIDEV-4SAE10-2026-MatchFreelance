package esprit.tn.blog.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Group Message Entity
 * Represents a message sent in a discussion group
 */
@Entity
@Table(name = "group_messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    @JsonIgnore  // Prevent circular reference
    private DiscussionGroup group;

    @Column(name = "group_id", insertable = false, updatable = false)
    private Long groupId; // For JSON serialization

    @Column(nullable = false)
    private Long senderId; // User ID from User microservice

    private String senderName; // Cached for display

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

    private LocalDateTime editedAt;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isEdited = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    // Reply/Thread support
    private Long replyToMessageId; // ID of message being replied to

    // Reactions support (stored as JSON or separate table)
    @Column(length = 1000)
    private String reactions; // JSON: {"👍": [userId1, userId2], "❤️": [userId3]}

    // AI Summary for long messages
    @Column(length = 2000)
    private String aiSummary; // Generated summary for long posts

    @Column(nullable = false)
    @Builder.Default
    private Boolean hasSummary = false;

    @PrePersist
    protected void onCreate() {
        sentAt = LocalDateTime.now();
    }

    @PostLoad
    @PostPersist
    protected void onLoad() {
        if (group != null) {
            this.groupId = group.getId();
        }
    }

    public enum MessageType {
        TEXT,       // Regular text message
        IMAGE,      // Image attachment
        GIF,        // GIF image
        FILE,       // File attachment
        EMOJI,      // Emoji-only message
        SYSTEM      // System message (user joined, etc.)
    }
}
