package com.user.dockerfileservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProjectUploadedEvent implements Serializable {
    private Long projectId;
    private String userEmail;
    private String name;
    private String language;
}
