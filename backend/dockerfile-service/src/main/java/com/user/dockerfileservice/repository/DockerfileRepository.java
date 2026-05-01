package com.user.dockerfileservice.repository;

import com.user.dockerfileservice.entity.Dockerfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DockerfileRepository extends JpaRepository<Dockerfile, Long> {
    Optional<Dockerfile> findTopByProjectIdOrderByCreatedAtDesc(Long projectId);
    java.util.List<Dockerfile> findAllByProjectIdOrderByCreatedAtDesc(Long projectId);
    java.util.Optional<Dockerfile> findByProjectId(Long projectId);
}
