package com.muratcan.yeldan.mavenanalyzer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for Maven dependency version lookup
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VersionLookupResponse {

    /**
     * The version string
     */
    private String version;

    /**
     * Whether the version was successfully determined
     */
    private boolean success;
} 