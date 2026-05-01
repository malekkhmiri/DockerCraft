package com.user.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RecentProjectDTO {
    private String id;
    private String name;
    private String user;
    private String language;
    private String status;
    private String uploadDate;
}
