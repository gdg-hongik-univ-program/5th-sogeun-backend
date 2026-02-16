package sogeun.backend.controller;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import sogeun.backend.common.exception.UnauthorizedException;
import sogeun.backend.common.message.ErrorMessage;
import sogeun.backend.dto.request.FavoriteSongUpdateRequest;
import sogeun.backend.dto.request.LoginRequest;
import sogeun.backend.dto.request.UpdateNicknameRequest;
import sogeun.backend.dto.request.UserCreateRequest;
import sogeun.backend.dto.response.LoginResponse;
import sogeun.backend.dto.response.MeResponse;
import sogeun.backend.dto.response.UserCreateResponse;
import sogeun.backend.dto.response.UserLikeSongResponse;
import sogeun.backend.entity.User;
import sogeun.backend.security.JwtProvider;
import sogeun.backend.security.RefreshTokenRepository;
import sogeun.backend.service.MusicService;
import sogeun.backend.service.UserService;

import java.net.URI;
import java.time.Duration;
import java.util.List;

@Slf4j
@Tag(name = "User", description = "회원가입/로그인/내정보/테스트 API")
@CrossOrigin("*")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class UserController {

    private final UserService userService;
    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final MusicService musicService;

    // 회원가입
    @Operation(summary = "회원가입", description = "loginId/password/nickname을 받아 회원 생성")
    @PostMapping("/auth/signup")
    @SecurityRequirements
    public ResponseEntity<UserCreateResponse> createUser(
            @RequestBody @Valid UserCreateRequest request
    ) {

        log.info("[회원가입] 요청 수신 - loginId={}", request.getLoginId());

        User savedUser = userService.createUser(request);

        log.info("[회원가입] 처리 완료 - userId={}", savedUser.getUserId());

        return ResponseEntity
                .created(URI.create("/api/users/" + savedUser.getUserId()))
                .body(UserCreateResponse.from(savedUser));
    }

    // 로그인
    @Operation(summary = "로그인", description = "loginId/password로 로그인 후 accessToken 발급 + refreshToken은 HttpOnly 쿠키로 발급")
    @PostMapping("/auth/login")
    @SecurityRequirements
    public ResponseEntity<LoginResponse> login(
            @RequestBody @Valid LoginRequest request,
            HttpServletResponse servletResponse
    ) {
        log.info("[로그인] 요청 수신 - loginId={}", request.getLoginId());

        LoginResponse result = userService.login(request);

        // 1) refreshToken을 HttpOnly 쿠키로 세팅
        String refreshToken = result.refreshToken();
        if (refreshToken != null && !refreshToken.isBlank()) {
            ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                    .httpOnly(true)
                    .secure(false)
                    .sameSite("Lax")
                    .path("/")
                    .maxAge(Duration.ofDays(14))
                    .build();

            servletResponse.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        }

        log.info("[로그인] 처리 완료 - accessToken 발급 + refreshToken 쿠키 세팅");

        // 2) 바디에는 accessToken만 내려줌
        return ResponseEntity.ok(new LoginResponse(result.accessToken(), null));
    }

    @Operation(summary = "Access 토큰 재발급", description = "HttpOnly 쿠키의 refreshToken으로 accessToken 재발급")
    @PostMapping("/auth/refresh")
    @SecurityRequirements
    public ResponseEntity<LoginResponse> refresh(HttpServletRequest request) {

        // 1) 쿠키에서 refreshToken 추출
        String refreshToken = extractCookie(request, "refreshToken");
        if (refreshToken == null || refreshToken.isBlank()) {
            log.warn("[REFRESH] refreshToken cookie missing");
            throw new UnauthorizedException(ErrorMessage.UNAUTHORIZED);
        }

        // 2) refreshToken 검증
        boolean valid = jwtProvider.validate(refreshToken);
        if (!valid) {
            log.warn("[REFRESH] invalid refreshToken");
            throw new UnauthorizedException(ErrorMessage.UNAUTHORIZED);
        }

        // 3) typ이 refresh인지 확인
        String typ = jwtProvider.parseTokenType(refreshToken);
        if (!"refresh".equals(typ)) {
            log.warn("[REFRESH] token typ is not refresh. typ={}", typ);
            throw new UnauthorizedException(ErrorMessage.UNAUTHORIZED);
        }

        // 4) refreshToken에서 userId 추출
        Long userId = jwtProvider.parseUserId(refreshToken);

        // 5) Redis 저장된 refreshToken과 일치하는지 확인
        String saved = refreshTokenRepository.get(userId);
        if (saved == null || !saved.equals(refreshToken)) {
            log.warn("[REFRESH] refreshToken mismatch or not found. userId={}", userId);
            throw new UnauthorizedException(ErrorMessage.UNAUTHORIZED);
        }

        // 6) 새 accessToken 발급
        String newAccessToken = jwtProvider.createAccessToken(userId);
        log.info("[REFRESH] new accessToken issued. userId={}", userId);

        // 7) 응답 바디는 accessToken만 (refresh는 쿠키로만 유지)
        return ResponseEntity.ok(new LoginResponse(newAccessToken, null));
    }

    //쿠키에서 특정 값 추출
    private String extractCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;

        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    //로그아웃
    @Operation(summary = "로그아웃", description = "refreshToken 무효화 및 쿠키 삭제")
    @PostMapping("/auth/logout")
    @SecurityRequirements
    public ResponseEntity<Void> logout(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        // 1) 쿠키에서 refreshToken 추출
        String refreshToken = extractCookie(request, "refreshToken");
        if (refreshToken == null || refreshToken.isBlank()) {
            log.info("[LOGOUT] refreshToken cookie not found (already logged out)");
            return ResponseEntity.noContent().build();
        }

        // 2) refreshToken 유효하면 userId 파싱
        if (jwtProvider.validate(refreshToken)
                && "refresh".equals(jwtProvider.parseTokenType(refreshToken))) {

            Long userId = jwtProvider.parseUserId(refreshToken);

            // 3) Redis에서 refreshToken 삭제
            refreshTokenRepository.delete(userId);
            log.info("[LOGOUT] refreshToken deleted. userId={}", userId);
        } else {
            log.warn("[LOGOUT] invalid refreshToken");
        }

        // 4) 브라우저 쿠키 삭제 (만료)
        ResponseCookie deleteCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, deleteCookie.toString());

        log.info("[LOGOUT] success");
        return ResponseEntity.noContent().build();
    }


    // 내 정보 반환
    @Operation(summary = "내 정보 조회", description = "accessToken이 유효하면 내 정보 반환")
    @GetMapping("/me/information")
    public MeResponse me(Authentication authentication) {
        Long userId = extractUserId(authentication);
        log.debug("[내정보] 조회 요청 - userId={}", userId);
        return userService.getMe(userId);
    }

    // 전체 유저 목록 조회
    @Operation(summary = "전체 유저 조회", description = "전체 유저 리스트를 반환")
    @GetMapping("/users")
    public ResponseEntity<List<MeResponse>> getAllUsers() {
        log.info("[유저전체조회] 요청 수신");

        List<MeResponse> users = userService.getAllUsers();

        log.info("[유저전체조회] 조회 완료 - count={}", users.size());

        return ResponseEntity.ok(users);
    }



    // 닉네임 변경
    @Operation(summary = "닉네임 변경", description = "accessToken이 유효하면 닉네임 변경")
    @PatchMapping("/me/nickname")
    public MeResponse updateNickname(Authentication authentication,
                                     @RequestBody @Valid UpdateNicknameRequest request) {
        Long userId = extractUserId(authentication);
        log.debug("[닉네임변경] 요청 수신 - userId={}", userId);
        return userService.updateNicknameByUserId(userId, request.getNickname());
    }

    // ===================== 테스트 전용 =====================

    @Hidden
    @DeleteMapping("/test/users/reset")
    public ResponseEntity<Void> resetUsers() {
        log.warn("[테스트] users 테이블 초기화(TRUNCATE) 요청");
        userService.resetUsersForTest();
        log.warn("[테스트] users 테이블 초기화 완료");
        return ResponseEntity.noContent().build();
    }

    // ===== 인증에서 userId 추출 =====
    private Long extractUserId(Authentication authentication) {
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

//    // 좋아하는 노래 설정
//    @PatchMapping("/users/me/favorite-song")
//    public MeResponse updateFavoriteSong(
//            Authentication authentication,
//            @RequestBody @Valid FavoriteSongUpdateRequest request
//    ) {
//        Long userId = extractUserId(authentication);
//        log.debug("[좋아하는노래변경] 요청 수신 - userId={}", userId);
//
//        return userService.updateFavoriteSong(userId, request);
//    }

}
