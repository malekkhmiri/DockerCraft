package com.platform.dockerfileservice.repository;

import com.platform.dockerfileservice.model.DockerfileKnowledge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * JPA repository for the RAG-lite knowledge base.
 * Stores successful and failed Dockerfile generation attempts
 * to improve future LLM prompt context.
 */
@Repository
public interface KnowledgeRepository extends JpaRepository<DockerfileKnowledge, Long> {

    /**
     * Returns the most recent generation attempts for a given language and status.
     * Used to build RAG success/failure context for the LLM prompt.
     */
    List<DockerfileKnowledge> findTop3ByLanguageAndStatusOrderByCreatedAtDesc(
            String language,
            DockerfileKnowledge.Status status
    );
}
