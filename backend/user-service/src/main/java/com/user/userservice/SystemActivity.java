package com.user.userservice;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "system_activities")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemActivity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String type; 
    
    @Column(length = 500)
    private String message;
    
    @Column(name = "activity_user") // 'user' est souvent réservé
    private String user;
    
    @Column(name = "activity_timestamp") // 'timestamp' peut être réservé
    private LocalDateTime timestamp;
}
