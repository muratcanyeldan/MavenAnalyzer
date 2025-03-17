package com.muratcan.yeldan.mavenanalyzer.service;

import com.muratcan.yeldan.mavenanalyzer.dto.request.ProjectRequest;
import com.muratcan.yeldan.mavenanalyzer.dto.response.ProjectResponse;

import java.util.List;

public interface ProjectService {

    ProjectResponse createProject(ProjectRequest projectRequest);

    ProjectResponse getProjectById(Long id);

    ProjectResponse getProjectByName(String name);

    List<ProjectResponse> getAllProjects();

    ProjectResponse updateProject(Long id, ProjectRequest projectRequest);

    void deleteProject(Long id);

    ProjectResponse toggleProjectStatus(Long id);
} 