package za.ac.cput.medisnyc.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class PasswordResetEmailService {

    private static final Logger logger = LoggerFactory.getLogger(PasswordResetEmailService.class);

    private final JavaMailSender mailSender;
    private final String from;
    private final String frontendUrl;

    public PasswordResetEmailService(JavaMailSender mailSender,
                                     @Value("${app.mail.from}") String from,
                                     @Value("${app.frontend-url}") String frontendUrl) {
        this.mailSender = mailSender;
        this.from = from;
        this.frontendUrl = frontendUrl;
    }

    public void send(String email, String token) {
        String resetUrl = frontendUrl + "/reset-password?token=" + token;
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject("MediSync password reset");
        message.setText("Use this link to reset your MediSync password within one hour:\n\n"
                + resetUrl + "\n\nIf you did not request this, ignore this email.");
        try {
            mailSender.send(message);
        } catch (MailException exception) {
            logger.error("Password reset email delivery failed ({})", exception.getClass().getSimpleName());
        }
    }
}
