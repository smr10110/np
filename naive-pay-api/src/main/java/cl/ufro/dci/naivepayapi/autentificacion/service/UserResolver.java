package cl.ufro.dci.naivepayapi.autentificacion.service;

import cl.ufro.dci.naivepayapi.registro.domain.User;
import cl.ufro.dci.naivepayapi.registro.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service responsible for resolving users by different identifier types (email or RUT).
 * Centralizes user resolution logic to be reused across authentication and registration flows.
 */
@Service
@RequiredArgsConstructor
public class UserResolver {

    private final UserRepository userRepo;

    /**
     * Resolves a user by their identifier, which can be either an email or a Chilean RUT.
     *
     * @param identifier User identifier (email or RUT with format "12345678-9")
     * @return Optional containing the User if found, empty otherwise
     */
    public Optional<User> resolve(String identifier) {
        final String id = identifier.trim();

        // Try to resolve as email first
        if (RutUtils.isEmail(id)) {
            return userRepo.findByRegisterRegEmail(id);
        }

        // Try to resolve as RUT
        var rut = RutUtils.parseRut(id).orElse(null);
        if (rut == null) {
            return Optional.empty();
        }

        try {
            Long rutNum = Long.parseLong(rut.rut());
            return userRepo.findByUseRutGeneral(rutNum)
                    .filter(u -> Character.toUpperCase(u.getUseVerificationDigit()) == rut.dv());
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
