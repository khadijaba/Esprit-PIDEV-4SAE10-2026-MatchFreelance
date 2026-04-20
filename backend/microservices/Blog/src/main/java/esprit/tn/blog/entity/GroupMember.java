package esprit.tn.blog.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Group Member Entity
 * Represents a user's membership in a discussion group
 */
@Entity
@Table(name = "group_members", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"group_id", "user_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    @JsonIgnore  // Prevent circular reference
    private DiscussionGroup group;

    @Column(name = "group_id", insertable = false, updatable = false)
    private Long groupId; // For JSON serialization

    @Column(name = "user_id", nullable = false)
    private Long userId; // User ID from User microservice

    private String userName; // Cached for display

    private String userAvatar; // Cached for display

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private MemberRole role = MemberRole.MEMBER;

    @Column(nullable = false)
    private LocalDateTime joinedAt;

    private LocalDateTime lastReadAt; // For unread message tracking

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isMuted = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean canPost = true;

    @PrePersist
    protected void onCreate() {
        joinedAt = LocalDateTime.now();
        lastReadAt = LocalDateTime.now();
    }

    @PostLoad
    @PostPersist
    protected void onLoad() {
        if (group != null) {
            this.groupId = group.getId();
        }
    }

    public enum MemberRole {
        ADMIN,      // Can manage group settings and members
        MODERATOR,  // Can moderate messages and members
        MEMBER      // Regular member
    }
}
