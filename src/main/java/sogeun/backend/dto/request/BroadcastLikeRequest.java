package sogeun.backend.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class BroadcastLikeRequest {
    private Long senderId;
    private Long likerId;
}
