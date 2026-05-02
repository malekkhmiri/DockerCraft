package com.user.userservice;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendSimpleEmail(String toEmail, String subject, String body) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, "DockerGeneration");
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(body, false);
            mailSender.send(message);
            logger.info("Email envoyé à : {} (sujet: {})", toEmail, subject);
        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            logger.error("Échec de l'envoi de l'email à {} : {}", toEmail, e.getMessage());
        }
    }

    public void sendVerificationCode(String toEmail, String code) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, "DockerGeneration");
            helper.setTo(toEmail);
            helper.setSubject("🔑 Votre code de vérification - DockerGeneration");
            helper.setText(buildEmailHtml(code), true);

            mailSender.send(message);
            logger.info("Code de vérification envoyé à : {}", toEmail);
        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            logger.error("Échec de l'envoi de l'email à {} : {}", toEmail, e.getMessage());
        }
    }

    private String buildEmailHtml(String code) {
        return """
            <!DOCTYPE html>
            <html>
            <body style="font-family: 'Segoe UI', Arial, sans-serif; background: #0f172a; margin: 0; padding: 40px;">
              <div style="max-width: 480px; margin: 0 auto; background: #1e293b; border-radius: 16px; padding: 40px; border: 1px solid rgba(99,102,241,0.3);">
                <div style="text-align: center; margin-bottom: 32px;">
                  <h1 style="color: #818cf8; font-size: 28px; margin: 0;">DockerGeneration</h1>
                  <p style="color: #64748b; font-size: 14px; margin-top: 4px;">Plateforme de déploiement automatique</p>
                </div>
                
                <h2 style="color: #f1f5f9; font-size: 20px; margin-bottom: 8px;">Vérification de votre email</h2>
                <p style="color: #94a3b8; font-size: 14px; line-height: 1.6; margin-bottom: 32px;">
                  Utilisez le code ci-dessous pour vérifier votre adresse email et accéder à la plateforme.
                  Ce code expire dans <strong style="color: #fbbf24;">10 minutes</strong>.
                </p>
                
                <div style="background: #0f172a; border: 2px solid #6366f1; border-radius: 12px; padding: 24px; text-align: center; margin-bottom: 32px;">
                  <span style="font-size: 42px; font-weight: 800; letter-spacing: 12px; color: #818cf8; font-family: monospace;">
                    %s
                  </span>
                </div>
                
                <p style="color: #475569; font-size: 12px; text-align: center; margin: 0;">
                  Si vous n'avez pas demandé ce code, ignorez cet email.<br>
                  Ne partagez jamais ce code avec quelqu'un.
                </p>
              </div>
            </body>
            </html>
            """.formatted(code);
    }
}
