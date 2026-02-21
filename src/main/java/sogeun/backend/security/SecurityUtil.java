package sogeun.backend.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import sogeun.backend.common.error.AppException;
import sogeun.backend.common.error.ErrorCode;

@Slf4j
public class SecurityUtil {

    private SecurityUtil() {} // 생성자 막기

    public static Long extractUserId(Authentication authentication) {

        if (authentication == null) {
            log.warn("[AUTH] Authentication is null");
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        String name = authentication.getName();
        if (name == null || name.isBlank()) {
            log.warn("[AUTH] authentication.getName() is blank");
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        try {
            return Long.parseLong(name); // JWT sub = userId
        } catch (NumberFormatException e) {
            log.warn("[AUTH] userId parse failed name={}", name);
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
    }
}