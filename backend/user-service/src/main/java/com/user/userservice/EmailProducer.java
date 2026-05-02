package com.user.userservice;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailProducer {
    
    private final EmailService emailService;
    
    public void queueVerificationCode(String email, String code) {
        emailService.sendVerificationCode(email, code);
    }
    
    public void queueSimpleEmail(String email, String subject, String body) {
        emailService.sendSimpleEmail(email, subject, body);
    }
}
