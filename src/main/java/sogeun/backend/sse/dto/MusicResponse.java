package sogeun.backend.sse.dto;

public record MusicResponse(
        Long trackId,
        String title,
        String artist,
        String artworkUrl,
        String previewUrl
) {}
