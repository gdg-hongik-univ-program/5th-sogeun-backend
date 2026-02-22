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
import sogeun.backend.common.error.AppException;
import sogeun.backend.common.error.ErrorCode;
import sogeun.backend.dto.request.LoginRequest;
import sogeun.backend.dto.request.UpdateNicknameRequest;
import sogeun.backend.dto.request.UserCreateRequest;
import sogeun.backend.dto.response.LoginResponse;
import sogeun.backend.dto.response.MeResponse;
import sogeun.backend.dto.response.UserCreateResponse;
import sogeun.backend.entity.User;
import sogeun.backend.security.JwtProvider;
import sogeun.backend.security.RefreshTokenRepository;
import sogeun.backend.security.SecurityUtil;
import sogeun.backend.service.MusicService;
import sogeun.backend.service.UserService;

import java.net.URI;
import java.time.Duration;
import java.util.List;

@Slf4j
@Tag(name = "User", description = "회원가입/로그인/내정보/테스트 API")
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
        log.info("[SIGNUP] loginId={}", request.getLoginId());

        User savedUser = userService.createUser(request);

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
        log.info("[LOGIN] loginId={}", request.getLoginId());

        LoginResponse result = userService.login(request);

        String refreshToken = result.refreshToken();
        if (refreshToken != null && !refreshToken.isBlank()) {
            ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                    .httpOnly(true)
                    .secure(true)                 // ✅ HTTPS
                    .sameSite("None")             // ✅ cross-site(서브도메인 포함) 안정
                    .domain(".sogeun.cloud")      // ✅ 서브도메인 공유 핵심
                    .path("/")
                    .maxAge(Duration.ofDays(14))
                    .build();

            servletResponse.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        }

        return ResponseEntity.ok(new LoginResponse(result.accessToken(), null));
    }

    @Operation(summary = "Access 토큰 재발급", description = "HttpOnly 쿠키의 refreshToken으로 accessToken 재발급")
    @PostMapping("/auth/refresh")
    @SecurityRequirements
    public ResponseEntity<LoginResponse> refresh(HttpServletRequest request) {

        String refreshToken = extractCookie(request, "refreshToken");
        if (refreshToken == null || refreshToken.isBlank()) {
            log.warn("[REFRESH] refreshToken cookie missing");
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        if (!jwtProvider.validate(refreshToken)) {
            log.warn("[REFRESH] invalid refreshToken");
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        String typ = jwtProvider.parseTokenType(refreshToken);
        if (!"refresh".equals(typ)) {
            log.warn("[REFRESH] token typ is not refresh typ={}", typ);
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        Long userId = jwtProvider.parseUserId(refreshToken);

        String saved = refreshTokenRepository.get(userId);
        if (saved == null || !saved.equals(refreshToken)) {
            log.warn("[REFRESH] refreshToken mismatch userId={}", userId);
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        String newAccessToken = jwtProvider.createAccessToken(userId);
        log.info("[REFRESH] accessToken issued userId={}", userId);

        return ResponseEntity.ok(new LoginResponse(newAccessToken, null));
    }

    // 로그아웃
    @Operation(summary = "로그아웃", description = "refreshToken 무효화 및 쿠키 삭제")
    @PostMapping("/auth/logout")
    @SecurityRequirements
    public ResponseEntity<Void> logout(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String refreshToken = extractCookie(request, "refreshToken");
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.noContent().build();
        }

        if (jwtProvider.validate(refreshToken)
                && "refresh".equals(jwtProvider.parseTokenType(refreshToken))) {

            Long userId = jwtProvider.parseUserId(refreshToken);
            refreshTokenRepository.delete(userId);
            log.info("[LOGOUT] refreshToken deleted userId={}", userId);

        } else {
            log.warn("[LOGOUT] invalid refreshToken");
        }

        ResponseCookie deleteCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, deleteCookie.toString());

        return ResponseEntity.noContent().build();
    }


    // 내 정보 반환
    @Operation(summary = "내 정보 조회", description = "accessToken이 유효하면 내 정보 반환")
    @GetMapping("/me/information")
    public MeResponse me(Authentication authentication) {
        Long userId = SecurityUtil.extractUserId(authentication);
        return userService.getMe(userId);
    }

    // 닉네임 변경
    @Operation(summary = "닉네임 변경", description = "accessToken이 유효하면 닉네임 변경")
    @PatchMapping("/me/nickname")
    public MeResponse updateNickname(Authentication authentication,
                                     @RequestBody @Valid UpdateNicknameRequest request) {
        Long userId = SecurityUtil.extractUserId(authentication);
        return userService.updateNicknameByUserId(userId, request.getNickname());
    }

    // ===================== 테스트 전용 =====================

    @Hidden
    @DeleteMapping("/test/users/reset")
    public ResponseEntity<Void> resetUsers() {
        log.warn("[TEST] truncate users");
        userService.resetUsersForTest();
        return ResponseEntity.noContent().build();
    }
    // 테스트용 전체 유저 목록 조회
    @Operation(summary = "전체 유저 조회", description = "전체 유저 리스트를 반환")
    @GetMapping("/users")
    public ResponseEntity<List<MeResponse>> getAllUsers() {
        List<MeResponse> users = userService.getAllUsers();
        log.info("[USERS] count={}", users.size());
        return ResponseEntity.ok(users);
    }

    // 쿠키에서 값 추출
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
}