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

    private static final String MANAGED_BY_BOM = "MANAGED_BY_BOM";

    private final LicenseEnricherService licenseEnricherService;
    private final MavenMetadataService mavenMetadataService;
    private final MavenCommandExecutorService mavenCommandExecutorService;


    @Override
    public List<DependencyInfo> parsePomDependencies(String pomContent, String pomDirectory, boolean includeTransitive) {
        List<DependencyInfo> dependencies = new ArrayList<>();

        try {
            Model model = parsePomModel(pomContent);
            ParentInfo parentInfo = extractParentInfo(model);
            Properties properties = processProperties(model, pomDirectory);
            Map<String, String> managedVersions = extractManagedVersions(model);

            String projectLicense = extractProjectLicense(model);

            for (Dependency dependency : model.getDependencies()) {
                if (!includeTransitive && ("true".equals(dependency.getOptional()) || "provided".equals(dependency.getScope()) || "test".equals(dependency.getScope()))) {
                    log.debug("Skipping dependency: {}:{}:{}",
                            dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion());
                } else {
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
            }

            return dependencies;
        } catch (IOException | XmlPullParserException e) {
            log.error("Error parsing POM: {}", e.getMessage());
            return dependencies;
        }
    }

    private Model parsePomModel(String pomContent) throws IOException, XmlPullParserException {
        MavenXpp3Reader reader = new MavenXpp3Reader();
        return reader.read(new StringReader(pomContent));
    }

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

    private Properties processProperties(Model model, String pomDirectory) {
        Properties properties = model.getProperties();
        if (properties == null) {
            properties = new Properties();
        }

        if (pomDirectory != null && !pomDirectory.trim().isEmpty()) {
            log.info("POM directory provided, fetching project properties using Maven");
            Map<String, String> mavenProperties = mavenCommandExecutorService.getProjectProperties(pomDirectory);

            for (Map.Entry<String, String> entry : mavenProperties.entrySet()) {
                properties.setProperty(entry.getKey(), entry.getValue());
            }

            log.debug("Added {} Maven properties to the properties map", mavenProperties.size());
        }

        return properties;
    }

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

        if (isBomManaged) {
            VersionResolutionResult result = resolveBomManagedVersion(
                    dependency, parentInfo, managedVersions, properties, pomDirectory);

            version = result.version();
            estimatedVersion = result.estimatedVersion();
            isBomManaged = result.bomManaged();
        }

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

        estimatedVersion.ifPresent(dependencyInfo::setEstimatedVersion);

        return dependencyInfo;
    }

    private VersionResolutionResult resolveBomManagedVersion(
            Dependency dependency,
            ParentInfo parentInfo,
            Map<String, String> managedVersions,
            Properties properties,
            String pomDirectory) {

        String version;
        Optional<String> estimatedVersion = Optional.empty();
        boolean isBomManaged = true;

        if (parentInfo.parentVersion() != null &&
                dependency.getGroupId().startsWith("org.springframework.boot")) {

            version = parentInfo.parentVersion() + " (from parent)";
        } else {
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

    private String resolveVersionFromExternalSources(
            String groupId,
            String artifactId,
            ParentInfo parentInfo,
            String pomDirectory) {

        String version = MANAGED_BY_BOM;
        Optional<String> estimatedVersion;

        if (pomDirectory != null && !pomDirectory.trim().isEmpty()) {
            Optional<String> resolvedVersion = mavenCommandExecutorService.resolveManagedDependencyVersion(
                    groupId, artifactId, pomDirectory);

            if (resolvedVersion.isPresent()) {
                String resolvedVersionStr = resolvedVersion.get();
                version = resolvedVersionStr + " (resolved from BOM)";
                log.info("Successfully resolved BOM-managed version for {}:{}: {}",
                        groupId, artifactId, resolvedVersionStr);
                return version;
            }
        }

        estimatedVersion = mavenMetadataService.estimateBomManagedVersion(
                groupId,
                artifactId,
                parentInfo.parentGroupId(),
                parentInfo.parentArtifactId(),
                parentInfo.parentVersion()
        );

        if (estimatedVersion.isPresent()) {
            version = estimatedVersion.get();
        }

        return version;
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

        Pattern pattern = Pattern.compile("\\$\\{([^}]+)\\}");
        Matcher matcher = pattern.matcher(version);

        if (matcher.find()) {
            String propertyName = matcher.group(1);
            String propertyValue = properties.getProperty(propertyName);

            if (propertyValue != null) {
                return version.replace("${" + propertyName + "}", propertyValue);
            } else {
                log.debug("Could not resolve property in version: {}", version);
                return version;
            }
        }

        return version;
    }

    private record ParentInfo(String parentGroupId, String parentArtifactId, String parentVersion) {

    }

    private record VersionResolutionResult(String version, Optional<String> estimatedVersion, boolean bomManaged) {
    }
} 