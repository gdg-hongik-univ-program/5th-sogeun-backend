package sogeun.backend.dto.request;

import lombok.Getter;

@Getter
public class MusicRecentRequest {
    private MusicInfo music;
    private Long playedAt; //필요하면?
}