package com.user.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailRequest implements Serializable {
    private String to;
    private String subject;
    private String body;
    private String type; // e.g. "VERIFICATION", "SIMPLE"
    private String code; // for verification
}
