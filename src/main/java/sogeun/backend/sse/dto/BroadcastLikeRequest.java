package sogeun.backend.sse.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class BroadcastLikeRequest {
    private Long senderId;   // 송출자
    private Long likerId;    // 좋아요 누른 유저
    private double lat;      // 송출자 현재 위도
    private double lon;      // 송출자 현재 경도
}
