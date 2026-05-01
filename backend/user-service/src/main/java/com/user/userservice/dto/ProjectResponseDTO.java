package com.user.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProjectResponseDTO {
    private Long id;
    private String name;
    
    private java.time.LocalDateTime createdAt;
    
    private String status; // On laisse en String ici car on reçoit du JSON
    private String language;
    private String userEmail;
    private String username;
}
