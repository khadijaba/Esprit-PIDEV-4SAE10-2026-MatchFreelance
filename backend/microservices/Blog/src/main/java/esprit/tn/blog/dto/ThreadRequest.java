package esprit.tn.blog.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ThreadRequest {
    private String title;
    private String content;
    private String author;
    /** Optional; when null, service uses default user id (must exist in your users table). */
    private Long authorId;
    private String authorRole;
    private String authorAvatar;
    private String category;
    private List<String> tags;
}
