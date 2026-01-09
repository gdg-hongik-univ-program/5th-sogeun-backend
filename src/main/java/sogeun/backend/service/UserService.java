package sogeun.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sogeun.backend.common.exception.ConflictException;
import sogeun.backend.common.exception.NotFoundException;
import sogeun.backend.common.exception.UnauthorizedException;
import sogeun.backend.common.message.ErrorMessage;
import sogeun.backend.dto.request.LoginRequest;
import sogeun.backend.dto.response.LoginResponse;
import sogeun.backend.dto.response.MeResponse;
import sogeun.backend.dto.request.UserCreateRequest;
import sogeun.backend.entity.User;
import sogeun.backend.repository.UserRepository;
import sogeun.backend.security.JwtProvider;


@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User createUser(UserCreateRequest request) {
        if (userRepository.existsByLoginId(request.getLoginId())) {
            throw new ConflictException(ErrorMessage.USER_ALREADY_EXISTS);
        }

        User user = new User(
                request.getLoginId(),
                passwordEncoder.encode(request.getPassword()),
                request.getNickname()
        );

        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        // 요청 수신
        log.info("[LOGIN] start loginId={}", request.getLoginId());

        // 사용자 조회
        User user = userRepository.findByLoginId(request.getLoginId())
                .orElseThrow(() -> {
                    log.warn("[LOGIN] user not found. loginId={}", request.getLoginId());
                    return new UnauthorizedException(ErrorMessage.LOGIN_INVALID);
                });

        log.info("[LOGIN] user found. userId={}, loginId={}, nickname={}",
                user.getUserId(), user.getLoginId(), user.getNickname());

        //  비밀번호 검증
        String rawPw = request.getPassword();
        String encodedPw = user.getPassword();

        log.debug("[LOGIN] password check begin. rawLen={}, encodedLen={}",
                rawPw == null ? -1 : rawPw.length(),
                encodedPw == null ? -1 : encodedPw.length());

        boolean matches = passwordEncoder.matches(rawPw, encodedPw);

        log.info("[LOGIN] password match result={}", matches);

        if (!matches) {
            log.warn("[LOGIN] invalid password. userId={}, loginId={}",
                    user.getUserId(), user.getLoginId());
            throw new UnauthorizedException(ErrorMessage.LOGIN_INVALID);
        }

        // 토큰 생성
        log.debug("[LOGIN] creating access token. userId={}", user.getUserId());
        String token = jwtProvider.createAccessToken(user.getUserId());

        log.info("[LOGIN] token issued. userId={}, tokenPrefix={}, tokenLen={}",
                user.getUserId(),
                token == null ? "null" : token.substring(0, Math.min(10, token.length())),
                token == null ? -1 : token.length());

        // (5) 응답 반환
        log.info("[LOGIN] success. userId={}", user.getUserId());
        return new LoginResponse(token);
    }

    @Transactional
    public void deleteAllUsers() {
        userRepository.deleteAll();
    }

    @Transactional
    public void resetUsersForTest() {
        userRepository.truncateUsers();
    }

    @Transactional(readOnly = true)
    public MeResponse getMe(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorMessage.USER_NOT_FOUND));
        return new MeResponse(user.getUserId(), user.getLoginId(), user.getNickname());
    }

    @Transactional
    public MeResponse updateNickname(String loginId, String nickname) {

        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new NotFoundException(ErrorMessage.USER_NOT_FOUND));

        user.UpdateNickname(nickname.trim());

        return new MeResponse(user.getUserId(), user.getLoginId(), user.getNickname());
    }

    @Transactional
    public MeResponse updateNicknameByUserId(Long userId, String nickname) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorMessage.USER_NOT_FOUND));

        user.UpdateNickname(nickname);

        return new MeResponse(user.getUserId(), user.getLoginId(), user.getNickname());
    }


}
