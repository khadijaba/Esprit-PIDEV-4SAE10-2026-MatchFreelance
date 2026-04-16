package esprit.tn.blog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiscussionGroupRequest {
    private String name;
    private String description;
    private String topic;
    private String logoUrl;
    private Long creatorId;
    private String creatorName;
    private Boolean isPrivate;
    private Boolean allowMemberInvites;
    private Boolean allowFileSharing;
    private Boolean allowGifs;
    private Boolean allowEmojis;
}
