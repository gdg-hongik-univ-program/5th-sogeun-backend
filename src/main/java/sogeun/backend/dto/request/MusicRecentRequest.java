package sogeun.backend.dto.request;

import lombok.Getter;
import lombok.Setter;
import sogeun.backend.sse.dto.MusicDto;

@Getter
@Setter
public class MusicRecentRequest {

    private MusicDto music;
    private Long userId;
    private Long musicId;
    private Long playedAt;
}
