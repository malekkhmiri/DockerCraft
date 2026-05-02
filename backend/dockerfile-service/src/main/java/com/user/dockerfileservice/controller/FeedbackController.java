package com.user.dockerfileservice.controller;

import com.user.dockerfileservice.entity.DockerfileKnowledge;
import com.user.dockerfileservice.repository.KnowledgeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/dockerfiles/feedback")
@RequiredArgsConstructor
@Slf4j
public class FeedbackController {

    private final KnowledgeRepository knowledgeRepository;

    @PostMapping("/{id}/success")
    public ResponseEntity<Void> reportSuccess(@PathVariable Long id) {
        log.info("Reporting SUCCESS for generation ID: {}", id);
        updateStatus(id, DockerfileKnowledge.Status.SUCCESS, null);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/failure")
    public ResponseEntity<Void> reportFailure(@PathVariable Long id, @RequestBody String errorMessage) {
        log.warn("Reporting FAILURE for generation ID: {}. Error: {}", id, errorMessage);
        updateStatus(id, DockerfileKnowledge.Status.FAILURE, errorMessage);
        return ResponseEntity.ok().build();
    }

    private void updateStatus(Long id, DockerfileKnowledge.Status status, String error) {
        Optional<DockerfileKnowledge> k = knowledgeRepository.findById(id);
        k.ifPresent(knowledge -> {
            knowledge.setStatus(status);
            knowledge.setErrorMessage(error);
            knowledgeRepository.save(knowledge);
        });
    }
}
