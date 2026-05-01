package com.platform.dockerfileservice.service;

import com.platform.dockerfileservice.exception.DockerfileGenerationException;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

@WireMockTest
class OllamaServiceTest {

    private final OllamaService ollamaService = new OllamaService();

    @BeforeEach
    void setUp(WireMockRuntimeInfo wmInfo) {
        ReflectionTestUtils.setField(ollamaService, "ollamaUrl", wmInfo.getHttpBaseUrl());
        ReflectionTestUtils.setField(ollamaService, "model", "qwen2.5-coder:7b-instruct");
    }

    @Test
    void testValidDockerfilePasses() {
        stubFor(post(urlEqualTo("/api/generate"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"response\": \"FROM alpine\\nCOPY . /app\\nCMD [\\\"ls\\\"]\"}")));

        String result = ollamaService.generateDockerfile("test prompt", null, null);
        
        assertNotNull(result);
        assertTrue(result.contains("FROM"));
    }

    @Test
    void testInvalidDockerfileThrowsException() {
        stubFor(post(urlEqualTo("/api/generate"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"response\": \"Hello this is not a dockerfile\"}")));

        assertThrows(DockerfileGenerationException.class, () -> 
            ollamaService.generateDockerfile("test prompt", null, null)
        );
    }

    @Test
    void testHttpErrorThrowsException() {
        stubFor(post(urlEqualTo("/api/generate"))
                .willReturn(aResponse().withStatus(500)));

        assertThrows(DockerfileGenerationException.class, () -> 
            ollamaService.generateDockerfile("test prompt", null, null)
        );
    }
}
