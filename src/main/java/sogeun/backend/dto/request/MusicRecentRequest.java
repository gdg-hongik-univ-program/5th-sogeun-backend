package sogeun.backend.dto.request;

import lombok.Getter;
import lombok.Setter;
import sogeun.backend.sse.dto.MusicDto;

@Getter
@Setter
public class MusicRecentRequest {

    private MusicDto music;   // 프론트에서 내려주는 음악 정보

    // 서버에서는 Authentication에서 userId를 꺼내 쓰면 되니까 사실상 불필요 (필요하면 유지)
    private Long userId;

    // 내부 PK를 직접 받는 구조가 아니라면 보통 불필요 (필요하면 유지)
    private Long musicId;

    // ✅ 추가: 마지막 재생 시각 (epoch milli)
    // 프론트가 주면 그걸 쓰고, 안 주면 서비스에서 now로 처리
    private Long playedAt;
}
