package sogeun.backend.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "로그인 응답")
public record LoginResponse(@Schema(
        description = "JWT Access Token",
        example = "eyJhbGciOiJIUzI1NiJ9..."
) String accessToken
) {}

