package sogeun.backend.sse.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MusicDto {

    private Long trackId;
    private String title;
    private String artist;
    private String artworkUrl;
    private String previewUrl;
}
