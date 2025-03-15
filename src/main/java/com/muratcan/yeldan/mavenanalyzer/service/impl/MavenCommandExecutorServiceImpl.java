package com.muratcan.yeldan.mavenanalyzer.service.impl;

import com.muratcan.yeldan.mavenanalyzer.service.MavenCommandExecutorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class MavenCommandExecutorServiceImpl implements MavenCommandExecutorService {

    private static final Pattern PROPERTY_PATTERN = Pattern.compile("<(.+)>(.+)</(.+)>");
    // Cache to store project properties
    private final Map<String, Map<String, String>> projectPropertiesCache = new HashMap<>();

    @Override
    public Map<String, String> getProjectProperties(String pomDirectory) {
        // Return cached properties if available
        if (projectPropertiesCache.containsKey(pomDirectory)) {
            return projectPropertiesCache.get(pomDirectory);
        }

        Map<String, String> properties = new HashMap<>();

        if (!isValidPomDirectory(pomDirectory)) {
            return properties;
        }

        try {
            // Extract project properties from Maven
            properties = extractMavenProjectProperties(pomDirectory);

            // Extract dependency versions for more complete resolution
            enrichWithDependencyVersions(pomDirectory, properties);

            // Cache the properties
            projectPropertiesCache.put(pomDirectory, properties);

        } catch (IOException | InterruptedException e) {
            handleMavenCommandError(e);
        }

        return properties;
    }

    /**
     * Validates if the POM directory is usable
     */
    private boolean isValidPomDirectory(String pomDirectory) {
        if (pomDirectory == null || pomDirectory.trim().isEmpty()) {
            log.warn("POM directory is null or empty");
            return false;
        }
        return true;
    }

    /**
     * Extract Maven project properties using help:evaluate
     */
    private Map<String, String> extractMavenProjectProperties(String pomDirectory)
            throws IOException, InterruptedException {

        Map<String, String> properties = new HashMap<>();
        log.info("Executing simple Maven properties command in directory: {}", pomDirectory);

        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.directory(new File(pomDirectory));
        processBuilder.command("mvn", "help:evaluate", "-Dexpression=project.properties", "-q", "-DforceStdout");

        Process process = processBuilder.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

        String line;
        while ((line = reader.readLine()) != null) {
            parsePropertyLine(line, properties);
        }

        int exitCode = process.waitFor();
        logMavenCommandResult(exitCode, properties.size());

        return properties;
    }

    /**
     * Parse a property line from Maven output
     */
    private void parsePropertyLine(String line, Map<String, String> properties) {
        Matcher matcher = PROPERTY_PATTERN.matcher(line);
        if (matcher.find()) {
            String propertyName = matcher.group(1);
            String propertyValue = matcher.group(2);
            properties.put(propertyName, propertyValue);
            log.debug("Found property: {} = {}", propertyName, propertyValue);
        }
    }

    /**
     * Log the result of a Maven command execution
     */
    private void logMavenCommandResult(int exitCode, int propertiesCount) {
        if (exitCode != 0) {
            log.warn("Maven properties command exited with non-zero exit code: {}", exitCode);
        } else {
            log.info("Successfully parsed {} properties", propertiesCount);
        }
    }

    /**
     * Enrich properties map with dependency versions
     */
    private void enrichWithDependencyVersions(String pomDirectory, Map<String, String> properties)
            throws IOException, InterruptedException {

        int exitCode = generateDependencyList(pomDirectory);

        if (exitCode == 0) {
            File dependencyFile = new File(pomDirectory + "/target/all-dependencies.txt");
            if (dependencyFile.exists()) {
                parseDependencyFile(dependencyFile, properties);
                dependencyFile.delete();
            }
        }
    }

    /**
     * Generate a dependency list file using Maven
     */
    private int generateDependencyList(String pomDirectory) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.directory(new File(pomDirectory));
        processBuilder.command(
                "mvn",
                "dependency:list",
                "-DoutputFile=target/all-dependencies.txt",
                "-q"
        );

        Process process = processBuilder.start();
        return process.waitFor();
    }

    /**
     * Parse the dependency list file and extract versions
     */
    private void parseDependencyFile(File dependencyFile, Map<String, String> properties)
            throws IOException {

        try (BufferedReader fileReader = new BufferedReader(new FileReader(dependencyFile))) {
            String line;
            while ((line = fileReader.readLine()) != null) {
                line = line.trim();
                extractVersionFromDependencyLine(line, properties);
            }
        }
    }

    /**
     * Extract version information from a single dependency line
     */
    private void extractVersionFromDependencyLine(String line, Map<String, String> properties) {
        // Format is typically: groupId:artifactId:jar:version:scope
        if (line.contains(":")) {
            String[] parts = line.split(":");
            if (parts.length >= 4) {
                String groupId = parts[0];
                String artifactId = parts[1];
                // Skip packaging part which is typically the 3rd element
                String version = parts[3];

                // Store as both artifactId.version and groupId:artifactId
                properties.put(artifactId + ".version", version);
                properties.put(groupId + ":" + artifactId, version);
                log.debug("Stored dependency: {}:{} = {}", groupId, artifactId, version);
            }
        }
    }

    /**
     * Handle errors from Maven command execution
     */
    private void handleMavenCommandError(Exception e) {
        log.error("Error executing Maven command: {}", e.getMessage(), e);
        if (e instanceof InterruptedException) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public Optional<String> resolveManagedDependencyVersion(String groupId, String artifactId, String pomDirectory) {
        if (!isValidPomDirectory(pomDirectory)) {
            return Optional.empty();
        }

        try {
            log.info("Resolving managed dependency version for {}:{} in directory: {}",
                    groupId, artifactId, pomDirectory);

            // Try to find version in existing properties
            Optional<String> versionFromProperties = findVersionInProperties(groupId, artifactId, pomDirectory);
            if (versionFromProperties.isPresent()) {
                return versionFromProperties;
            }

            // As a last resort, run Maven to resolve this specific dependency
            return resolveSpecificDependency(groupId, artifactId, pomDirectory);

        } catch (IOException | InterruptedException e) {
            handleMavenCommandError(e);
            return Optional.empty();
        }
    }

    /**
     * Try to find a dependency version in the existing properties
     */
    private Optional<String> findVersionInProperties(String groupId, String artifactId, String pomDirectory) {
        // Get the properties map
        Map<String, String> properties = getProjectProperties(pomDirectory);

        // Check for the version using various possible keys
        String version = properties.get(artifactId + ".version");
        if (version == null) {
            version = properties.get(groupId + ":" + artifactId);
        }

        if (version != null) {
            log.info("Found version for {}:{} in properties: {}", groupId, artifactId, version);
            return Optional.of(version);
        }

        return Optional.empty();
    }

    /**
     * Resolve a specific dependency using Maven
     */
    private Optional<String> resolveSpecificDependency(String groupId, String artifactId, String pomDirectory)
            throws IOException, InterruptedException {

        int exitCode = runDependencyResolveCommand(groupId, artifactId, pomDirectory);

        if (exitCode == 0) {
            File dependencyFile = new File(pomDirectory + "/target/specific-dependency.txt");
            if (dependencyFile.exists()) {
                Optional<String> version = extractVersionFromDependencyFile(groupId, artifactId, dependencyFile);
                dependencyFile.delete();
                return version;
            }
        }

        log.debug("Could not resolve version for {}:{}", groupId, artifactId);
        return Optional.empty();
    }

    /**
     * Run Maven dependency:resolve for a specific artifact
     */
    private int runDependencyResolveCommand(String groupId, String artifactId, String pomDirectory)
            throws IOException, InterruptedException {

        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.directory(new File(pomDirectory));
        processBuilder.command(
                "mvn",
                "dependency:resolve",
                "-DincludeArtifactIds=" + artifactId,
                "-DoutputFile=target/specific-dependency.txt",
                "-DincludeGroupIds=" + groupId,
                "-q"
        );

        Process process = processBuilder.start();
        return process.waitFor();
    }

    /**
     * Extract version from the dependency:resolve output file
     */
    private Optional<String> extractVersionFromDependencyFile(String groupId, String artifactId, File dependencyFile)
            throws IOException {

        try (BufferedReader reader = new BufferedReader(new FileReader(dependencyFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // Look for lines with the dependency information
                if (line.contains(groupId + ":" + artifactId)) {
                    // Try to extract version from the line
                    String[] parts = line.split(":");
                    if (parts.length >= 4) {
                        String resolvedVersion = parts[3];
                        log.info("Resolved version for {}:{}: {}", groupId, artifactId, resolvedVersion);
                        return Optional.of(resolvedVersion);
                    }
                }
            }
        }

        return Optional.empty();
    }
} 