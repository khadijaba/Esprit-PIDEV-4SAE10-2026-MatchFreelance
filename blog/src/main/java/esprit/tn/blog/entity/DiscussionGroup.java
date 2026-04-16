package esprit.tn.blog.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Discussion Group Entity
 * Represents a group chat/discussion room that users can create and join
 */
@Entity
@Table(name = "discussion_groups")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiscussionGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private String topic; // e.g., "Technology", "Freelancing", "Design", etc.

    @Column(columnDefinition = "LONGTEXT")
    private String logoUrl; // Base64-encoded image or URL to group logo/avatar

    @Column(nullable = false)
    private Long creatorId; // User ID from User microservice

    private String creatorName; // Cached for display

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isPrivate = false; // Private groups require invitation

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Builder.Default
    private Integer memberCount = 0;

    @Builder.Default
    private Integer messageCount = 0;

    private LocalDateTime lastActivityAt;

    // Group settings
    @Column(nullable = false)
    @Builder.Default
    private Boolean allowMemberInvites = true; // Can members invite others?

    @Column(nullable = false)
    @Builder.Default
    private Boolean allowFileSharing = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean allowGifs = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean allowEmojis = true;

    // Relationships
    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<GroupMember> members = new ArrayList<>();

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<GroupMessage> messages = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        lastActivityAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
