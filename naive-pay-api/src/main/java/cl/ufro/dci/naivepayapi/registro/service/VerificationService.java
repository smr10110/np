package cl.ufro.dci.naivepayapi.registro.service;

import cl.ufro.dci.naivepayapi.registro.domain.User;
import cl.ufro.dci.naivepayapi.registro.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Service
public class VerificationService {

    private final UserRepository userRepository;

    public VerificationService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public boolean verifyUserSignature(String userEmail, String plainText, String signature) throws Exception {
        User user = userRepository.findByRegisterRegEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        String publicKeyStr = user.getCredencial().getCrePublicKeyRsa();
        if (publicKeyStr == null) {
            throw new IllegalStateException("El usuario no tiene una clave pública registrada.");
        }

        // 3. Reconstruir el objeto PublicKey desde el string Base64
        byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyStr);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);
        PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);

        // 4. Usar RsaKeyService para verificar la firma
        return RsaKeyService.verify(plainText, signature, publicKey);
    }
}
