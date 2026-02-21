package sogeun.backend.sse.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BroadcastLikeEventDto {
    private Long senderId;
    private Long broadcastId;
    private int likeCount;
    private int radiusMeter;

    public static BroadcastLikeEventDto of(Long senderId, Long broadcastId, int likeCount, int radiusMeter) {
        return new BroadcastLikeEventDto(senderId, broadcastId, likeCount, radiusMeter);
    }
}