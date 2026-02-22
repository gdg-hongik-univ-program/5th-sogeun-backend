package sogeun.backend.dto.response;

public record MusicResponse(
        Long trackId,
        String title,
        String artist,
        String artworkUrl,
        String previewUrl
) {}
