package com.platform.dockerfileservice.service;

import com.platform.dockerfileservice.model.ProjectContext;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Service responsible for analyzing project structure to extract metadata.
 */
@Service
public class ProjectAnalyzer {

    /**
     * Analyzes the project at the given root path.
     *
     * @param projectRoot the root path of the project
     * @return the analyzed ProjectContext
     */
    public ProjectContext analyze(Path projectRoot) {
        ProjectContext.ProjectContextBuilder builder = ProjectContext.builder();

        detectLanguageAndVersion(projectRoot, builder);
        detectOS(projectRoot, builder);
        detectPort(projectRoot, builder);
        listFilesToCopy(projectRoot, builder);

        return builder.build();
    }

    private void detectLanguageAndVersion(Path projectRoot, ProjectContext.ProjectContextBuilder builder) {
        if (Files.exists(projectRoot.resolve("pom.xml"))) {
            builder.language("java");
            builder.buildTool("maven");
            try {
                String content = Files.readString(projectRoot.resolve("pom.xml"));
                Pattern pattern = Pattern.compile("<java\\.version>(.*?)</java\\.version>");
                Matcher matcher = pattern.matcher(content);
                if (matcher.find()) {
                    builder.version(matcher.group(1));
                } else {
                    builder.version("17"); // Default
                }
            } catch (IOException e) {
                builder.version("17");
            }
        } else if (Files.exists(projectRoot.resolve("requirements.txt"))) {
            builder.language("python");
            builder.version("3.9"); // Default for python
        } else if (Files.exists(projectRoot.resolve("package.json"))) {
            builder.language("nodejs");
            builder.version("18"); // Default for node
        } else if (Files.exists(projectRoot.resolve("go.mod"))) {
            builder.language("go");
            builder.version("1.21"); // Default for go
        }
    }

    private void detectOS(Path projectRoot, ProjectContext.ProjectContextBuilder builder) {
        try (Stream<Path> stream = Files.list(projectRoot)) {
            boolean hasWindowsFiles = stream.anyMatch(path -> {
                String name = path.getFileName().toString().toLowerCase();
                return name.endsWith(".bat") || name.endsWith(".ps1") || name.endsWith(".cmd");
            });
            builder.targetOS(hasWindowsFiles ? ProjectContext.TargetOS.WINDOWS : ProjectContext.TargetOS.LINUX);
        } catch (IOException e) {
            builder.targetOS(ProjectContext.TargetOS.LINUX);
        }
    }

    private void detectPort(Path projectRoot, ProjectContext.ProjectContextBuilder builder) {
        Path propsPath = projectRoot.resolve("src/main/resources/application.properties");
        if (Files.exists(propsPath)) {
            try {
                Properties props = new Properties();
                props.load(Files.newInputStream(propsPath));
                String port = props.getProperty("server.port", "8080");
                builder.port(Integer.parseInt(port));
                return;
            } catch (IOException | NumberFormatException ignored) {}
        }
        builder.port(8080);
    }

    private void listFilesToCopy(Path projectRoot, ProjectContext.ProjectContextBuilder builder) {
        List<String> files = new ArrayList<>();
        try (Stream<Path> stream = Files.list(projectRoot)) {
            files = stream.filter(path -> !Files.isDirectory(path))
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(".py") || name.endsWith(".java") || name.endsWith(".js") || name.endsWith(".sh"))
                    .collect(Collectors.toList());
        } catch (IOException ignored) {}
        builder.filesToCopy(files);
    }
}
