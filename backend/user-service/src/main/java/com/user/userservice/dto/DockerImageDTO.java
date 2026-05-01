package com.user.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DockerImageDTO {
    private String name;
    private String tag;
    private String projectName;
    private String registryUrl;
}
