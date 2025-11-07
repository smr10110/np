package cl.ufro.dci.naivepayapi.registro.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

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
}