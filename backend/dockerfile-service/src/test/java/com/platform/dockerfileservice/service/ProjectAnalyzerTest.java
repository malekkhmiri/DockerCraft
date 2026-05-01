package com.platform.dockerfileservice.service;

import com.platform.dockerfileservice.model.ProjectContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ProjectAnalyzerTest {

    private final ProjectAnalyzer analyzer = new ProjectAnalyzer();

    @TempDir
    Path tempDir;

    @Test
    void testJavaDetection() throws IOException {
        Files.writeString(tempDir.resolve("pom.xml"), 
            "<project><properties><java.version>17</java.version></properties></project>");
        
        ProjectContext ctx = analyzer.analyze(tempDir);
        
        assertEquals("java", ctx.getLanguage());
        assertEquals("17", ctx.getVersion());
        assertEquals("maven", ctx.getBuildTool());
    }

    @Test
    void testPythonWindowsDetection() throws IOException {
        Files.createFile(tempDir.resolve("requirements.txt"));
        Files.createFile(tempDir.resolve("run.ps1"));
        
        ProjectContext ctx = analyzer.analyze(tempDir);
        
        assertEquals("python", ctx.getLanguage());
        assertEquals(ProjectContext.TargetOS.WINDOWS, ctx.getTargetOS());
    }

    @Test
    void testPortDetection() throws IOException {
        Files.createFile(tempDir.resolve("pom.xml"));
        Path resourceDir = tempDir.resolve("src/main/resources");
        Files.createDirectories(resourceDir);
        Files.writeString(resourceDir.resolve("application.properties"), "server.port=9090");
        
        ProjectContext ctx = analyzer.analyze(tempDir);
        
        assertEquals(9090, ctx.getPort());
    }

    @Test
    void testFilesToCopy() throws IOException {
        Files.createFile(tempDir.resolve("pom.xml"));
        Files.createFile(tempDir.resolve("script.sh"));
        Files.createFile(tempDir.resolve("App.java"));
        Files.createDirectories(tempDir.resolve("target"));
        Files.createFile(tempDir.resolve("target/ignored.jar"));
        
        ProjectContext ctx = analyzer.analyze(tempDir);
        
        assertTrue(ctx.getFilesToCopy().contains("script.sh"));
        assertTrue(ctx.getFilesToCopy().contains("App.java"));
        assertFalse(ctx.getFilesToCopy().contains("ignored.jar"));
    }
}
