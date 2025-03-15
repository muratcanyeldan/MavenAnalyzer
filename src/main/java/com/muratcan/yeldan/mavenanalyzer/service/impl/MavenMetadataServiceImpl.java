package com.muratcan.yeldan.mavenanalyzer.service.impl;

import com.muratcan.yeldan.mavenanalyzer.service.MavenMetadataService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.model.License;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class MavenMetadataServiceImpl implements MavenMetadataService {

    private static final String MAVEN_CENTRAL_BASE_URL = "https://repo1.maven.org/maven2/";
    private static final String POM_FILE_PATH = "%s/%s/%s/%s-%s.pom";
    // Constant to indicate BOM-managed dependencies
    private static final String MANAGED_BY_BOM = "MANAGED_BY_BOM";
    private final RestTemplate restTemplate;
    @Getter
    @Value("${license.cache.enabled:true}")
    private boolean licenseCacheEnabled;

    @Getter
    @Value("${version.estimate.cache.enabled:true}")
    private boolean versionEstimateCacheEnabled;

    public MavenMetadataServiceImpl() {
        this.restTemplate = new RestTemplate();
    }

    @Override
    @Cacheable(value = "licenseCache", key = "#groupId + ':' + #artifactId + ':' + #version",
            unless = "#result.isEmpty()", condition = "#root.target.licenseCacheEnabled")
    public Optional<String> fetchLicenseInfo(String groupId, String artifactId, String version) {
        try {
            // Convert groupId to path format (org.springframework becomes org/springframework)
            String groupPath = groupId.replace('.', '/');

            // Construct POM URL
            String pomUrl = String.format(
                    MAVEN_CENTRAL_BASE_URL + POM_FILE_PATH,
                    groupPath, artifactId, version, artifactId, version
            );

            log.debug("Fetching POM from: {}", pomUrl);

            // Fetch POM file
            String pomContent = restTemplate.getForObject(pomUrl, String.class);
            if (pomContent == null) {
                log.warn("Could not fetch POM for {}:{}:{}", groupId, artifactId, version);
                return Optional.empty();
            }

            // Parse POM to extract license info
            MavenXpp3Reader reader = new MavenXpp3Reader();
            Model model = reader.read(new StringReader(pomContent));

            Optional<String> license = extractLicenseFromModel(model);

            // If no license found in the artifact's POM, try to check parent POM if available
            if (license.isEmpty() && model.getParent() != null) {
                String parentGroupId = model.getParent().getGroupId();
                String parentArtifactId = model.getParent().getArtifactId();
                String parentVersion = model.getParent().getVersion();

                log.debug("No license found in artifact POM, checking parent: {}:{}:{}",
                        parentGroupId, parentArtifactId, parentVersion);

                // Recursive call to fetch parent's license
                Optional<String> parentLicense = fetchLicenseInfo(parentGroupId, parentArtifactId, parentVersion);
                if (parentLicense.isPresent()) {
                    log.info("Found license in parent POM for {}:{}:{}: {}",
                            groupId, artifactId, version, parentLicense.get());
                    license = parentLicense;
                }
            }

            return license;

        } catch (RestClientException e) {
            log.warn("Error fetching POM for {}:{}:{}: {}", groupId, artifactId, version, e.getMessage());
        } catch (IOException | XmlPullParserException e) {
            log.warn("Error parsing POM for {}:{}:{}: {}", groupId, artifactId, version, e.getMessage());
        }

        return Optional.empty();
    }

    @Override
    @Cacheable(value = "versionEstimateCache", key = "#groupId + ':' + #artifactId + ':' + #parentGroupId + ':' + #parentArtifactId + ':' + #parentVersion",
            unless = "#result.isEmpty()", condition = "#root.target.versionEstimateCacheEnabled")
    public Optional<String> estimateBomManagedVersion(String groupId, String artifactId,
                                                      String parentGroupId, String parentArtifactId, String parentVersion) {
        if (groupId == null || artifactId == null) {
            return Optional.empty();
        }

        // If we have parent BOM info, simply indicate it's managed by a BOM
        if (parentGroupId != null && parentArtifactId != null && parentVersion != null) {
            // For Spring Boot-related dependencies, we can be more specific
            if (parentGroupId.equals("org.springframework.boot") && parentArtifactId.contains("spring-boot")) {
                log.debug("Dependency {}:{} is managed by Spring Boot BOM {}",
                        groupId, artifactId, parentVersion);
                return Optional.of(MANAGED_BY_BOM + " (Spring Boot " + parentVersion + ")");
            }
            // For other BOMs, provide the parent info
            else {
                log.debug("Dependency {}:{} is managed by BOM {}:{}:{}",
                        groupId, artifactId, parentGroupId, parentArtifactId, parentVersion);
                return Optional.of(MANAGED_BY_BOM + " (" + parentGroupId + ":" + parentArtifactId + ")");
            }
        }

        return Optional.empty();
    }

    private Optional<String> extractLicenseFromModel(Model model) {
        List<License> licenses = model.getLicenses();
        if (licenses != null && !licenses.isEmpty()) {
            String licenseStr = licenses.stream()
                    .map(License::getName)
                    .collect(Collectors.joining(", "));
            return Optional.of(licenseStr);
        }
        return Optional.empty();
    }
} 