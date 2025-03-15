package com.muratcan.yeldan.mavenanalyzer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Information about a Maven dependency extracted from a POM file
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DependencyInfo {

    private String groupId;
    private String artifactId;
    private String version;
    private String scope;
    private String license;
    private boolean optional;
    private boolean isOptional;
    private boolean isBomManaged;
    private String originalDefinition;

    /**
     * Estimated version for dependencies managed by BOMs
     * This field is populated when a dependency is managed by a BOM
     * and we can estimate its actual version based on common frameworks.
     */
    private String estimatedVersion;
} 