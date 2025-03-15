package com.muratcan.yeldan.mavenanalyzer.dto;

import com.muratcan.yeldan.mavenanalyzer.entity.Project.ProjectStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectResponse {

    private Long id;
    private String name;
    private String description;
    private String defaultPomPath;
    private ProjectStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer totalAnalyses;
    private String lastAnalysisDate;

    // Dependency statistics for the latest analysis
    private Integer dependencyCount;
    private Integer outdatedCount;
    private Integer vulnerableCount;
} 