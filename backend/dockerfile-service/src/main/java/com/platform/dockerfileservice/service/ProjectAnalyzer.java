package com.platform.dockerfileservice.service;

import com.platform.dockerfileservice.model.ProjectContext;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
        
        // Les conteneurs ciblent toujours Linux — évite les faux positifs des fichiers .bat/.cmd
        builder.targetOS(ProjectContext.TargetOS.LINUX);

        detectLanguageAndBuildInfo(projectRoot, builder);
        detectPort(projectRoot, builder);

        return builder.build();
    }

    private void detectLanguageAndBuildInfo(Path projectRoot,
                                             ProjectContext.ProjectContextBuilder builder) {
        Path pom = projectRoot.resolve("pom.xml");
        if (Files.exists(pom)) {
            try {
                String content = Files.readString(pom);
                builder.language("java")
                       .buildTool("maven")
                       .framework(detectFramework(content))
                       .databaseType(detectDatabase(content))
                       .version(detectJavaVersion(content));

                detectHealthEndpoint(projectRoot)
                        .ifPresent(builder::healthEndpoint);

            } catch (IOException e) {
                builder.language("java").buildTool("maven")
                       .framework("java-plain").version("17");
            }
        } else if (Files.exists(projectRoot.resolve("requirements.txt"))) {
            builder.language("python").version("3.9");
        } else if (Files.exists(projectRoot.resolve("package.json"))) {
            builder.language("nodejs").version("18");
        } else if (Files.exists(projectRoot.resolve("go.mod"))) {
            builder.language("go").version("1.21");
        }
    }

    private String detectJavaVersion(String pomContent) {
        Matcher m = Pattern.compile("<java\\.version>(.*?)</java\\.version>").matcher(pomContent);
        return m.find() ? m.group(1) : "17";
    }

    private String detectFramework(String pomContent) {
        if (pomContent.contains("spring-boot-starter")) return "spring-boot";
        if (pomContent.contains("quarkus-universe"))    return "quarkus";
        return "java-plain";
    }

    private String detectDatabase(String pomContent) {
        if (pomContent.contains("mysql-connector"))  return "mysql";
        if (pomContent.contains("postgresql"))       return "postgresql";
        if (pomContent.contains("com.h2database"))   return "h2";
        return null;
    }

    private void detectPort(Path projectRoot, ProjectContext.ProjectContextBuilder builder) {
        // Essaye application.properties puis application-prod.properties
        int port = readPortFromProperties(
                    projectRoot.resolve("src/main/resources/application.properties"))
                .orElseGet(() -> readPortFromProperties(
                    projectRoot.resolve("src/main/resources/application-prod.properties"))
                .orElse(8080));
        builder.port(port);
    }

    private Optional<Integer> readPortFromProperties(Path path) {
        if (!Files.exists(path)) return Optional.empty();
        try (var is = Files.newInputStream(path)) {
            Properties props = new Properties();
            props.load(is);
            String portStr = props.getProperty("server.port", "8080");
            return Optional.of(Integer.parseInt(portStr));
        } catch (IOException | NumberFormatException e) {
            return Optional.empty();
        }
    }

    private Optional<String> detectHealthEndpoint(Path projectRoot) {
        Path srcRoot = projectRoot.resolve("src/main/java");
        if (!Files.exists(srcRoot)) return Optional.empty();
        try (Stream<Path> stream = Files.walk(srcRoot)) {
            return stream
                .filter(p -> p.toString().endsWith(".java"))
                .map(p -> { 
                    try { return Files.readString(p); }
                    catch (IOException e) { return ""; } 
                })
                .flatMap(content -> extractGetRoutes(content).stream())
                .filter(route -> !route.contains("{")) // Exclure les PathVariables
                .findFirst();
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private List<String> extractGetRoutes(String content) {
        List<String> routes = new ArrayList<>();
        Matcher m = Pattern.compile("@GetMapping\\(\"([^\"]+)\"\\)").matcher(content);
        while (m.find()) routes.add(m.group(1));
        return routes;
    }
}
