package cl.ufro.dci.naivepayapi.autentificacion.configuration.security;


import cl.ufro.dci.naivepayapi.autentificacion.service.AuthSessionService;
import cl.ufro.dci.naivepayapi.autentificacion.service.JWTService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.util.AntPathMatcher;
import org.springframework.http.HttpMethod;


import java.io.IOException;
import java.util.Collections;
import java.util.UUID;


@Component
public class JwtAuthFilter extends OncePerRequestFilter {


    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";


    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();


    private static final String[] PUBLIC_PATHS = {
            "/h2-console/**",
            "/api/register/**",
            "/auth/password/**",
            "/auth/login",
            "/api/devices/recover/**",
            "/api/dispositivos/recover/**"
    };


    private final JWTService jwtService;
    private final AuthSessionService authSessionService;


    public JwtAuthFilter(JWTService jwtService, AuthSessionService authSessionService) {
        this.jwtService = jwtService;
        this.authSessionService = authSessionService;
    }


    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {


        final String uri = request.getRequestURI();


        if (HttpMethod.OPTIONS.matches(request.getMethod()) || isPublic(request.getRequestURI())) {
            chain.doFilter(request, response);
            return;
        }


        final String header = request.getHeader(AUTH_HEADER);
        if (header == null || !header.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            chain.doFilter(request, response);
            return;
        }


        final String token = header.substring(BEARER_PREFIX.length()).trim();


        try {
            final Claims claims = jwtService.parse(token);
            final UUID jti = UUID.fromString(claims.getId());


            if (authSessionService.findActiveByJti(jti).isEmpty()) {
                SecurityContextHolder.clearContext();
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }


            var auth = new UsernamePasswordAuthenticationToken(
                    claims.getSubject(),
                    null,
                    Collections.emptyList()
            );
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(auth);


            chain.doFilter(request, response);


        } catch (ExpiredJwtException ex) {
            SecurityContextHolder.clearContext();
            try {
                UUID jti = UUID.fromString(ex.getClaims().getId());
                authSessionService.closeByJti(jti);
            } catch (Exception ignored) {
            }
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);


        } catch (JwtException ex) {
            SecurityContextHolder.clearContext();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }


    private boolean isPublic(String uri) {
        for (String pattern : PUBLIC_PATHS) {
            if (PATH_MATCHER.match(pattern, uri)) {
                return true;
            }
        }
        return false;
    }
}