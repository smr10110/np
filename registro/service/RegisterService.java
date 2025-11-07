package cl.ufro.dci.naivepayapi.registro.service;

import cl.ufro.dci.naivepayapi.dispositivos.service.DeviceService; //D
import cl.ufro.dci.naivepayapi.registro.domain.AccountState;
import cl.ufro.dci.naivepayapi.registro.domain.Credencial;
import cl.ufro.dci.naivepayapi.registro.domain.Register;
import cl.ufro.dci.naivepayapi.registro.domain.User;
import cl.ufro.dci.naivepayapi.registro.dto.RegistrationCompleteDTO;
import cl.ufro.dci.naivepayapi.registro.dto.RegistrationStartDTO;
import cl.ufro.dci.naivepayapi.registro.repository.CredencialRepository;
import cl.ufro.dci.naivepayapi.registro.repository.RegisterRepository;
import cl.ufro.dci.naivepayapi.registro.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Calendar;

import java.util.Date;
import java.util.UUID;



@Service
public class RegisterService {
    private final UserRepository userRepository;
    private final RegisterRepository registerRepository;
    private final CredencialRepository credencialRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final ApplicationEventPublisher eventPublisher; // campo para los eventos
    private final DeviceService deviceService;

    public RegisterService(UserRepository userRepository, RegisterRepository registerRepository, CredencialRepository credencialRepository, PasswordEncoder passwordEncoder, EmailService emailService, ApplicationEventPublisher eventPublisher, DeviceService deviceService) {
        this.userRepository = userRepository;
        this.registerRepository = registerRepository;
        this.credencialRepository = credencialRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.eventPublisher = eventPublisher;
        this.deviceService = deviceService;
    }


    @Transactional
    public void startRegistration(RegistrationStartDTO startData) {
        // Se verifica si el email ya esta registrado
        Register existingRegister = registerRepository.findByRegEmail(startData.getEmail());
        if (existingRegister != null && existingRegister.isRegVerified()) {
            throw new IllegalStateException("El email ya está registrado y verificado.");
        }

        // Se crea el codigo de verificacion
        String verificationCode = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        // Se hashea la contraseña
        String hashedPassword = passwordEncoder.encode(startData.getPassword());

        // Se calcula la fecha de expiracion del codigo de verificacion
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, 2);
        Date expirationDate = calendar.getTime();

        // Se crea el objeto registro y se guarda
        Register register = new Register();
        register.setRegEmail(startData.getEmail());
        register.setRegHashedLoginPassword(hashedPassword);
        register.setRegRegisterDate(new Date());
        register.setRegVerified(false);
        register.setRegVerificationCode(verificationCode);
        register.setRegVerificationCodeExpiration(expirationDate);
        registerRepository.save(register);

