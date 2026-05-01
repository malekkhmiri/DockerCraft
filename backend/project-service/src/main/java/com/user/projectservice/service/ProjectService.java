package com.user.projectservice.service;

import com.user.projectservice.dto.ProjectResponse;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface ProjectService {
    ProjectResponse uploadProject(String name, String language, String userEmail, String username, MultipartFile file);
    List<ProjectResponse> getAllProjects();
    ProjectResponse getProjectById(Long id);
    void deleteProject(Long id);
    long countProjects();
    List<ProjectResponse> getProjectsByUserEmail(String userEmail);
    long countProjectsByUserEmail(String userEmail);
    void updateStatus(Long id, String status);
}
