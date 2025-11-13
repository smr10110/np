package cl.ufro.dci.naivepayapi.registro.configuration;

import cl.ufro.dci.naivepayapi.fondos.domain.Account;
import cl.ufro.dci.naivepayapi.fondos.repository.AccountRepository;
import cl.ufro.dci.naivepayapi.registro.domain.AccountState;
import cl.ufro.dci.naivepayapi.registro.domain.Credencial;
import cl.ufro.dci.naivepayapi.registro.domain.Register;
import cl.ufro.dci.naivepayapi.registro.domain.User;
import cl.ufro.dci.naivepayapi.registro.domain.UserRole;
import cl.ufro.dci.naivepayapi.registro.repository.CredencialRepository;
import cl.ufro.dci.naivepayapi.registro.repository.RegisterRepository;
import cl.ufro.dci.naivepayapi.registro.repository.UserRepository;
import cl.ufro.dci.naivepayapi.registro.service.RsaKeyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.security.KeyPair;
import java.time.LocalDate;
import java.util.Base64;
import java.util.Date;

/**
 * Inicializa el usuario administrador en la base de datos al arrancar la aplicación.
 * Solo crea el admin si no existe (idempotente).
 *
 * CREDENCIALES ADMIN (Solo desarrollo):
 * - Email: admin@naivepay.cl
 * - RUT: 11111111-1
 * - Password: Admin@2025
 * - Rol: ADMIN
 *
 * NOTA: Esta clase está DESACTIVADA porque se usa data.sql en su lugar.
 * Para activar este enfoque en lugar de data.sql:
 * 1. Descomentar @Component("adminUserInitializer")
 * 2. Borrar o renombrar src/main/resources/data.sql
 * 3. Quitar spring.jpa.defer-datasource-initialization=true de application-dev.properties
 */
//@Component("adminUserInitializer") // DESACTIVADO - usando data.sql en su lugar
public class AdminUserInitializer implements CommandLineRunner {

    private static final String ADMIN_EMAIL = "admin@naivepay.cl";
    private static final String ADMIN_PASSWORD = "Admin@2025";
    private static final Long ADMIN_RUT = 11111111L;
    private static final char ADMIN_DV = '1';

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RegisterRepository registerRepository;

    @Autowired
    private CredencialRepository credencialRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Verificar si ya existe el admin por email
        Register existingRegister = registerRepository.findByRegEmail(ADMIN_EMAIL);

        if (existingRegister != null && existingRegister.isRegVerified()) {
            System.out.println("ℹ️  Usuario ADMIN ya existe: " + ADMIN_EMAIL);
            return;
        }

        System.out.println("🔧 Creando usuario ADMIN...");

        // 1. Crear Register
        Register register = new Register();
        register.setRegEmail(ADMIN_EMAIL);
        register.setRegHashedLoginPassword(passwordEncoder.encode(ADMIN_PASSWORD));
        register.setRegRegisterDate(new Date());
        register.setRegVerified(true); // Ya verificado
        register.setRegVerificationCode(null);
        register.setRegVerificationCodeExpiration(null);
        registerRepository.save(register);

        // 2. Crear Credencial con claves RSA
        Credencial credencial = new Credencial();
        credencial.setCreCreationDate(new Date());
        credencial.setCreDenied(false);
        credencial.setCreActiveDinamicKey(false); // Admin no necesita clave dinámica

        try {
            KeyPair keyPair = RsaKeyService.generateRsaKeyPair();
            String privateKey = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());
            String publicKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
            credencial.setCrePrivateKeyRsa(privateKey);
            credencial.setCrePublicKeyRsa(publicKey);
        } catch (Exception e) {
            throw new RuntimeException("Error generando claves RSA para admin", e);
        }

        credencialRepository.save(credencial);

        // 3. Crear User
        User admin = new User();
        admin.setUseNames("Admin");
        admin.setUseLastNames("Sistema");
        admin.setUseRutGeneral(ADMIN_RUT);
        admin.setUseVerificationDigit(ADMIN_DV);
        admin.setUseBirthDate(LocalDate.of(1990, 1, 1));
        admin.setUsePhoneNumber(56912345678L);
        admin.setUseProfession("Administrador");
        admin.setUseAdress("UFRO");
        admin.setUseState(AccountState.ACTIVE);
        admin.setUseRole(UserRole.ADMIN); // ROL ADMIN
        admin.setCredencial(credencial);
        admin.setRegister(register);

        User savedAdmin = userRepository.save(admin);

        // 4. Crear cuenta de fondos
        Account account = new Account(savedAdmin.getUseId());
        account.updateBalance(BigDecimal.ZERO);
        accountRepository.save(account);

        System.out.println("✅ Usuario ADMIN creado exitosamente");
        System.out.println("   Email: " + ADMIN_EMAIL);
        System.out.println("   RUT: " + ADMIN_RUT + "-" + ADMIN_DV);
        System.out.println("   Password: " + ADMIN_PASSWORD);
        System.out.println("   Rol: ADMIN");
    }
}
