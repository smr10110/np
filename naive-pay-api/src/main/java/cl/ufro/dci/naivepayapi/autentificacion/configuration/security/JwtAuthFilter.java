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
import org.springframework.web.server.ResponseStatusException;


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
                write401(response, "TOKEN_CLOSED", "Sesión cerrada");
                return;
            }

            // Actualizar última actividad de la sesión
            try {
                authSessionService.updateLastActivity(jti);
            } catch (ResponseStatusException e) {
                String errorMessage = e.getReason() != null ? e.getReason() : "";
                if ("SESSION_EXPIRED".equals(errorMessage)) {
                    write401(response, "SESSION_EXPIRED", "Sesión expirada por límite de tiempo");
                } else if ("SESSION_INACTIVE".equals(errorMessage)) {
                    write401(response, "SESSION_INACTIVE", "Sesión cerrada por inactividad");
                } else {
                    write401(response, "SESSION_ERROR", "Error en la sesión");
                }
                return;
            } catch (IllegalArgumentException e) {
                write401(response, "SESSION_NOT_FOUND", "Sesión no encontrada");
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
            write401(response, "TOKEN_EXPIRED", "El token ha expirado");


        } catch (JwtException ex) {
            SecurityContextHolder.clearContext();
            write401(response, "TOKEN_INVALID", "Token inválido o mal formado");
        }
    }


    private boolean isPublic(String uri) {
        for (String pattern : PUBLIC_PATHS) {
            if (PATH_MATCHER.match(pattern, uri)) {
                System.out.println("Ruta pública detectada: " + uri);
                return true;
            }
        }
        return false;
    }


    private void write401(HttpServletResponse response, String code, String msg) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.setHeader("WWW-Authenticate",
                "Bearer error=\"invalid_token\", error_description=\"" + msg + "\"");
        response.getWriter().write("{\"error\":\"" + code + "\",\"message\":\"" + msg + "\"}");
    }
}