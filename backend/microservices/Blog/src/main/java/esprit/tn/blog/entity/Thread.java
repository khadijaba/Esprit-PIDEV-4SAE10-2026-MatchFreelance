package esprit.tn.blog.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "forum_threads")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Thread {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    // Mapping both author and author_name for maximum compatibility with existing schema
    private String author;

    /** Required by DB schema (forum_threads.author_id); set from request or default in service. */
    @Column(name = "author_id")
    private Long authorId;

    @Column(name = "author_name")
    private String authorName;

    @Column(name = "author_role")
    private String authorRole;

    @Column(name = "author_avatar")
    private String authorAvatar;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "post_count")
    @Builder.Default
    private int postCount = 0;

    @Builder.Default
    private int likes = 0;

    @Builder.Default
    private int retweets = 0;

    // Added columns seen in screenshot to avoid DDL errors
    @Column(name = "is_locked")
    @Builder.Default
    private boolean isLocked = false;

    @Column(name = "is_pinned")
    @Builder.Default
    private boolean isPinned = false;

    @Column(name = "last_post_at")
    private LocalDateTime lastPostAt;

    @ElementCollection
    @CollectionTable(name = "forum_thread_tags", joinColumns = @JoinColumn(name = "thread_id"))
    @Column(name = "tag")
    @Builder.Default
    private List<String> tags = new ArrayList<>();

    @Column(name = "category_id", insertable = false, updatable = false)
    private String category;

    @OneToMany(mappedBy = "thread", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @JsonIgnore
    private List<Comment> comments = new ArrayList<>();
}
