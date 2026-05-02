package com.user.userservice;

import org.springframework.stereotype.Service;

@Service
public class EmailProducer {
    
    public void queueVerificationCode(String email, String code) {
        // Logique RabbitMQ désactivée
        System.out.println("EmailProducer simulé: Code " + code + " pour " + email);
    }
    
    public void queueSimpleEmail(String email, String subject, String body) {
        // Logique RabbitMQ désactivée
        System.out.println("EmailProducer simulé: Sujet '" + subject + "' pour " + email);
    }
}
