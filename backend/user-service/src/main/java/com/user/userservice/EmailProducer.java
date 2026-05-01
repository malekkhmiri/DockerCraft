package com.user.userservice;

import com.user.userservice.dto.EmailRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailProducer {

    private final RabbitTemplate rabbitTemplate;

    public void queueSimpleEmail(String toEmail, String subject, String body) {
        EmailRequest request = EmailRequest.builder()
                .to(toEmail)
                .subject(subject)
                .body(body)
                .type("SIMPLE")
                .build();
        send(request);
    }

    public void queueVerificationCode(String toEmail, String code) {
        EmailRequest request = EmailRequest.builder()
                .to(toEmail)
                .type("VERIFICATION")
                .code(code)
                .build();
        send(request);
    }

    private void send(EmailRequest request) {
        log.info("📤 Mise en file d'attente de l'email pour {}", request.getTo());
        rabbitTemplate.convertAndSend(
                ActivityRabbitListener.EMAIL_EXCHANGE,
                ActivityRabbitListener.EMAIL_ROUTING_KEY,
                request);
    }
}
