package sogeun.backend.dto.request;

import lombok.Getter;

@Getter
public class FavoriteSongUpdateRequest {
    private String spotifyTrackId;   // 강력 권장 (unique)
    private String title;
    private String artistName;
}
