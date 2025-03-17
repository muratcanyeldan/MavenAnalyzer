package com.muratcan.yeldan.mavenanalyzer.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.muratcan.yeldan.mavenanalyzer.dto.MavenArtifactInfo;
import com.muratcan.yeldan.mavenanalyzer.service.MavenClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

            // simplified comparison logic - in a production app, 
            // we would use Maven's version comparison logic
            return !cleanedCurrentVersion.equals(latestVersion);
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

            if (cleanedCurrentVersion.equals(latestVersionInfo.get().getLatestVersion())) {
                return 0;
            }

            // This is a simplified calculation - in a production app, we would fetch all versions
            // and calculate the exact number of versions between current and latest
            // For now, we'll estimate based on version numbers
            String latestVersion = latestVersionInfo.get().getLatestVersion();

            // Basic semantic versioning comparison (simplified)
            String[] currentParts = cleanedCurrentVersion.split("\\.");
            String[] latestParts = latestVersion.split("\\.");

            int majorDiff = compareVersionParts(currentParts, latestParts, 0);
            int minorDiff = compareVersionParts(currentParts, latestParts, 1);
            int patchDiff = compareVersionParts(currentParts, latestParts, 2);

            // Rough estimation of versions behind
            int versionsBehind = Math.max(1, majorDiff * 100 + minorDiff * 10 + patchDiff);

            log.debug("Estimated versions behind for {}:{}: {}", groupId, artifactId, versionsBehind);
            return versionsBehind;

        } catch (Exception e) {
            log.error("Error calculating versions behind for artifact: {}:{}", groupId, artifactId, e);
            return 0;
        }
    }

    private int compareVersionParts(String[] currentParts, String[] latestParts, int index) {
        if (currentParts.length <= index || latestParts.length <= index) {
            return 0;
        }

        try {
            int current = Integer.parseInt(currentParts[index]);
            int latest = Integer.parseInt(latestParts[index]);
            return Math.max(0, latest - current);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
} 