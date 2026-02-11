package sogeun.backend.security;

import org.springframework.security.core.Authentication;
import sogeun.backend.common.exception.UnauthorizedException;
import sogeun.backend.common.message.ErrorMessage;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SecurityUtil {

    private SecurityUtil() {} // 생성자 막기

    public static Long extractUserId(Authentication authentication) {
        if (authentication == null) {
            log.warn("[인증] Authentication 객체가 null 입니다");
            throw new UnauthorizedException(ErrorMessage.UNAUTHORIZED);
        }

        String name = authentication.getName();
        if (name == null || name.isBlank()) {
            log.warn("[인증] authentication.getName() 이 비어있습니다");
            throw new UnauthorizedException(ErrorMessage.UNAUTHORIZED);
        }

        try {
            return Long.parseLong(name); // JWT sub = userId
        } catch (NumberFormatException e) {
            log.warn("[인증] userId 파싱 실패 - name={}", name);
            throw new UnauthorizedException(ErrorMessage.UNAUTHORIZED);
        }
    }
}
