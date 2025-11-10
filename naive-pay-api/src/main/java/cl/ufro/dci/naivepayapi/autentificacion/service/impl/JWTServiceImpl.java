package cl.ufro.dci.naivepayapi.autentificacion.service.impl;

import cl.ufro.dci.naivepayapi.autentificacion.service.JWTService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
public class JWTServiceImpl implements JWTService {

    @Value("${security.jwt.secret}")
    private String secret;

    @Value("${security.jwt.ttl-minutes}") // toma del properties
    private long ttlMinutes;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String generate(String userId, String deviceFingerprint, String jti) {
        Instant now = Instant.now();
        return Jwts.builder()
                .id(jti)                               // jti
                .subject(userId)
                .claim("deviceFingerprint", deviceFingerprint) //  <------------------ Devices Cambio
                .issuedAt(Date.from(now))              // iat
                .expiration(Date.from(now.plus(ttlMinutes, ChronoUnit.MINUTES))) // exp
                .signWith(key())
                .compact();
    }

    /** Parsea y valida firma/estructura. Si está expirado o es inválido, lanza JwtException. */
    @Override
    public Claims parse(String token) throws JwtException {
        return Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // --- Helpers seguros para usar en controladores ---

    /** true si la firma/estructura es válida (puede estar expirado). */
    public boolean isValidSignature(String token) {
        try {
            parse(token);              // si expira igual lanza JwtException → capturamos abajo
            return true;
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            return true;               // firma OK pero expiró (ya se responderá 401 por el handler)
        } catch (JwtException e) {
            return false;              // firma/estructura inválida
        }
    }

    @Override
    public boolean isExpired(String token) {
        try {
            return Instant.now().isAfter(getExpiration(token));
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            return true;
        }
    }

    @Override public String  getUserId(String token)       { return parse(token).getSubject(); }
    @Override public String  getJti(String token)          { return parse(token).getId(); }
    @Override public Instant getExpiration(String token)   { return parse(token).getExpiration().toInstant(); }
    @Override public String getDeviceFingerprint(String token) { return parse(token).get("deviceFingerprint", String.class); } //  <------------------ Devices Cambio
}
