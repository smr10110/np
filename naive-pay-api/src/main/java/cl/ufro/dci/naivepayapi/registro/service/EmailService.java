package cl.ufro.dci.naivepayapi.registro.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendVerificationEmail(String to, String verificationCode) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Código de Verificación de Correo");
        message.setText("Hola,\n\nTu código de verificación es: " + verificationCode );
        mailSender.send(message);
    }

    public void sendDeviceRecoveryEmail(String to, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Código para vincular tu nuevo dispositivo");
        message.setText(
                "Hola,\n\n" +
                        "Detectamos un intento de inicio de sesión a tu cuenta desde un dispositivo no vinculado.\n" +
                        "Para autorizarlo y vincular este nuevo dispositivo a tu cuenta, ingresa el siguiente código:\n\n" +
                        code + "\n\n" +
                        "Este código expira en 10 minutos.\n\n" +
                        "Si no fuiste tú, ignora este mensaje."
        );
        mailSender.send(message);
    }

    public void sendPasswordRecoveryEmail(String to, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Recuperación de Contraseña - NaivePay");
        message.setText(
                "Hola,\n\n" +
                        "Recibimos una solicitud para restablecer tu contraseña.\n" +
                        "Usa el siguiente código de verificación:\n\n" +
                        code + "\n\n" +
                        "Este código expira en 10 minutos.\n\n" +
                        "Si no solicitaste este cambio, ignora este mensaje y tu contraseña permanecerá sin cambios."
        );
        mailSender.send(message);
    }

    public void sendPasswordChangeConfirmation(String to, String userName) {
        // Genera timestamp para registrar cuándo ocurrió el cambio
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Contraseña Actualizada - NaivePay");
        message.setText(
                "Hola " + userName + ",\n\n" +
                        "Tu contraseña ha sido cambiada exitosamente.\n\n" +
                        "Fecha y hora: " + timestamp + "\n\n" +
                        "---\n" +
                        "Equipo NaivePay"
        );
        mailSender.send(message);
    }

    public void sendAccountBlockedNotice(String to) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Cuenta bloqueada por seguridad - NaivePay");
        message.setText(
                "Hola,\n\n" +
                "Por seguridad hemos bloqueado temporalmente tu cuenta tras varios intentos fallidos de inicio de sesión.\n\n" +
                "Sigue estos pasos para recuperarla usando la opción Cambiar contraseña:\n" +
                "  1) Ingresa a Naive‑Pay y ve a Iniciar sesión > Recuperar Acceso.\n" +
                "  2) Selecciona \"Olvidé mi contraseña\".\n" +
                "  3) Escribe tu correo y solicita el código de verificación.\n" +
                "  4) Revisa tu correo e ingresa el código.\n" +
                "  5) Crea una nueva contraseña para tu cuenta.\n" +
                "  6) Volveras a tener acceso a tu cuenta.\n\n" +
                "— Equipo Naive‑Pay"
        );
        mailSender.send(message);
    }
}