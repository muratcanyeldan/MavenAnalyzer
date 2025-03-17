package com.muratcan.yeldan.mavenanalyzer.dto.request;

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

    private Long projectId;

    @NotEmpty(message = "POM content is required")
    private String pomContent;

    private boolean checkVulnerabilities;

    private boolean checkLicenses;

    private String pomDirectoryPath;

    private boolean includeTransitive = true;

    private boolean notifyOnCompletion;
} 