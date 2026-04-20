package esprit.tn.blog.dto;

import esprit.tn.blog.entity.GroupMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupMessageRequest {
    private Long groupId;
    private Long senderId;
    private String senderName;
    private String senderAvatar;
    private String content;
    private GroupMessage.MessageType type;
    private String mediaUrl;
    private String gifUrl;
    private Long replyToMessageId;
}
