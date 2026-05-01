package com.user.projectservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PipelineFinishedEvent implements Serializable {
    private Long projectId;
    private String status;
    private String executionTime;
}
