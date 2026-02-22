package sogeun.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        // Bearer 없으면 그냥 통과
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        //토큰 검증 실패시
        try {
            if (!jwtProvider.validate(token)) {
                log.warn("[JWT] invalid token (validation failed). {} {}", request.getMethod(), request.getRequestURI());
                filterChain.doFilter(request, response);
                return;
            }
        } catch (Exception e) {
            // 검증 과정에서 발생한 구체적인 에러 메시지(만료, 서명 불일치 등)를 로그로 남깁니다.
            log.warn("[JWT] invalid token. Reason: {} | Path: {} {}", e.getMessage(), request.getMethod(), request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }
        // access 토큰만 인증 처리 (refresh면 스킵)
        String typ = jwtProvider.parseTokenType(token);
        if (!"access".equals(typ)) {
            log.debug("[JWT] non-access token typ={} -> skip. {} {}", typ, request.getMethod(), request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        Long userId = jwtProvider.parseUserId(token);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userId, null, List.of());

        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        log.debug("[JWT] authenticated userId={} {} {}", userId, request.getMethod(), request.getRequestURI());

        filterChain.doFilter(request, response);
    }
}