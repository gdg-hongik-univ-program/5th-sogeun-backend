package sogeun.backend.service;

import lombok.RequiredArgsConstructor;
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

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;

    @Transactional
    public Long createUser(UserCreateRequest request) {
        if (userRepository.existsByLoginId(request.getLoginId())) {
            throw new ConflictException(ErrorMessage.USER_ALREADY_EXISTS);
        }
        User user = new User(request.getLoginId(), request.getPassword(), request.getNickname());
        return userRepository.save(user).getUserId();
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByLoginId(request.getLoginId())
                .orElseThrow(() -> new UnauthorizedException(ErrorMessage.LOGIN_INVALID));

        if (!user.getPassword().equals(request.getPassword())) {
            throw new UnauthorizedException(ErrorMessage.LOGIN_INVALID);
        }

        String token = jwtProvider.createAccessToken(user.getUserId());
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
