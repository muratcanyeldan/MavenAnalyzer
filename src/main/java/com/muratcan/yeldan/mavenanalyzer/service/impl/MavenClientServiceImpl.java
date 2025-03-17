package com.muratcan.yeldan.mavenanalyzer.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.muratcan.yeldan.mavenanalyzer.dto.MavenArtifactInfo;
import com.muratcan.yeldan.mavenanalyzer.service.MavenClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MavenClientServiceImpl implements MavenClientService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${maven.api.base-url}")
    private String mavenApiBaseUrl;

    @Override
    public Optional<MavenArtifactInfo> getLatestArtifactVersion(String groupId, String artifactId) {
        try {
            log.debug("Fetching latest version for artifact: {}:{}", groupId, artifactId);

            String encodedGroupId = URLEncoder.encode(groupId, StandardCharsets.UTF_8);
            String encodedArtifactId = URLEncoder.encode(artifactId, StandardCharsets.UTF_8);

            String url = UriComponentsBuilder.fromUriString(mavenApiBaseUrl)
                    .queryParam("q", "g:" + encodedGroupId + " AND a:" + encodedArtifactId)
                    .queryParam("rows", "1")
                    .queryParam("wt", "json")
                    .build()
                    .toUriString();

            String response = restTemplate.getForObject(url, String.class);

            if (response == null) {
                log.warn("No response received from Maven central for {}:{}", groupId, artifactId);
                return Optional.empty();
            }

            JsonNode rootNode = objectMapper.readTree(response);
            JsonNode responseNode = rootNode.path("response");

            if (responseNode.path("numFound").asInt() == 0) {
                log.warn("No artifact found for {}:{}", groupId, artifactId);
                return Optional.empty();
            }

            JsonNode docNode = responseNode.path("docs").get(0);

            String latestVersion = docNode.path("latestVersion").asText();
            long timestamp = docNode.path("timestamp").asLong();
            int versionCount = docNode.path("versionCount").asInt();

            MavenArtifactInfo artifactInfo = MavenArtifactInfo.builder()
                    .groupId(groupId)
                    .artifactId(artifactId)
                    .latestVersion(latestVersion)
                    .timestamp(Instant.ofEpochMilli(timestamp))
                    .totalVersions(versionCount)
                    .build();

            log.debug("Found latest version for {}:{}: {}", groupId, artifactId, latestVersion);
            return Optional.of(artifactInfo);

        } catch (Exception e) {
            log.error("Error fetching latest version for artifact: {}:{}", groupId, artifactId, e);
            return Optional.empty();
        }
    }

    @Override
    public boolean isNewerVersionAvailable(String groupId, String artifactId, String currentVersion) {
        try {
            log.debug("Checking if newer version is available for artifact: {}:{} (current: {})",
                    groupId, artifactId, currentVersion);

            String cleanedCurrentVersion = currentVersion.replaceAll("\\s+\\(.*\\)$", "");

            Optional<MavenArtifactInfo> latestVersionInfo = getLatestArtifactVersion(groupId, artifactId);

            if (latestVersionInfo.isEmpty()) {
                return false;
            }

            String latestVersion = latestVersionInfo.get().getLatestVersion();

            log.debug("Latest version of {}:{} is {}. Current version (cleaned): {}",
                    groupId, artifactId, latestVersion, cleanedCurrentVersion);

            ComparableVersion currentVersionComparable = new ComparableVersion(cleanedCurrentVersion);
            ComparableVersion latestVersionComparable = new ComparableVersion(latestVersion);

            return currentVersionComparable.compareTo(latestVersionComparable) < 0;
        } catch (Exception e) {
            log.error("Error checking for newer version of {}:{}: {}",
                    groupId, artifactId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public int getVersionsBehind(String groupId, String artifactId, String currentVersion) {
        try {
            log.debug("Calculating versions behind for artifact: {}:{} (current: {})",
                    groupId, artifactId, currentVersion);

            String cleanedCurrentVersion = currentVersion.replaceAll("\\s+\\(.*\\)$", "");

            Optional<MavenArtifactInfo> latestVersionInfo = getLatestArtifactVersion(groupId, artifactId);

            if (latestVersionInfo.isEmpty()) {
                return 0;
            }

            String latestVersion = latestVersionInfo.get().getLatestVersion();

            ComparableVersion currentVersionComparable = new ComparableVersion(cleanedCurrentVersion);
            ComparableVersion latestVersionComparable = new ComparableVersion(latestVersion);

            if (currentVersionComparable.compareTo(latestVersionComparable) >= 0) {
                return 0;
            }

            // In a production environment, we could fetch all versions between current and latest
            // For now, we'll calculate a reasonable estimate of versions behind based on the version difference
            String[] currentParts = cleanedCurrentVersion.split("\\.");
            String[] latestParts = latestVersion.split("\\.");

            int majorDiff = compareVersionParts(currentParts, latestParts, 0);
            int minorDiff = compareVersionParts(currentParts, latestParts, 1);
            int patchDiff = compareVersionParts(currentParts, latestParts, 2);

            int versionsBehind;

            if (majorDiff > 0) {
                versionsBehind = majorDiff * 10 + Math.min(minorDiff, 5);
            } else if (minorDiff > 0) {
                versionsBehind = minorDiff * 3 + Math.min(patchDiff, 5);
            } else {
                versionsBehind = Math.max(1, patchDiff);
            }

            log.debug("Estimated versions behind for {}:{}: {} (current: {}, latest: {})",
                    groupId, artifactId, versionsBehind, cleanedCurrentVersion, latestVersion);
            return versionsBehind;

        } catch (Exception e) {
            log.error("Error calculating versions behind for artifact: {}:{}: {}",
                    groupId, artifactId, e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Compare version parts and handle non-numeric version components
     *
     * @param currentParts The parts of the current version (split by ".")
     * @param latestParts  The parts of the latest version (split by ".")
     * @param index        The index of the part to compare (0=major, 1=minor, 2=patch)
     * @return The difference between the parts, or 0 if not comparable
     */
    private int compareVersionParts(String[] currentParts, String[] latestParts, int index) {
        if (currentParts.length <= index || latestParts.length <= index) {
            if (latestParts.length > index) {
                try {
                    return Integer.parseInt(latestParts[index]);
                } catch (NumberFormatException e) {
                    ComparableVersion latestPartVersion = new ComparableVersion(latestParts[index]);
                    return latestPartVersion.compareTo(new ComparableVersion("0"));
                }
            }
            return 0;
        }

        try {
            int current = Integer.parseInt(extractNumericPart(currentParts[index]));
            int latest = Integer.parseInt(extractNumericPart(latestParts[index]));
            return Math.max(0, latest - current);
        } catch (NumberFormatException e) {
            ComparableVersion currentPartVersion = new ComparableVersion(currentParts[index]);
            ComparableVersion latestPartVersion = new ComparableVersion(latestParts[index]);

            int comparison = latestPartVersion.compareTo(currentPartVersion);
            return comparison > 0 ? 1 : 0;
        }
    }

    private String extractNumericPart(String versionPart) {
        if (versionPart.contains("-")) {
            return versionPart.split("-")[0];
        }
        StringBuilder numericPart = new StringBuilder();
        for (char c : versionPart.toCharArray()) {
            if (Character.isDigit(c)) {
                numericPart.append(c);
            } else {
                break;
            }
        }
        return !numericPart.isEmpty() ? numericPart.toString() : "0";
    }
} 