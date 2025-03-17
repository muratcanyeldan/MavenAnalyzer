package com.muratcan.yeldan.mavenanalyzer.dto.request;

import com.muratcan.yeldan.mavenanalyzer.entity.Project.ProjectStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectRequest {

    @NotBlank(message = "Project name is required")
    private String name;

    private String description;

    private String defaultPomPath;

    private ProjectStatus status;
} 