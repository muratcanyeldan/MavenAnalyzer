package com.muratcan.yeldan.mavenanalyzer.controller;

import com.muratcan.yeldan.mavenanalyzer.dto.VersionLookupResponse;
import com.muratcan.yeldan.mavenanalyzer.service.MavenMetadataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for Maven-related API endpoints
 */
@RestController
@RequestMapping("/api/maven")
@RequiredArgsConstructor
@Slf4j
public class MavenApiController {

    private final MavenMetadataService mavenMetadataService;

    /**
     * Lookup the exact version of a Maven dependency
     * This is particularly useful for BOM-managed dependencies
     *
     * @param groupId          Maven group ID
     * @param artifactId       Maven artifact ID
     * @param parentGroupId    Optional parent/BOM group ID
     * @param parentArtifactId Optional parent/BOM artifact ID
     * @param parentVersion    Optional parent/BOM version
     * @return Version lookup response
     */
    @GetMapping("/lookup")
    public ResponseEntity<VersionLookupResponse> lookupVersion(
            @RequestParam String groupId,
            @RequestParam String artifactId,
            @RequestParam(required = false) String parentGroupId,
            @RequestParam(required = false) String parentArtifactId,
            @RequestParam(required = false) String parentVersion) {

        log.info("Looking up version for {}:{} with parent {}:{}:{}",
                groupId, artifactId, parentGroupId, parentArtifactId, parentVersion);

        try {
            // For now we're just using the estimateBomManagedVersion method
            // In a full implementation, you might want to look up the version from Maven Central directly
            var estimatedVersion = mavenMetadataService.estimateBomManagedVersion(
                    groupId, artifactId, parentGroupId, parentArtifactId, parentVersion);

            if (estimatedVersion.isPresent()) {
                return ResponseEntity.ok(new VersionLookupResponse(estimatedVersion.get(), true));
            } else {
                // If we can't estimate, return a placeholder message
                return ResponseEntity.ok(new VersionLookupResponse("Unable to determine version", false));
            }
        } catch (Exception e) {
            log.error("Error looking up version for {}:{}: {}", groupId, artifactId, e.getMessage());
            return ResponseEntity.badRequest().body(
                    new VersionLookupResponse("Error looking up version: " + e.getMessage(), false));
        }
    }
} 