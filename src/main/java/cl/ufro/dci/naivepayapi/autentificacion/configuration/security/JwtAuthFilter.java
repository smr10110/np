package cl.ufro.dci.naivepayapi.autentificacion.configuration.security;


import cl.ufro.dci.naivepayapi.autentificacion.service.AuthSessionService;
import cl.ufro.dci.naivepayapi.autentificacion.service.JWTService;
import cl.ufro.dci.naivepayapi.dispositivos.service.DeviceService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String DEVICE_FINGERPRINT_HEADER = "X-Device-Fingerprint";


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
    private final DeviceService deviceService;


    public JwtAuthFilter(JWTService jwtService, AuthSessionService authSessionService, DeviceService deviceService) {
        this.jwtService = jwtService;
        this.authSessionService = authSessionService;
        this.deviceService = deviceService;
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

        // Validar que el token no esté vacío
        if (token.isEmpty()) {
            log.warn("Empty token received from IP: {}", request.getRemoteAddr());
            write401(response, "TOKEN_MISSING", "Token vacío");
            return;
        }

        try {
            final Claims claims = jwtService.parse(token);
            final UUID jti = UUID.fromString(claims.getId());

            if (authSessionService.findActiveByJti(jti).isEmpty()) {
                log.info("Session closed for JTI: {}", jti);
                write401(response, "TOKEN_CLOSED", "Sesión cerrada");
                return;
            }

            // Validar que el dispositivo coincida (excepto para logout)
            if (!uri.equals("/auth/logout")) {
                final String fingerprintFromRequest = request.getHeader(DEVICE_FINGERPRINT_HEADER);

                // Validar que el fingerprint esté presente
                if (fingerprintFromRequest == null || fingerprintFromRequest.trim().isEmpty()) {
                    log.warn("Missing device fingerprint for user from IP: {}", request.getRemoteAddr());
                    SecurityContextHolder.clearContext();
                    write401(response, "DEVICE_FINGERPRINT_MISSING", "Fingerprint del dispositivo requerido");
                    return;
                }

                final Long userId = Long.valueOf(claims.getSubject());

                // Validar que el fingerprint de la petición coincida con el del usuario
                try {
                    deviceService.ensureAuthorizedDevice(userId, fingerprintFromRequest);
                } catch (IllegalArgumentException | IllegalStateException ex) {
                    log.warn("Device unauthorized for user {}: {}", userId, ex.getMessage());
                    SecurityContextHolder.clearContext();
                    write401(response, "DEVICE_UNAUTHORIZED", "Dispositivo no autorizado para esta sesión");
                    return;
                } catch (Exception ex) {
                    log.error("Unexpected error validating device for user {}: {}", userId, ex.getMessage(), ex);
                    SecurityContextHolder.clearContext();
                    write401(response, "DEVICE_VALIDATION_ERROR", "Error al validar dispositivo");
                    return;
                }
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
                log.info("Session closed due to token expiration, JTI: {}", jti);
            } catch (Exception e) {
                log.error("Error closing expired session: {}", e.getMessage(), e);
            }
            write401(response, "TOKEN_EXPIRED", "El token ha expirado");

        } catch (JwtException ex) {
            SecurityContextHolder.clearContext();
            log.warn("Invalid JWT token: {}", ex.getMessage());
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
