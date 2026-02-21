package sogeun.backend.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class SogeunLibraryResponse {

    private int totalTracks;
    private int totalLikes;
    private List<TrackStat> tracks;

    @Getter
    @Builder
    public static class TrackStat {
        private Long trackId;
        private String title;
        private String artist;
        private String artworkUrl;
        private int likeCount;
    }
}