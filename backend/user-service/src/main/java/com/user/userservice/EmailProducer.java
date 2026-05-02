package com.user.userservice;

import org.springframework.stereotype.Service;

@Service
public class EmailProducer {
    
    public void sendVerificationEmail(String email, String code) {
        // Logique RabbitMQ désactivée
        System.out.println("EmailProducer simulé: Code " + code + " pour " + email);
    }
    
    public void sendPasswordResetEmail(String email, String code) {
        // Logique RabbitMQ désactivée
        System.out.println("EmailProducer simulé: Reset " + code + " pour " + email);
    }
}
