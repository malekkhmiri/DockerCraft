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
    private boolean isMultiModule = false;
    private List<String> modules = new ArrayList<>();

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
        // ✅ FIX: scan from the full extracted archive root, not just the pom root
        // This finds properties deep in sub-modules (e.g. mall-admin/src/main/resources/application-prod.yml)
        String props = detectProperties(root);

        // ✅ Détection robuste : évite les faux positifs (ex: dépendances commentées)
        boolean hasActuator = buildContent.matches("(?s).*<artifactId>\\s*spring-boot-starter-actuator\\s*</artifactId>.*") 
                           && !buildContent.matches("(?s).*<!--.*<artifactId>\\s*spring-boot-starter-actuator\\s*</artifactId>.*-->.*");

        String framework = "java-plain";
        if (buildContent.contains("spring-boot")) {
            framework = "spring-boot";
        } else if (buildContent.contains("quarkus")) {
            framework = "quarkus";
        } else if (buildContent.contains("micronaut")) {
            framework = "micronaut";
        }

        AnalysisResult.AnalysisResultBuilder builder = AnalysisResult.builder()
                .language("JAVA")
                .os("linux")
                .buildTool(isMaven ? "maven" : "gradle")
                .framework(framework);

        if (isMaven) {
            Document doc = parsePom(realRoot.resolve("pom.xml"));
            this.modules = extractModules(doc);
            this.isMultiModule = !modules.isEmpty();
            if (isMultiModule) logger.info("Multi-module project detected: {}", modules);

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
               .databaseName(extractDbNameFromProps(props))
               .databaseUsername(extractBestProperty(props, "spring.datasource.username", "username"))
               .ddlAuto(extractProperty(props, "spring.jpa.hibernate.ddl-auto"))
               .datasourceUrl(extractBestProperty(props, "spring.datasource.url", "url"))
               .artifactVersion(extractProperty(props, "project.version"))
               .springProfile(extractProperty(props, "spring.profiles.active"))
               .springProfiles(parseProfiles(extractProperty(props, "spring.profiles.active")))
               .extraEnvVars(detectExtraEnvVars(props))
               .isMultiModule(isMultiModule)
               .modules(modules);

        AnalysisResult result = builder.build();
        result.setHealthEndpoint(resolveHealthEndpoint(result, realRoot));
        
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

    private String resolveHealthEndpoint(AnalysisResult a, Path root) {
        if (!"spring-boot".equals(a.getFramework())) {
            return null; // Pas de healthcheck par défaut pour le Java pur
        }
        if (a.isHasActuator()) return "/actuator/health";
        if (a.isHasCustomHealthEndpoint()) return "/api/health";
        
        // HEURISTIQUE : Scanner les fichiers Java pour trouver une route @GetMapping
        String detectedRoute = scanForApiRoute(root);
        if (detectedRoute != null) return detectedRoute;
        
        if (a.isHasThymeleaf()) return "/";
        return "/";
    }

    private String scanForApiRoute(Path root) {
        try (var walk = Files.walk(root)) {
            return walk.filter(p -> p.toString().endsWith(".java"))
                       .map(p -> {
                           try {
                               String content = Files.readString(p);
                               if (content.contains("@RestController") || content.contains("@Controller")) {
                                   // 1. Chercher le prefixe de classe
                                   String classPath = "";
                                   Matcher mClass = Pattern.compile("@RequestMapping\\s*\\(\\s*\"([^\"]+)\"").matcher(content);
                                   if (mClass.find()) classPath = mClass.group(1);
                                   
                                   // 2. Chercher la première méthode GET
                                   Matcher mMethod = Pattern.compile("@GetMapping\\s*\\(\\s*\"([^\"]+)\"").matcher(content);
                                   if (mMethod.find()) return (classPath + mMethod.group(1)).replace("//", "/");
                                   
                                   if (!classPath.isEmpty()) return classPath;
                               }
                               return null;
                           } catch (IOException e) { return null; }
                       })
                       .filter(Objects::nonNull)
                       .findFirst().orElse(null);
        } catch (IOException e) { return null; }
    }

    private String buildJarName(AnalysisResult a) {
        if (a.getArtifactId() != null && a.getArtifactVersion() != null) {
            String name = a.getArtifactId() + "-" + a.getArtifactVersion() + ".jar";
            // If DB name is still null or generic, use artifactId as a strong hint
            if (a.getDatabaseName() == null || a.getDatabaseName().equals("app_db")) {
                a.setDatabaseName(a.getArtifactId());
                logger.info("Using artifactId '{}' as DB name hint", a.getArtifactId());
            }
            return name;
        }
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

    private List<String> extractModules(Document doc) {
        List<String> res = new ArrayList<>();
        NodeList nodes = doc.getElementsByTagName("module");
        for (int i = 0; i < nodes.getLength(); i++) {
            res.add(nodes.item(i).getTextContent().trim());
        }
        return res;
    }

    private String detectJavaVersion(Document pom) {
        for (String tag : List.of("java.version", "maven.compiler.release", "maven.compiler.source")) {
            String v = getXmlTag(pom, tag);
            if (v != null) return v.trim();
        }
        return "21"; // Default modern
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
        StringBuilder baseProps = new StringBuilder();
        StringBuilder prodProps = new StringBuilder();
        try (var walk = Files.walk(root)) {
            walk.filter(Files::isRegularFile)
                .filter(p -> {
                    String name = p.getFileName().toString();
                    return name.startsWith("application") && (name.endsWith(".yml") || name.endsWith(".properties"));
                }).forEach(p -> {
                    try {
                        String name = p.getFileName().toString();
                        String content = Files.readString(p);
                        String header = "\n--- " + root.relativize(p) + " ---\n";
                        // ✅ Prioritize -prod files — they contain real credentials
                        if (name.contains("-prod") || name.contains("-production")) {
                            logger.info("Found PROD properties: {}", p);
                            prodProps.append(header).append(content);
                        } else {
                            logger.info("Found BASE properties: {}", p);
                            baseProps.append(header).append(content);
                        }
                    } catch (IOException ignored) {}
                });
        } catch (IOException e) {
            logger.error("Error walking for properties: {}", e.getMessage());
        }
        // prod files come first so their values win during extraction
        String result = prodProps + "\n" + baseProps;
        logger.info("Properties detected ({} chars). Prod files: {}", result.length(), prodProps.length() > 0 ? "YES" : "NO");
        return result;
    }

    private int extractPort(String props) {
        String p = extractRegex(props, "server.port\\s*[:=]\\s*(\\d+)", "8080");
        return Integer.parseInt(p);
    }

    private String extractProperty(String props, String key) {
        // Try flat key first (e.g. spring.datasource.url: value)
        String flatRegex = "(?m)^\\s*" + key.replace(".", "\\.") + "\\s*[:=]\\s*([^\\n\\r#]+)";
        String val = extractRegex(props, flatRegex, null);
        if (val != null) return val.trim();

        // Try nested YAML last-segment (e.g. for spring.datasource.url, look for "url: ...")
        String[] parts = key.split("\\.");
        String lastPart = parts[parts.length - 1];
        String nestedRegex = "(?m)^\\s*" + lastPart + "\\s*:\\s*([^\\n\\r#]+)";
        val = extractRegex(props, nestedRegex, null);
        return val != null ? val.trim() : null;
    }

    /**
     * Tries multiple key patterns and returns the first non-null result.
     * Used to extract values that may be written with their full path or just the last segment.
     */
    private String extractBestProperty(String props, String fullKey, String shortKey) {
        String val = extractProperty(props, fullKey);
        if (val != null) {
            logger.info("Extracted '{}' = '{}'", fullKey, val);
            return val;
        }
        val = extractProperty(props, shortKey);
        if (val != null) logger.info("Extracted '{}' via shortKey '{}' = '{}'", fullKey, shortKey, val);
        return val;
    }

    private String extractDbNameFromProps(String props) {
        // 1. Try standard keys first
        String url = extractBestProperty(props, "spring.datasource.url", "url");
        if (url != null && !url.contains("[") && !url.contains("${")) {
            Matcher m = Pattern.compile("://[^/]+/([^?\\s]+)").matcher(url);
            if (m.find()) {
                String name = m.group(1).trim();
                logger.info("Detected DB name from primary URL: '{}'", name);
                return name;
            }
        }

        // 2. Fallback: Search for any JDBC-like string in the whole props blob
        Matcher m2 = Pattern.compile("jdbc:[a-z]+://[^/]+/([^?\\s;]+)").matcher(props);
        while (m2.find()) {
            String name = m2.group(1).trim();
            if (!name.equals("app_db") && !name.equals("test") && !name.contains("$")) {
                logger.info("Detected DB name from secondary JDBC string: '{}'", name);
                return name;
            }
        }

        // 3. Fallback: Search for spring.datasource.name or similar
        String nameProp = extractBestProperty(props, "spring.datasource.name", "database-name");
        if (nameProp != null) return nameProp;

        return null;
    }

    /** @deprecated Use extractDbNameFromProps instead */
    private String extractDbName(String url) {
        if (url == null) return null;
        Matcher m = Pattern.compile("://[^/]+/([^?\\s]+)").matcher(url);
        return m.find() ? m.group(1).trim() : null;
    }

    private String extractRegex(String src, String regex, String fallback) {
        Matcher m = Pattern.compile(regex, Pattern.DOTALL).matcher(src);
        return m.find() ? m.group(1).trim() : fallback;
    }

    private String resolveMavenImage(String jv, String sv) {
        String mappedV = "1.8".equals(jv) ? "8" : jv;
        return "maven:3.9.6-eclipse-temurin-" + mappedV + "-alpine";
    }
}
