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
import sogeun.backend.dto.request.UserCreateRequest;
import sogeun.backend.dto.response.LoginResponse;
import sogeun.backend.dto.response.MeResponse;
import sogeun.backend.dto.response.UserLikeSongResponse;
import sogeun.backend.entity.User;
//import sogeun.backend.repository.ArtistRepository;
import sogeun.backend.repository.MusicLikeRepository;   // ✅ 추가
import sogeun.backend.repository.UserRepository;
import sogeun.backend.security.JwtProvider;
import sogeun.backend.security.RefreshTokenRepository;

import java.time.Duration;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;
//    private final ArtistRepository artistRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    private final MusicLikeRepository musicLikeRepository;

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
        log.info("[LOGIN] start loginId={}", request.getLoginId());

        User user = userRepository.findByLoginId(request.getLoginId())
                .orElseThrow(() -> {
                    log.warn("[LOGIN] user not found. loginId={}", request.getLoginId());
                    return new UnauthorizedException(ErrorMessage.LOGIN_INVALID);
                });

        boolean matches = passwordEncoder.matches(request.getPassword(), user.getPassword());
        if (!matches) {
            throw new UnauthorizedException(ErrorMessage.LOGIN_INVALID);
        }

        Long userId = user.getUserId();

        String accessToken = jwtProvider.createAccessToken(userId);
        String refreshToken = jwtProvider.createRefreshToken(userId);

        Duration refreshTtl = Duration.ofDays(14);
        refreshTokenRepository.save(userId, refreshToken, refreshTtl);

        return new LoginResponse(accessToken, refreshToken);
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

        return new MeResponse(
                user.getUserId(),
                user.getLoginId(),
                user.getNickname()
        );
    }

//    @Transactional
//    public Void updateNickname(String loginId, String nickname) {
//        User user = userRepository.findByLoginId(loginId)
//                .orElseThrow(() -> new NotFoundException(ErrorMessage.USER_NOT_FOUND));
//
//        user.updateNickname(nickname.trim());
//        return null;
//    }

    @Transactional
    public MeResponse updateNicknameByUserId(Long userId, String nickname) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorMessage.USER_NOT_FOUND));

        user.updateNickname(nickname);

        return new MeResponse(
                user.getUserId(),
                user.getLoginId(),
                user.getNickname()
        );
    }

    @Transactional(readOnly = true)
    public List<MeResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(user -> new MeResponse(
                        user.getUserId(),
                        user.getLoginId(),
                        user.getNickname()
                ))
                .toList();
    }


//    @Transactional(readOnly = true)
//    public List<UserLikeSongResponse> getLikedSongs(Long userId) {
//        return musicLikeRepository.findLikedMusics(userId).stream()
//                .map(UserLikeSongResponse::new)
//                .toList();
//    }


//    @Transactional
//    public MeResponse updateFavoriteSong(
//            Long userId,
//            FavoriteSongUpdateRequest request
//    ) {
//        User user = userRepository.findById(userId)
//                .orElseThrow(() -> new IllegalArgumentException("USER NOT FOUND"));
//
//        Artist artist = artistRepository.findByName(request.getArtistName())
//                .orElseGet(() ->
//                        artistRepository.save(
//                                new Artist(request.getArtistName())
//                        )
//                );
//
//        Song song = songRepository.save(
//                new Song(request.getTitle(), artist)
//        );
//
//        user.updateFavoriteSong(song);
//
//        return new MeResponse(
//                user.getUserId(),
//                user.getLoginId(),
//                user.getNickname(),
//                song.getTitle(),
//                artist.getName()
//        );
//    }

//    public List<MusicDto> findMusicByUserIds(List<Long> userIds) {
//        return userRepository.findByUserIdIn(userIds).stream()
//                .filter(user -> user.getFavoriteSong() != null)
//                .map(user -> {
//                    Song song = user.getFavoriteSong();
//                    return new MusicDto(
//                            song.getTitle(),
//                            song.getArtist().getName(),
//                            null,
//                            null
//                    );
//                })
//                .toList();
//    }

//    public List<UserNearbyResponse> findUsersWithSong(List<Long> ids) {
//
//        return userRepository.findAllById(ids).stream()
//                .map(user -> {
//                    Song song = user.getFavoriteSong();
//
//                    return new UserNearbyResponse(
//                            user.getUserId(),
//                            user.getNickname(),
//                           song != null ? song.getTitle() : null,
//                           song != null ? song.getArtist().getName() : null
//                    );
//                })
//                .toList();
//    }

}
