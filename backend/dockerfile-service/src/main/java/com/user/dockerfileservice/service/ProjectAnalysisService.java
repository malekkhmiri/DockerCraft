package com.user.dockerfileservice.service;

import com.user.dockerfileservice.dto.AnalysisResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class ProjectAnalysisService {

    private static final Logger logger = LoggerFactory.getLogger(ProjectAnalysisService.class);

    public AnalysisResult analyze(String archivePath) {
        Path extracted = extractArchive(archivePath);
        try {
            return performAnalysis(extracted);
        } catch (Exception e) {
            logger.error("Analysis failed for {}: {}", archivePath, e.getMessage());
            return AnalysisResult.builder().language("UNKNOWN").build();
        }
    }

    private Path extractArchive(String archivePath) {
        try {
            Path targetDir = Files.createTempDirectory("docker-analysis-");
            try (ZipInputStream zis = new ZipInputStream(new FileInputStream(archivePath))) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    Path dest = targetDir.resolve(entry.getName()).normalize();
                    if (entry.isDirectory()) Files.createDirectories(dest);
                    else {
                        Files.createDirectories(dest.getParent());
                        Files.copy(zis, dest, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
            return targetDir;
        } catch (IOException e) { throw new RuntimeException(e); }
    }

    private AnalysisResult performAnalysis(Path root) throws Exception {
        Path realRoot = findProjectRoot(root);
        boolean isMaven = Files.exists(realRoot.resolve("pom.xml"));
        String buildContent = isMaven ? Files.readString(realRoot.resolve("pom.xml")) : 
                             (Files.exists(realRoot.resolve("build.gradle")) ? Files.readString(realRoot.resolve("build.gradle")) : "");
        String props = detectProperties(realRoot);

        // ✅ Détection robuste : évite les faux positifs (ex: dépendances commentées)
        boolean hasActuator = buildContent.matches("(?s).*<artifactId>\\s*spring-boot-starter-actuator\\s*</artifactId>.*") 
                           && !buildContent.matches("(?s).*<!--.*<artifactId>\\s*spring-boot-starter-actuator\\s*</artifactId>.*-->.*");

        AnalysisResult.AnalysisResultBuilder builder = AnalysisResult.builder()
                .language("JAVA")
                .os("linux")
                .buildTool(isMaven ? "maven" : "gradle")
                .framework("spring-boot");

        if (isMaven) {
            Document doc = parsePom(realRoot.resolve("pom.xml"));
            String javaVer = detectJavaVersion(doc);
            String sbVer = extractRegex(buildContent, "spring-boot-starter-parent.*?<version>([\\d.]+)</version>", "3.0.0");
            
            builder.javaVersion(javaVer)
                   .springBootVersion(sbVer)
                   .artifactId(getXmlTag(doc, "artifactId"))
                   .version(getXmlTag(doc, "version"))
                   .mavenImageRecommended(resolveMavenImage(javaVer, sbVer));
        }

        builder.port(extractPort(props))
               .hasActuator(hasActuator)
               .hasLombok(buildContent.contains("lombok"))
               .hasDevtools(buildContent.contains("spring-boot-devtools"))
               .hasSecurity(buildContent.contains("spring-boot-starter-security"))
               .hasValidation(buildContent.contains("spring-boot-starter-validation"))
               .hasThymeleaf(buildContent.contains("thymeleaf"))
               .hasCustomHealthEndpoint(detectCustomHealthEndpoint(realRoot))
               .databaseType(detectDatabase(buildContent))
               .databaseName(extractDbName(extractProperty(props, "spring.datasource.url")))
               .databaseUsername(extractProperty(props, "spring.datasource.username"))
               .ddlAuto(extractProperty(props, "spring.jpa.hibernate.ddl-auto"))
               .datasourceUrl(extractProperty(props, "spring.datasource.url"))
               .artifactVersion(extractProperty(props, "project.version"))
               .springProfile(extractProperty(props, "spring.profiles.active"))
               .springProfiles(parseProfiles(extractProperty(props, "spring.profiles.active")))
               .extraEnvVars(detectExtraEnvVars(props));

        AnalysisResult result = builder.build();
        result.setHealthEndpoint(resolveHealthEndpoint(result));
        
        if (result.getArtifactVersion() == null) result.setArtifactVersion("0.0.1-SNAPSHOT");
        result.setArtifactName(buildJarName(result));
        return result;
    }

    private void injectActuator(Path pomPath) {
        try {
            String content = Files.readString(pomPath);
            String dependency = "\n\t\t<dependency>\n" +
                                "\t\t\t<groupId>org.springframework.boot</groupId>\n" +
                                "\t\t\t<artifactId>spring-boot-starter-actuator</artifactId>\n" +
                                "\t\t</dependency>";
            String updated = content.replace("<dependencies>", "<dependencies>" + dependency);
            Files.writeString(pomPath, updated);
        } catch (IOException e) { logger.error("Actuator injection failed", e); }
    }

    private boolean detectCustomHealthEndpoint(Path root) {
        try (var walk = Files.walk(root)) {
            return walk.filter(p -> p.toString().endsWith(".java"))
                       .anyMatch(p -> {
                           try {
                               String content = Files.readString(p);
                               return content.contains("/api/health");
                           } catch (IOException e) { return false; }
                       });
        } catch (IOException e) { return false; }
    }

    private String resolveHealthEndpoint(AnalysisResult a) {
        if (a.isHasActuator()) return "/actuator/health";
        if (a.isHasCustomHealthEndpoint()) return "/api/health";
        if (a.isHasThymeleaf()) return "/";
        return null;
    }

    private String buildJarName(AnalysisResult a) {
        if (a.getArtifactId() != null && a.getArtifactVersion() != null)
            return a.getArtifactId() + "-" + a.getArtifactVersion() + ".jar";
        return "*.jar";
    }

    private Path findProjectRoot(Path start) throws IOException {
        try (var walk = Files.walk(start, 3)) {
            return walk.filter(p -> Files.exists(p.resolve("pom.xml")) || Files.exists(p.resolve("build.gradle")))
                       .findFirst().orElse(start);
        }
    }

    private Document parsePom(Path pom) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        return factory.newDocumentBuilder().parse(pom.toFile());
    }

    private String getXmlTag(Document doc, String tag) {
        NodeList nodes = doc.getElementsByTagName(tag);
        return nodes.getLength() > 0 ? nodes.item(0).getTextContent().trim() : null;
    }

    private String detectJavaVersion(Document pom) {
        for (String tag : List.of("java.version", "maven.compiler.release", "maven.compiler.source")) {
            String v = getXmlTag(pom, tag);
            if (v != null) return v;
        }
        return "17";
    }

    private List<String> detectExtraEnvVars(String props) {
        if (props == null) return List.of();
        List<String> extra = new ArrayList<>();
        if (props.contains("jwt.secret")) extra.add("JWT_SECRET");
        if (props.contains("mail.password")) extra.add("SPRING_MAIL_PASSWORD");
        return extra;
    }

    private List<String> parseProfiles(String p) {
        return (p == null) ? List.of("prod") : Arrays.asList(p.split(","));
    }

    private String detectDatabase(String build) {
        if (build.contains("postgresql")) return "postgresql";
        if (build.contains("mysql")) return "mysql";
        if (build.contains("mongodb")) return "mongodb";
        return null;
    }

    private String detectProperties(Path root) {
        for (String f : List.of("src/main/resources/application.properties", "src/main/resources/application.yml")) {
            Path p = root.resolve(f);
            if (Files.exists(p)) {
                try { return Files.readString(p); } catch (IOException ignored) {}
            }
        }
        return "";
    }

    private int extractPort(String props) {
        String p = extractRegex(props, "server.port\\s*[:=]\\s*(\\d+)", "8080");
        return Integer.parseInt(p);
    }

    private String extractProperty(String props, String key) {
        return extractRegex(props, key + "\\s*[:=]\\s*([^\\n\\r]+)", null);
    }

    private String extractDbName(String url) {
        if (url == null) return "app_db";
        return extractRegex(url, ".*/([^?]+)", "app_db");
    }

    private String extractRegex(String src, String regex, String fallback) {
        Matcher m = Pattern.compile(regex, Pattern.DOTALL).matcher(src);
        return m.find() ? m.group(1).trim() : fallback;
    }

    private String resolveMavenImage(String jv, String sv) {
        return "maven:3.9.6-eclipse-temurin-" + jv + "-alpine";
    }
}
