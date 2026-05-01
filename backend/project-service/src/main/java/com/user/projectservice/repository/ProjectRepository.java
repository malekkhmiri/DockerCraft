package com.user.projectservice.repository;

import com.user.projectservice.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByUserEmail(String userEmail);
    long countByUserEmail(String userEmail);
}
