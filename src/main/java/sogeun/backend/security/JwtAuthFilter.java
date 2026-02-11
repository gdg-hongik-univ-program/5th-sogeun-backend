package sogeun.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;

    public JwtAuthFilter(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        System.out.println("[JwtAuthFilter] "
                + request.getMethod() + " " + request.getRequestURI());

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        System.out.println("[JwtAuthFilter] Authorization header = " + authHeader);

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            System.out.println("[JwtAuthFilter] token extracted");

            boolean valid = jwtProvider.validate(token);
            System.out.println("[JwtAuthFilter] token valid = " + valid);

            if (valid) {
                // ✅ typ 검사: access 토큰만 인증 처리
                String typ = jwtProvider.parseTokenType(token);
                System.out.println("[JwtAuthFilter] token typ = " + typ);

                if (!"access".equals(typ)) {
                    System.out.println("[JwtAuthFilter] Not an access token -> skip authentication");
                    filterChain.doFilter(request, response);
                    return;
                }

                Long userId = jwtProvider.parseUserId(token);
                System.out.println("[JwtAuthFilter] parsed userId = " + userId);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userId, null, List.of());

                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                SecurityContextHolder.getContext().setAuthentication(authentication);
                System.out.println("[JwtAuthFilter] Authentication set in SecurityContext");
            }
        } else {
            System.out.println("[JwtAuthFilter] No Bearer token");
        }

        filterChain.doFilter(request, response);
    }
}
