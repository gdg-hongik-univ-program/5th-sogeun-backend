package sogeun.backend.sse.dto;

public record UserNearbyResponse(
        Long userId,
        String nickname,
        boolean isBroadcasting,
        MusicDto music,
        Integer radiusMeter,
        Integer likeCount
) {}
