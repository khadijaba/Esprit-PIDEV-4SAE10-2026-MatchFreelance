package esprit.tn.blog.dto;

import esprit.tn.blog.entity.PrivateMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrivateMessageRequest {
    private Long senderId;
    private Long receiverId;
    private String senderName;
    private String receiverName;
    private String senderAvatar;
    private String content;
    private PrivateMessage.MessageType type;
    private String mediaUrl;
    private String gifUrl;
    private Long replyToMessageId;
}
