package com.platform.dockerfileservice.exception;

/**
 * Exception thrown when Dockerfile generation fails.
 */
public class DockerfileGenerationException extends RuntimeException {
    public DockerfileGenerationException(String message) {
        super(message);
    }

    public DockerfileGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
