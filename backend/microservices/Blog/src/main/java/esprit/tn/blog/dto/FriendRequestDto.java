package esprit.tn.blog.dto;

import esprit.tn.blog.entity.FriendRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FriendRequestDto {
    private Long senderId;
    private Long receiverId;
    private String senderName;
    private String receiverName;
    private String senderAvatar;
    private String message;
    private FriendRequest.RequestStatus status;
}
