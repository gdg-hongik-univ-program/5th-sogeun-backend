package sogeun.backend.dto.response;

public record UserNearbyResponse(
        Long userId,
        String nickname,
        String songTitle,
        String artist
) {}

