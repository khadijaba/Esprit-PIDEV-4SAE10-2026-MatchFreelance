package esprit.tn.blog.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Forum Post entity for Blog microservice
 * User data fields (author, username, avatar) are populated from User microservice (port 9090)
 * userId field references the User microservice user ID
 */
@Entity
@Table(
    name = "forum_post",
    indexes = {
        @Index(name = "idx_fp_topic",  columnList = "topicId"),
        @Index(name = "idx_fp_user",   columnList = "userId"),
        @Index(name = "idx_fp_parent", columnList = "parentPostId")
    }
)
public class ForumPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long topicId;
    
    // User microservice integration
    private Long userId; // References User.id from User microservice
    
    // Cached user data from User microservice (for performance)
    private String author;     // User.firstName + " " + User.lastName
    private String username;   // User.email or display name
    private String avatar;     // User.profilePictureUrl

    @Column(length = 2000, nullable = false)
    private String content;

    @Column(columnDefinition = "TEXT")
    private String image;

    private Boolean isEdited = false;

    private Long parentPostId;
    private Long sharedPostId;

    private Integer comments = 0;
    private Integer reposts  = 0;
    private Integer likes    = 0;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ForumPost() {}

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ── Getters & Setters ──────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTopicId() { return topicId; }
    public void setTopicId(Long topicId) { this.topicId = topicId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    public Boolean getIsEdited() { return isEdited; }
    public void setIsEdited(Boolean isEdited) { this.isEdited = isEdited; }

    public Long getParentPostId() { return parentPostId; }
    public void setParentPostId(Long parentPostId) { this.parentPostId = parentPostId; }

    public Long getSharedPostId() { return sharedPostId; }
    public void setSharedPostId(Long sharedPostId) { this.sharedPostId = sharedPostId; }

    public Integer getComments() { return comments; }
    public void setComments(Integer comments) { this.comments = comments; }

    public Integer getReposts() { return reposts; }
    public void setReposts(Integer reposts) { this.reposts = reposts; }

    public Integer getLikes() { return likes; }
    public void setLikes(Integer likes) { this.likes = likes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
