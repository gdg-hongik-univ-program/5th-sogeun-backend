package sogeun.backend.common.exception;

/**
 * 404 Not Found: 요청한 리소스를 찾을 수 없는 경우
 */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
