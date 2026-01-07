package sogeun.backend.common.exception;

/**
 * 409 Conflict: 중복 등 리소스 상태 충돌이 발생한 경우
 */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
