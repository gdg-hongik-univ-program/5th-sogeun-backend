package sogeun.backend.sse.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyBroadcastResponse {

    private boolean active;       // 송출중인지 여부
    private Double lat;           // 송출중이면 값, 아니면 null 권장
    private Double lon;

    private MusicDto music;       // 송출중이면 값, 아니면 null 권장
    private long likeCount;       // 누적 좋아요 수 (또는 현재 방송 기준)
    private Integer radiusMeter;  // 현재 반경(m). 방송 꺼져도 마지막 반경을 보여줄지 정책 선택

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MusicDto {
        private Long trackId;
        private String title;
        private String artist;

        private String artworkUrl;
        private String previewUrl;
    }
}
