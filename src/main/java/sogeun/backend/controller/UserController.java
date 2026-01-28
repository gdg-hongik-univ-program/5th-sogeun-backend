package sogeun.backend.controller;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import sogeun.backend.entity.User;
import sogeun.backend.service.UserService;

import java.net.URI;
import java.util.List;

@Slf4j
@Tag(name = "User", description = "회원가입/로그인/내정보/테스트 API")
@CrossOrigin("*")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class UserController {

    private final UserService userService;

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
    @Operation(summary = "로그인", description = "loginId/password로 로그인 후 accessToken 발급")
    @PostMapping("/auth/login")
    @SecurityRequirements
    public ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginRequest request) {

        log.info("[로그인] 요청 수신 - loginId={}", request.getLoginId());

        LoginResponse response = userService.login(request);

        log.info("[로그인] 처리 완료 - accessToken 발급");

        return ResponseEntity.ok(response);
    }

    // 내 정보 반환
    @Operation(summary = "내 정보 조회", description = "accessToken이 유효하면 내 정보 반환")
    @GetMapping("/users/me")
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
    @PatchMapping("/users/me/nickname")
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

    // ===== 공통: 인증에서 userId 추출 =====
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

    // 좋아하는 노래 설정
    @PatchMapping("/users/me/favorite-song")
    public MeResponse updateFavoriteSong(
            Authentication authentication,
            @RequestBody @Valid FavoriteSongUpdateRequest request
    ) {
        Long userId = extractUserId(authentication);
        log.debug("[좋아하는노래변경] 요청 수신 - userId={}", userId);

        return userService.updateFavoriteSong(userId, request);
    }

}
