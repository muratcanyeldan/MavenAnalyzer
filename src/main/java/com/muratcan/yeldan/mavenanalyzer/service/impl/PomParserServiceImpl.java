package com.muratcan.yeldan.mavenanalyzer.service.impl;

import com.muratcan.yeldan.mavenanalyzer.dto.DependencyInfo;
import com.muratcan.yeldan.mavenanalyzer.service.LicenseEnricherService;
import com.muratcan.yeldan.mavenanalyzer.service.MavenCommandExecutorService;
import com.muratcan.yeldan.mavenanalyzer.service.MavenMetadataService;
import com.muratcan.yeldan.mavenanalyzer.service.PomParserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.License;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PomParserServiceImpl implements PomParserService {

    private final LicenseEnricherService licenseEnricherService;
    private final MavenMetadataService mavenMetadataService;
    private final MavenCommandExecutorService mavenCommandExecutorService;

    @Override
    public List<DependencyInfo> parsePomDependencies(String pomContent) {
        return parsePomDependencies(pomContent, null);
    }

    @Override
    public List<DependencyInfo> parsePomDependencies(String pomContent, String pomDirectory) {
        return parsePomDependencies(pomContent, pomDirectory, true); // Default to including transitive dependencies
    }

    @Override
    public List<DependencyInfo> parsePomDependencies(String pomContent, String pomDirectory, boolean includeTransitive) {
        List<DependencyInfo> dependencies = new ArrayList<>();

        try {
            // Parse POM model
            Model model = parsePomModel(pomContent);

            // Extract parent information
            ParentInfo parentInfo = extractParentInfo(model);

            // Process properties
            Properties properties = processProperties(model, pomDirectory);

            // Extract managed versions from dependencyManagement section
            Map<String, String> managedVersions = extractManagedVersions(model);

            // Extract license from project if available
            String projectLicense = extractProjectLicense(model);

            // Process each dependency
            for (Dependency dependency : model.getDependencies()) {
                // Skip transitive dependencies if not requested
                if (!includeTransitive && "true".equals(dependency.getOptional())) {
                    log.debug("Skipping optional/transitive dependency: {}:{}:{}",
                            dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion());
                    continue;
                }

                // Skip dependencies with "provided" scope when not including transitive deps
                if (!includeTransitive && "provided".equals(dependency.getScope())) {
                    log.debug("Skipping provided scope dependency: {}:{}:{}",
                            dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion());
                    continue;
                }

                // Skip test dependencies when not including transitive deps
                if (!includeTransitive && "test".equals(dependency.getScope())) {
                    log.debug("Skipping test scope dependency: {}:{}:{}",
                            dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion());
                    continue;
                }

                DependencyInfo dependencyInfo = processDependency(
                        dependency,
                        properties,
                        parentInfo,
                        managedVersions,
                        projectLicense,
                        pomDirectory
                );

                dependencies.add(licenseEnricherService.enrichWithLicenseInfo(dependencyInfo));
            }

            return dependencies;
        } catch (IOException | XmlPullParserException e) {
            log.error("Error parsing POM: {}", e.getMessage());
            return dependencies;
        }
    }

    /**
     * Parse the POM content into a Model object
     */
    private Model parsePomModel(String pomContent) throws IOException, XmlPullParserException {
        MavenXpp3Reader reader = new MavenXpp3Reader();
        return reader.read(new StringReader(pomContent));
    }

    /**
     * Extract parent POM information
     */
    private ParentInfo extractParentInfo(Model model) {
        if (model.getParent() != null) {
            String parentGroupId = model.getParent().getGroupId();
            String parentArtifactId = model.getParent().getArtifactId();
            String parentVersion = model.getParent().getVersion();
            log.debug("Parent POM: {}:{}:{}", parentGroupId, parentArtifactId, parentVersion);

            return new ParentInfo(parentGroupId, parentArtifactId, parentVersion);
        }

        return new ParentInfo(null, null, null);
    }

    /**
     * Process model properties and Maven project properties
     */
    private Properties processProperties(Model model, String pomDirectory) {
        Properties properties = model.getProperties();
        if (properties == null) {
            properties = new Properties();
        }

        // If pomDirectory is provided, fetch project properties using Maven
        if (pomDirectory != null && !pomDirectory.trim().isEmpty()) {
            log.info("POM directory provided, fetching project properties using Maven");
            Map<String, String> mavenProperties = mavenCommandExecutorService.getProjectProperties(pomDirectory);

            // Add Maven properties to the properties map for resolution
            for (Map.Entry<String, String> entry : mavenProperties.entrySet()) {
                properties.setProperty(entry.getKey(), entry.getValue());
            }

            log.debug("Added {} Maven properties to the properties map", mavenProperties.size());
        }

        return properties;
    }

    /**
     * Extract managed versions from dependencyManagement section
     */
    private Map<String, String> extractManagedVersions(Model model) {
        Map<String, String> managedVersions = new HashMap<>();

        if (model.getDependencyManagement() != null &&
                model.getDependencyManagement().getDependencies() != null) {

            for (Dependency managedDep : model.getDependencyManagement().getDependencies()) {
                String key = managedDep.getGroupId() + ":" + managedDep.getArtifactId();
                managedVersions.put(key, managedDep.getVersion());
            }
        }

        return managedVersions;
    }

    /**
     * Extract license from project if available
     */
    private String extractProjectLicense(Model model) {
        if (model.getLicenses() != null && !model.getLicenses().isEmpty()) {
            String projectLicense = model.getLicenses().stream()
                    .map(License::getName)
                    .collect(Collectors.joining(", "));

            log.debug("Project license: {}", projectLicense);
            return projectLicense;
        }

        return null;
    }

    /**
     * Process a single dependency
     */
    private DependencyInfo processDependency(
            Dependency dependency,
            Properties properties,
            ParentInfo parentInfo,
            Map<String, String> managedVersions,
            String projectLicense,
            String pomDirectory) {

        String version = resolveVersion(dependency.getVersion(), properties);
        boolean isBomManaged = version == null; // If version is null, it's managed by BOM
        Optional<String> estimatedVersion = Optional.empty();

        // Handle BOM managed dependencies
        if (isBomManaged) {
            VersionResolutionResult result = resolveBomManagedVersion(
                    dependency, parentInfo, managedVersions, properties, pomDirectory);

            version = result.getVersion();
            estimatedVersion = result.getEstimatedVersion();
            isBomManaged = result.isBomManaged();
        }

        // Create dependency info
        DependencyInfo dependencyInfo = DependencyInfo.builder()
                .groupId(dependency.getGroupId())
                .artifactId(dependency.getArtifactId())
                .version(version)
                .scope(dependency.getScope())
                .license(projectLicense) // Start with project license as default
                .isOptional(dependency.isOptional())
                .isBomManaged(isBomManaged)
                .originalDefinition(dependency.toString())
                .build();

        // If we have an estimated version, set it in the dependency info
        estimatedVersion.ifPresent(dependencyInfo::setEstimatedVersion);

        return dependencyInfo;
    }

    /**
     * Resolve version for BOM managed dependencies
     */
    private VersionResolutionResult resolveBomManagedVersion(
            Dependency dependency,
            ParentInfo parentInfo,
            Map<String, String> managedVersions,
            Properties properties,
            String pomDirectory) {

        String version = null;
        Optional<String> estimatedVersion = Optional.empty();
        boolean isBomManaged = true;

        // For Spring Boot starters managed by parent, use parent version
        if (parentInfo.getParentVersion() != null &&
                dependency.getGroupId().startsWith("org.springframework.boot")) {

            version = parentInfo.getParentVersion() + " (from parent)";
        } else {
            // Try to find in dependency management
            String key = dependency.getGroupId() + ":" + dependency.getArtifactId();
            String managedVersion = managedVersions.get(key);

            if (managedVersion != null) {
                version = resolveVersion(managedVersion, properties) + " (managed)";
            } else {
                version = resolveVersionFromExternalSources(
                        dependency.getGroupId(),
                        dependency.getArtifactId(),
                        parentInfo,
                        pomDirectory
                );
            }
        }

        return new VersionResolutionResult(version, estimatedVersion, isBomManaged);
    }

    /**
     * Resolve version from Maven or estimate based on parent BOM
     */
    private String resolveVersionFromExternalSources(
            String groupId,
            String artifactId,
            ParentInfo parentInfo,
            String pomDirectory) {

        String version = "MANAGED_BY_BOM";
        Optional<String> estimatedVersion = Optional.empty();

        // If pomDirectory is provided, try to resolve the managed version using Maven
        if (pomDirectory != null && !pomDirectory.trim().isEmpty()) {
            Optional<String> resolvedVersion = mavenCommandExecutorService.resolveManagedDependencyVersion(
                    groupId, artifactId, pomDirectory);

            if (resolvedVersion.isPresent()) {
                String resolvedVersionStr = resolvedVersion.get();
                estimatedVersion = resolvedVersion;
                version = resolvedVersionStr + " (resolved from BOM)";
                log.info("Successfully resolved BOM-managed version for {}:{}: {}",
                        groupId, artifactId, resolvedVersionStr);
                return version;
            }
        }

        // Try to estimate the version based on parent BOM
        estimatedVersion = mavenMetadataService.estimateBomManagedVersion(
                groupId,
                artifactId,
                parentInfo.getParentGroupId(),
                parentInfo.getParentArtifactId(),
                parentInfo.getParentVersion()
        );

        if (estimatedVersion.isPresent()) {
            version = estimatedVersion.get();
        }

        return version;
    }

    @Override
    public boolean isValidPom(String pomContent) {
        try {
            MavenXpp3Reader reader = new MavenXpp3Reader();
            reader.read(new StringReader(pomContent));
            return true;
        } catch (IOException | XmlPullParserException e) {
            log.error("Invalid POM content: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Resolve a version string that may contain property references
     *
     * @param version    The version string that may contain property references like ${project.version}
     * @param properties The properties to use for resolution
     * @return The resolved version string, or null if it couldn't be resolved
     */
    private String resolveVersion(String version, Properties properties) {
        if (version == null) {
            return null;
        }

        // Pattern to match ${property}
        Pattern pattern = Pattern.compile("\\$\\{([^}]+)\\}");
        Matcher matcher = pattern.matcher(version);

        if (matcher.find()) {
            String propertyName = matcher.group(1);
            String propertyValue = properties.getProperty(propertyName);

            if (propertyValue != null) {
                return version.replace("${" + propertyName + "}", propertyValue);
            } else {
                // If the property value is not found, return the original version
                log.debug("Could not resolve property in version: {}", version);
                return version;
            }
        }

        return version;
    }

    /**
     * Simple value class to hold parent POM information
     */
    private static class ParentInfo {
        private final String parentGroupId;
        private final String parentArtifactId;
        private final String parentVersion;

        public ParentInfo(String parentGroupId, String parentArtifactId, String parentVersion) {
            this.parentGroupId = parentGroupId;
            this.parentArtifactId = parentArtifactId;
            this.parentVersion = parentVersion;
        }

        public String getParentGroupId() {
            return parentGroupId;
        }

        public String getParentArtifactId() {
            return parentArtifactId;
        }

        public String getParentVersion() {
            return parentVersion;
        }
    }

    /**
     * Simple value class to hold version resolution results
     */
    private static class VersionResolutionResult {
        private final String version;
        private final Optional<String> estimatedVersion;
        private final boolean bomManaged;

        public VersionResolutionResult(String version, Optional<String> estimatedVersion, boolean bomManaged) {
            this.version = version;
            this.estimatedVersion = estimatedVersion;
            this.bomManaged = bomManaged;
        }

        public String getVersion() {
            return version;
        }

        public Optional<String> getEstimatedVersion() {
            return estimatedVersion;
        }

        public boolean isBomManaged() {
            return bomManaged;
        }
    }
} 