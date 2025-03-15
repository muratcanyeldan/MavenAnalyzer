package com.muratcan.yeldan.mavenanalyzer.service.impl;

import com.muratcan.yeldan.mavenanalyzer.dto.ProjectRequest;
import com.muratcan.yeldan.mavenanalyzer.dto.ProjectResponse;
import com.muratcan.yeldan.mavenanalyzer.entity.DependencyAnalysis;
import com.muratcan.yeldan.mavenanalyzer.entity.Project;
import com.muratcan.yeldan.mavenanalyzer.exception.ResourceNotFoundException;
import com.muratcan.yeldan.mavenanalyzer.repository.ProjectRepository;
import com.muratcan.yeldan.mavenanalyzer.service.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;

    @Override
    @Transactional
    public ProjectResponse createProject(ProjectRequest projectRequest) {
        log.info("Creating new project with name: {}", projectRequest.getName());

        Project project = Project.builder()
                .name(projectRequest.getName())
                .description(projectRequest.getDescription())
                .defaultPomPath(projectRequest.getDefaultPomPath())
                .build();

        Project savedProject = projectRepository.save(project);
        log.info("Project created with ID: {}", savedProject.getId());

        return mapToProjectResponse(savedProject);
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectResponse getProjectById(Long id) {
        log.debug("Fetching project with ID: {}", id);

        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with ID: " + id));

        return mapToProjectResponse(project);
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectResponse getProjectByName(String name) {
        log.debug("Fetching project with name: {}", name);

        Project project = projectRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with name: " + name));

        return mapToProjectResponse(project);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectResponse> getAllProjects() {
        log.debug("Fetching all projects");

        List<Project> projects = projectRepository.findAll();

        return projects.stream()
                .map(this::mapToProjectResponse)
                .toList();
    }

    @Override
    @Transactional
    public ProjectResponse updateProject(Long id, ProjectRequest projectRequest) {
        log.info("Updating project with ID: {}, new name: {}", id, projectRequest.getName());

        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with ID: " + id));

        project.setName(projectRequest.getName());
        project.setDescription(projectRequest.getDescription());
        project.setDefaultPomPath(projectRequest.getDefaultPomPath());

        Project updatedProject = projectRepository.save(project);

        return mapToProjectResponse(updatedProject);
    }

    @Override
    @Transactional
    public void deleteProject(Long id) {
        log.debug("Deleting project with ID: {}", id);

        if (!projectRepository.existsById(id)) {
            throw new ResourceNotFoundException("Project not found with ID: " + id);
        }

        projectRepository.deleteById(id);
        log.info("Project deleted with ID: {}", id);
    }

    @Override
    @Transactional
    public ProjectResponse toggleProjectStatus(Long id) {
        log.info("Toggling status for project with ID: {}", id);

        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with ID: " + id));

        // Toggle the status
        project.setStatus(project.getStatus() == Project.ProjectStatus.ACTIVE ?
                Project.ProjectStatus.INACTIVE : Project.ProjectStatus.ACTIVE);

        Project updatedProject = projectRepository.save(project);
        log.info("Project status updated to {} for ID: {}", updatedProject.getStatus(), id);

        return mapToProjectResponse(updatedProject);
    }

    private ProjectResponse mapToProjectResponse(Project project) {
        // Get the latest analysis for the project
        DependencyAnalysis latestAnalysis = project.getAnalyses().stream()
                .max(Comparator.comparing(DependencyAnalysis::getAnalysisDate))
                .orElse(null);

        return ProjectResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .defaultPomPath(project.getDefaultPomPath())
                .status(project.getStatus())
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .totalAnalyses(project.getAnalyses().size())
                .lastAnalysisDate(latestAnalysis != null ?
                        latestAnalysis.getAnalysisDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null)
                .dependencyCount(latestAnalysis != null ? latestAnalysis.getTotalDependencies() : null)
                .outdatedCount(latestAnalysis != null ? latestAnalysis.getOutdatedDependencies() : null)
                .vulnerableCount(latestAnalysis != null ?
                        latestAnalysis.getDependencies().stream()
                                .filter(d -> Boolean.TRUE.equals(d.getIsVulnerable()))
                                .mapToInt(d -> d.getVulnerableCount() != null ? d.getVulnerableCount() : 0)
                                .sum() : null)
                .build();
    }
} 