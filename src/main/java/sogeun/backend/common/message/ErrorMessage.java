package sogeun.backend.common.message;

public class ErrorMessage {

    // ===== User 관련 에러 =====
    public static final String USER_NOT_FOUND = "회원을 찾을 수 없습니다";
    public static final String USER_ALREADY_EXISTS = "이미 존재하는 로그인 아이디입니다";

    // ===== 인증 / 인가 =====
    public static final String UNAUTHORIZED = "인증이 필요합니다";
    public static final String LOGIN_INVALID = "아이디 또는 비밀번호가 올바르지 않습니다.";

    // ===== DTO Validation =====
    public static final String LOGIN_ID_NOT_NULL = "로그인 아이디는 필수입니다";
    public static final String LOGIN_ID_SIZE = "아이디는 4자 이상 20자 이하";
}
