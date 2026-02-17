package sogeun.backend.sse.dto;

public record UserNearbyResponse(
        Long userId,            // 내부 식별자 (프론트에서 안 써도 됨)
        String nickname,
        boolean isBroadcasting,
        Long broadcastId,       // ✅ 추가: 좋아요 타겟 식별자
        MusicDto music,
        Integer radiusMeter,
        Integer likeCount
) {}
