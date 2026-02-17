package sogeun.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.geo.Point;
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
import sogeun.backend.entity.Broadcast;
import sogeun.backend.entity.Music;
import sogeun.backend.entity.User;
//import sogeun.backend.repository.ArtistRepository;
import sogeun.backend.repository.BroadcastRepository;
import sogeun.backend.repository.MusicLikeRepository;
import sogeun.backend.repository.UserRepository;
import sogeun.backend.security.JwtProvider;
import sogeun.backend.security.RefreshTokenRepository;
import sogeun.backend.sse.dto.MusicDto;
import sogeun.backend.sse.dto.UserNearbyResponse;
import sogeun.backend.sse.LocationService;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;
//    private final ArtistRepository artistRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final BroadcastRepository broadcastRepository;
    private final MusicLikeRepository musicLikeRepository;
    private final LocationService locationService;


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

    @Transactional(readOnly = true)
    public List<UserNearbyResponse> findUsersWithSong(List<Long> userIds) {

        if (userIds.isEmpty()) return List.of();

        List<User> users = userRepository.findAllById(userIds);

        List<Broadcast> broadcasts =
                broadcastRepository.findBySenderIdInAndIsActiveTrue(userIds);

        Map<Long, Broadcast> broadcastMap = broadcasts.stream()
                .collect(Collectors.toMap(Broadcast::getSenderId, b -> b));

        return users.stream()
                .map(user -> {
                    Broadcast b = broadcastMap.get(user.getUserId());
                    if (b == null) return null; // 방송 안 하면 제외

                    Music m = b.getMusic();
                    if (m == null) return null; // 음악 없으면 제외

                    return new UserNearbyResponse(
                            user.getUserId(),
                            user.getNickname(),
                            true,
                            b.getBroadcastId(),
                            new MusicDto(
                                    m.getTrackId(),
                                    m.getTitle(),
                                    m.getArtist(),
                                    m.getArtworkUrl(),
                                    m.getPreviewUrl()
                            ),
                            b.getRadiusMeter(),
                            b.getLikeCount()
                    );
                })
                .filter(Objects::nonNull)
                .toList();
    }



    // 내 주변 '방송중' 유저 조회
    @Transactional(readOnly = true)
    public List<UserNearbyResponse> findNearbyBroadcastingUsers(Long userId) {

        log.info("[NEARBY] start requesterId={}", userId);

        Point p = locationService.getLocation(userId);
        if (p == null) {
            log.warn("[NEARBY] no location requesterId={}", userId);
            return List.of(); // 또는 예외
        }

        // Point 규칙: x=lon, y=lat
        double lat = p.getY();
        double lon = p.getX();

        log.info("[NEARBY] location requesterId={} lat={} lon={} (pointX={} pointY={})",
                userId, lat, lon, p.getX(), p.getY());

        final double NEARBY_RADIUS_METER = 500.0;
        log.info("[NEARBY] radius meter={} requesterId={}", NEARBY_RADIUS_METER, userId);

        List<Long> ids = locationService.findNearbyUsersWithRadius(
                userId,
                lat,
                lon,
                NEARBY_RADIUS_METER
        );

        if (ids == null) {
            log.warn("[NEARBY] ids is null requesterId={}", userId);
            return List.of();
        }

        log.info("[NEARBY] foundIds requesterId={} count={} ids={}",
                userId, ids.size(), ids);

        List<UserNearbyResponse> result = findUsersWithSong(ids);

        log.info("[NEARBY] result requesterId={} count={}",
                userId, result.size());

        if (!result.isEmpty()) {
            String summary = result.stream()
                    .limit(5)
                    .map(r -> String.format(
                            "{userId=%d,nick=%s,broadcastId=%s,like=%s,radius=%s}",
                            r.userId(),
                            r.nickname(),
                            String.valueOf(r.broadcastId()),
                            String.valueOf(r.likeCount()),
                            String.valueOf(r.radiusMeter())
                    ))
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("");
            log.info("[NEARBY] result sample requesterId={} sample=[{}]", userId, summary);
        } else {
            log.info("[NEARBY] result empty requesterId={} (filtered by broadcast/music?)", userId);
        }

        log.info("[NEARBY] done requesterId={}", userId);
        return result;
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
////                    Song song = user.getFavoriteSong();
//
//                    return new UserNearbyResponse(
//                            user.getUserId(),
//                            user.getNickname(),
////                            song != null ? song.getTitle() : null,
////                            song != null ? song.getArtist().getName() : null
//                    );
//                })
//                .toList();
//    }

}