        // Se envia el email de verificacion
        emailService.sendVerificationEmail(register.getRegEmail(), verificationCode);
    }


    @Transactional
    public void verifyEmail(String email, String code) {
        // Se obtiene el registro por email
        Register register = registerRepository.findByRegEmail(email);

        // Se verifica que el codigo no haya vencido
        if (register.getRegVerificationCodeExpiration().before(new Date())) {
            throw new IllegalStateException("El código de verificación ha expirado.");
        }

        // Se valida el codigo de verificacion
        if (register.getRegVerificationCode() == null || !register.getRegVerificationCode().equals(code)) {
            throw new IllegalStateException("Código de verificación inválido.");
        }
        // Se actualiza el registro como verificado y se guarda
        register.setRegVerified(true);
        register.setRegVerificationCode(null);
        register.setRegVerificationCodeExpiration(null);
        registerRepository.save(register);
    }

    @Transactional
    public void resendVerificationCode(String email) {
        if (email == null || email.isEmpty()) {
            throw new IllegalArgumentException("El email no puede ser nulo o vacío.");
        }

        Register register = registerRepository.findByRegEmail(email);
        if (register == null) {
            throw new IllegalStateException("No se encontró un registro con el email proporcionado.");
        }

        if (register.isRegVerified()) {
            throw new IllegalStateException("Este email ya ha sido verificado.");
        }

        // Generar un nuevo código y actualizar la fecha de expiración
        String newCode = generateNewVerificationCode();
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, 2); // Mantiene la misma duración que el registro inicial
        Date newExpirationDate = calendar.getTime();

        register.setRegVerificationCode(newCode);
        register.setRegVerificationCodeExpiration(newExpirationDate);
        registerRepository.save(register);

        // Enviar el nuevo código por email
        emailService.sendVerificationEmail(register.getRegEmail(), newCode);
    }

    private String generateNewVerificationCode() {
        return UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    @Transactional
    public User completeRegistration(RegistrationCompleteDTO completeData) {
        // se valida directamente el RUT
        String rutString = completeData.getRutGeneral().toString() + "-" + completeData.getVerificationDigit();
        if (!RutUtils.isValid(rutString)) {
            throw new IllegalArgumentException("El RUT ingresado no es válido.");
        }

        Register register = registerRepository.findByRegEmail(completeData.getEmail());

        // verificar el rut completo
        if (userRepository.findByUseRutGeneralAndUseVerificationDigit(completeData.getRutGeneral(), completeData.getVerificationDigit()).isPresent()) {
            throw new IllegalStateException("El RUT ya está registrado.");
        }

        // Se creo el objeto base de credencial
        Credencial credencial = createDefaultCredencial();

        // se generan el par de claves RSA
        try {
            // 1. Generar el par de claves RSA
            KeyPair keyPair = RsaKeyService.generateRsaKeyPair();
            String privateKeyStr = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());
            String publicKeyStr = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());

            // 3. Asignar las claves a la entidad Credencial
            credencial.setCrePrivateKeyRsa(privateKeyStr);
            credencial.setCrePublicKeyRsa(publicKeyStr);

        } catch (NoSuchAlgorithmException e) {
            // Es crucial manejar esta excepción, aunque es rara.
            throw new RuntimeException("Error al generar las claves RSA", e);
        }

        credencialRepository.save(credencial);

        // Se crea el objeto usuario segun los datos del dto
        User user = buildUserFromDTO(completeData);
        user.setCredencial(credencial);
        user.setRegister(register);

        // Se guarda el usuario
        User savedUser = userRepository.save(user);

        //Agregar fp
        String fp = completeData.getFingerprint();
        deviceService.registerForUser(
                savedUser.getUseId(),
                completeData.getFingerprint(),
                completeData.getType(),
                completeData.getOs(),
                completeData.getBrowser()
        );

        // Publica un evento despues del registro de usuario.
        eventPublisher.publishEvent(new UserRegisteredEvent(this, savedUser.getUseId()));

        return savedUser;
    }


    public void setDynamicKey(String email, String dynamicKey) {
        // Se valida el formato de la clave dinamica
        if (dynamicKey == null || !dynamicKey.matches("\\d{6}")) {
            throw new IllegalArgumentException("La clave dinámica debe ser de 6 dígitos.");
        }

        // Se busca el registro por email para obtener el usuario
        Register register = registerRepository.findByRegEmail(email);
        if (register == null || register.getUser() == null) {
            throw new RuntimeException("Usuario no encontrado con el email: " + email);
        }
        User user = register.getUser();

        Credencial credencial = user.getCredencial();
        if (credencial == null) {
            throw new RuntimeException("El usuario no tiene credenciales asociadas.");
        }

        // Se settea la clave dinámica y se guarda
        credencialRepository.save(credencial);
    }


    // funcion para el objeto base de credencial
    private Credencial createDefaultCredencial() {
        Credencial credencial = new Credencial();
        credencial.setCreCreationDate(new Date());
        credencial.setCreDenied(false);
        credencial.setCreActiveDinamicKey(true);
        return credencial;
    }

    // funcion para crear el objeto usuario desde el dto
    private User buildUserFromDTO(RegistrationCompleteDTO userData) {
        User user = new User();
        user.setUseNames(userData.getNames());
        user.setUseLastNames(userData.getLastNames());
        user.setUseRutGeneral(userData.getRutGeneral());
        user.setUseBirthDate(userData.getBirthDate());
        user.setUseVerificationDigit(userData.getVerificationDigit());
        user.setUsePhoneNumber(userData.getPhoneNumber());
        user.setUseProfession(userData.getProfession());
        user.setUseAdress(userData.getAdress());
        user.setUseState(AccountState.ACTIVE);
        return user;
    }
}