package sogeun.backend.dto.request;

import lombok.Getter;
import sogeun.backend.sse.dto.MusicDto;

@Getter
public class MusicLikeRequest {

    private MusicDto music;
    private Long userId;
    private Long musicId;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

}