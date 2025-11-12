package cl.ufro.dci.naivepayapi.autentificacion.service;

import io.jsonwebtoken.Claims;
import java.time.Instant;

public interface JWTService {
    String generate(String userId, String deviceFingerprint, String jti); // TTL 15 min
    Claims parse(String token);                                      // lanza si firma inválida/expirado
    String getUserId(String token);
    String getJti(String token);
    Instant getExpiration(String token);
    boolean isExpired(String token);
    String getDeviceFingerprint(String token);
}
