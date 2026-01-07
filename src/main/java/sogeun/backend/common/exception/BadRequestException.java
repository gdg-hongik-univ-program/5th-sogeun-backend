package sogeun.backend.common.exception;

/**
 * 400 Bad Request: 요청 값/형식이 잘못된 경우(유효성/비즈니스 규칙 위반)
 */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
