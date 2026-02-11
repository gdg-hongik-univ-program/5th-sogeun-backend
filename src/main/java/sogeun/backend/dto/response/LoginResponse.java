package sogeun.backend.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "로그인 응답")
public record LoginResponse(
        @Schema(description = "JWT Access Token")
        String accessToken,

        @Schema(description = "JWT Refresh Token (HttpOnly Cookie로만 사용)", hidden = true)
        String refreshToken
) {}
