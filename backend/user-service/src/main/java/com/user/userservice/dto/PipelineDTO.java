package com.user.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PipelineDTO {
    private String id;
    private String projectName;
    private String status;
    private String duration;
    private String date;
}
