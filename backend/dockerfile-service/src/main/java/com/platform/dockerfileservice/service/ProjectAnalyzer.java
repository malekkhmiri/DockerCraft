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
 * Service responsible for analyzing project structure with high precision.
 */
@Service
public class ProjectAnalyzer {

    public ProjectContext analyze(Path projectRoot) {
        ProjectContext.ProjectContextBuilder builder = ProjectContext.builder();
        
        // Cible Linux par défaut pour les conteneurs
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
                       .version(detectJavaVersion(content))
                       .artifactName(detectArtifactName(content));

                detectHealthEndpoint(projectRoot).ifPresent(builder::healthEndpoint);

            } catch (IOException e) {
                builder.language("java").buildTool("maven")
                       .framework("java-plain").version("17")
                       .artifactName("app.jar");
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
        if (pomContent.contains("com.mysql") || 
            pomContent.contains("mysql-connector"))  return "mysql";
        if (pomContent.contains("org.postgresql"))   return "postgresql";
        if (pomContent.contains("com.h2database"))   return "h2";
        return null;
    }

    private String detectArtifactName(String pomContent) {
        // 1. Chercher <finalName> explicite
        Matcher finalName = Pattern.compile("<finalName>(.*?)</finalName>").matcher(pomContent);
        if (finalName.find()) return finalName.group(1) + ".jar";

        // 2. Sinon construire depuis artifactId + version (en isolant le header avant dependencies)
        int depsIdx = pomContent.indexOf("<dependencies>");
        String header = depsIdx > 0 ? pomContent.substring(0, depsIdx) : pomContent;

        Matcher artifactId = Pattern.compile("<artifactId>(.*?)</artifactId>").matcher(header);
        String id = "app";
        int idEnd = 0;
        while (artifactId.find()) {
            String candidate = artifactId.group(1);
            if (!candidate.contains("parent") && !candidate.contains("starter")) {
                id = candidate;
                idEnd = artifactId.end();
                break;
            }
        }
        
        // Chercher la version APRÈS l'artifactId du projet pour éviter de prendre celle du parent
        String afterId = idEnd > 0 ? header.substring(idEnd) : header;
        Matcher version = Pattern.compile("<version>(.*?)</version>").matcher(afterId);
        String ver = version.find() ? version.group(1) : "";
        
        return ver.isBlank() ? id + ".jar" : id + "-" + ver + ".jar";
    }

    private void detectPort(Path projectRoot, ProjectContext.ProjectContextBuilder builder) {
        Path base = projectRoot.resolve("src/main/resources");
        int port = readPortFromProperties(base.resolve("application.properties"))
                .orElseGet(() -> readPortFromYaml(base.resolve("application.yml"))
                .orElseGet(() -> readPortFromProperties(base.resolve("application-prod.properties"))
                .orElse(8080)));
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

    private Optional<Integer> readPortFromYaml(Path path) {
        if (!Files.exists(path)) return Optional.empty();
        try {
            String content = Files.readString(path);
            Matcher block = Pattern.compile("server:\\s*\\n\\s*port:\\s*(\\d+)").matcher(content);
            if (block.find()) return Optional.of(Integer.parseInt(block.group(1)));
            
            Matcher inline = Pattern.compile("server\\.port:\\s*(\\d+)").matcher(content);
            if (inline.find()) return Optional.of(Integer.parseInt(inline.group(1)));
            
            return Optional.empty();
        } catch (IOException | NumberFormatException e) {
            return Optional.empty();
        }
    }

    private Optional<String> detectHealthEndpoint(Path projectRoot) {
        Path srcRoot = projectRoot.resolve("src/main/java");
        if (!Files.exists(srcRoot)) return Optional.empty();
        // Limite de profondeur à 15 pour éviter de scanner inutilement des modules profonds
        try (Stream<Path> stream = Files.walk(srcRoot, 15)) {
            return stream
                .filter(p -> p.toString().endsWith(".java"))
                .map(p -> { 
                    try { return Files.readString(p); }
                    catch (IOException e) { return ""; } 
                })
                .flatMap(content -> extractGetRoutes(content).stream())
                .filter(route -> !route.contains("{"))
                .findFirst();
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private List<String> extractGetRoutes(String javaSource) {
        List<String> routes = new ArrayList<>();
        String classPrefix = extractClassPrefix(javaSource);

        // 1. @GetMapping avec route explicite
        Matcher m = Pattern.compile(
            "@GetMapping\\((?:(?:value|path)\\s*=\\s*)?\"([^\"]+)\"\\)"
        ).matcher(javaSource);
        while (m.find()) {
            String route = classPrefix + m.group(1);
            if (!route.startsWith("/")) route = "/" + route;
            routes.add(route.replace("//", "/"));
        }

        // 2. @GetMapping sans argument (cas local via regex pour éviter les faux positifs globaux)
        Matcher noArg = Pattern.compile("@GetMapping\\s*\\n").matcher(javaSource);
        if (noArg.find() && !classPrefix.isBlank()) {
            String route = classPrefix.startsWith("/") ? classPrefix : "/" + classPrefix;
            routes.add(route.replace("//", "/"));
        }

        return routes;
    }

    private String extractClassPrefix(String javaSource) {
        Matcher m = Pattern.compile(
            "@RequestMapping\\([^)]*(?:value|path)\\s*=\\s*\"([^\"]+)\"[^)]*\\)"
        ).matcher(javaSource);
        if (m.find()) return m.group(1);
        
        m = Pattern.compile("@RequestMapping\\(\"([^\"]+)\"\\)").matcher(javaSource);
        return m.find() ? m.group(1) : "";
    }
}
