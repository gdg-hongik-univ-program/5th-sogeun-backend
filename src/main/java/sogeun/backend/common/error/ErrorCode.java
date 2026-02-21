package sogeun.backend.common.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    // ===== AUTH =====
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH_401", "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "AUTH_403", "접근 권한이 없습니다."),
    LOGIN_INVALID(HttpStatus.BAD_REQUEST, "AUTH_400", "아이디 또는 비밀번호가 올바르지 않습니다."),

    // ===== USER =====
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_404", "회원을 찾을 수 없습니다."),
    USER_ALREADY_EXISTS(HttpStatus.CONFLICT, "USER_409", "이미 존재하는 로그인 아이디입니다."),

    // ===== BROADCAST / LOCATION =====
    MUSIC_TRACK_ID_REQUIRED(HttpStatus.BAD_REQUEST, "MUSIC_400", "music.trackId는 필수입니다."),
    LOCATION_NOT_FOUND(HttpStatus.NOT_FOUND, "LOC_404", "위치 정보가 없습니다."),
    BROADCAST_NOT_FOUND(HttpStatus.NOT_FOUND, "BROADCAST_404", "방송 정보를 찾을 수 없습니다."),
    BROADCAST_NOT_ACTIVE(HttpStatus.CONFLICT, "BROADCAST_409", "방송 중이 아닙니다."),

    // ===== COMMON =====
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "COMMON_400", "요청 형식이 올바르지 않습니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_500", "서버 내부 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    public HttpStatus getStatus() { return status; }
    public String getCode() { return code; }
    public String getMessage() { return message; }
}