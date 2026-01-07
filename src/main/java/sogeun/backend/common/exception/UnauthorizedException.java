package sogeun.backend.common.exception;

/**
 * 401 Unauthorized: 인증 실패(로그인 실패, 토큰 없음/만료 등)
 */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
