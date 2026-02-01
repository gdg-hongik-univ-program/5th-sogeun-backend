package sogeun.backend.dto.request;

public record LocationRequest(
        Long userId,
        double lat,
        double lon
) {}

