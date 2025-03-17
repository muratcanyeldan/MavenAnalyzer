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

}