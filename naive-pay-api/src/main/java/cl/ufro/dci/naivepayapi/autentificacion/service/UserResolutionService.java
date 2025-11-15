package cl.ufro.dci.naivepayapi.autentificacion.service;

import cl.ufro.dci.naivepayapi.registro.domain.User;
import cl.ufro.dci.naivepayapi.registro.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Servicio responsable de resolver usuarios a partir de un identificador (email o RUT).
 *
 * Este servicio centraliza la lógica de búsqueda de usuarios, que antes estaba duplicada
 * en múltiples servicios del sistema. Soporta dos tipos de identificadores:
 *
 * <ul>
 *   <li><b>Email:</b> Busca el usuario por su dirección de correo electrónico</li>
 *   <li><b>RUT:</b> Busca el usuario por su RUT y valida el dígito verificador (DV)</li>
 * </ul>
 *
 * <p>El servicio maneja automáticamente la detección del tipo de identificador y
 * aplica la lógica de búsqueda correspondiente.</p>
 *
 * @see RutUtils
 * @author Sistema de Autenticación
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
public class UserResolutionService {

    private static final Logger logger = LoggerFactory.getLogger(UserResolutionService.class);

    private final UserRepository userRepository;

    /**
     * Resuelve un usuario a partir de un identificador que puede ser email o RUT.
     *
     * <p>El método detecta automáticamente el tipo de identificador:</p>
     * <ul>
     *   <li>Si el identificador contiene '@', se asume que es un email</li>
     *   <li>Si no contiene '@', se intenta parsear como RUT chileno con DV</li>
     * </ul>
     *
     * <p>Para RUTs, el método valida que el dígito verificador (DV) coincida con
     * el almacenado en la base de datos. La validación del DV es case-insensitive.</p>
     *
     * <h3>Ejemplos de uso:</h3>
     * <pre>
     * // Búsqueda por email
     * Optional&lt;User&gt; user = resolveUser("juan.perez@example.com");
     *
     * // Búsqueda por RUT con formato
     * Optional&lt;User&gt; user = resolveUser("12.345.678-9");
     *
     * // Búsqueda por RUT sin formato
     * Optional&lt;User&gt; user = resolveUser("123456789");
     * </pre>
     *
     * @param identifier identificador del usuario (email o RUT). No puede ser null ni vacío.
     * @return {@link Optional} conteniendo el usuario si se encontró, o {@link Optional#empty()}
     *         si no existe o el identificador es inválido
     *
     * @throws IllegalArgumentException si el identifier es null o está vacío
     */
    public Optional<User> resolveUser(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            logger.debug("Identificador nulo o vacío, retornando Optional.empty()");
            return Optional.empty();
        }

        String trimmedId = identifier.trim();
        logger.debug("Resolviendo usuario con identificador: {}", trimmedId);

        // Detectar si es email o RUT
        if (RutUtils.isEmail(trimmedId)) {
            return resolveByEmail(trimmedId);
        } else {
            return resolveByRut(trimmedId);
        }
    }

    /**
     * Busca un usuario por su dirección de email.
     *
     * @param email dirección de correo electrónico del usuario
     * @return {@link Optional} conteniendo el usuario si existe
     */
    private Optional<User> resolveByEmail(String email) {
        logger.debug("Buscando usuario por email: {}", email);
        Optional<User> user = userRepository.findByRegisterRegEmail(email);

        if (user.isPresent()) {
            logger.debug("Usuario encontrado por email: {} (userId={})", email, user.get().getUseId());
        } else {
            logger.debug("Usuario no encontrado por email: {}", email);
        }

        return user;
    }

    /**
     * Busca un usuario por su RUT y valida el dígito verificador.
     *
     * <p>El método parsea el RUT utilizando {@link RutUtils#parseRut(String)}, que soporta
     * múltiples formatos (con/sin puntos, con/sin guión). Luego busca el usuario por el
     * número de RUT y valida que el DV coincida.</p>
     *
     * <p>La validación del DV es case-insensitive (K y k son equivalentes).</p>
     *
     * @param rawRut RUT en cualquier formato (ej: "12.345.678-9", "12345678-9", "123456789")
     * @return {@link Optional} conteniendo el usuario si el RUT y DV coinciden, o
     *         {@link Optional#empty()} si no existe o el RUT es inválido
     */
    private Optional<User> resolveByRut(String rawRut) {
        logger.debug("Intentando parsear RUT: {}", rawRut);

        var rutParsed = RutUtils.parseRut(rawRut).orElse(null);
        if (rutParsed == null) {
            logger.debug("RUT inválido o mal formateado: {}", rawRut);
            return Optional.empty();
        }

        try {
            Long rutNumber = Long.parseLong(rutParsed.rut());
            char expectedDv = rutParsed.dv();

            logger.debug("Buscando usuario por RUT: {} (DV esperado: {})", rutNumber, expectedDv);

            Optional<User> userOpt = userRepository.findByUseRutGeneral(rutNumber);

            if (userOpt.isEmpty()) {
                logger.debug("Usuario no encontrado con RUT: {}", rutNumber);
                return Optional.empty();
            }

            User user = userOpt.get();
            char actualDv = Character.toUpperCase(user.getUseVerificationDigit());

            if (actualDv == Character.toUpperCase(expectedDv)) {
                logger.debug("Usuario encontrado por RUT: {}-{} (userId={})",
                    rutNumber, actualDv, user.getUseId());
                return Optional.of(user);
            } else {
                logger.debug("DV no coincide para RUT {}: esperado={}, actual={}",
                    rutNumber, expectedDv, actualDv);
                return Optional.empty();
            }

        } catch (NumberFormatException e) {
            logger.debug("Error al parsear número de RUT: {}", rawRut, e);
            return Optional.empty();
        }
    }
}
