package com.muratcan.yeldan.mavenanalyzer.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisRequest {

    // Project ID is optional to support analyses without a project
    private Long projectId;

    @NotEmpty(message = "POM content is required")
    private String pomContent;

    private boolean checkVulnerabilities;

    private boolean checkLicenses;

    private String pomDirectoryPath;

    // Whether to include transitive dependencies in the analysis
    private boolean includeTransitive = true;

    private boolean notifyOnCompletion;
} 