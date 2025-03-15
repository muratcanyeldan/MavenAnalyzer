package com.muratcan.yeldan.mavenanalyzer.controller;

import com.muratcan.yeldan.mavenanalyzer.dto.ProjectRequest;
import com.muratcan.yeldan.mavenanalyzer.dto.ProjectResponse;
import com.muratcan.yeldan.mavenanalyzer.entity.Project;
import com.muratcan.yeldan.mavenanalyzer.exception.ResourceNotFoundException;
import com.muratcan.yeldan.mavenanalyzer.repository.ProjectRepository;
import com.muratcan.yeldan.mavenanalyzer.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Project Management", description = "API endpoints for managing projects")
public class ProjectController {

    private final ProjectService projectService;
    private final ProjectRepository projectRepository;

    @PostMapping
    @Operation(summary = "Create a new project", description = "Create a new project for tracking dependency updates")
    public ResponseEntity<ProjectResponse> createProject(@Valid @RequestBody ProjectRequest projectRequest) {
        log.info("Creating new project with name: {}", projectRequest.getName());
        ProjectResponse createdProject = projectService.createProject(projectRequest);
        return new ResponseEntity<>(createdProject, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get project by ID", description = "Retrieve a project by its ID")
    public ResponseEntity<ProjectResponse> getProjectById(@PathVariable Long id) {
        log.info("Fetching project with ID: {}", id);
        ProjectResponse project = projectService.getProjectById(id);
        return ResponseEntity.ok(project);
    }

    @GetMapping("/name/{name}")
    @Operation(summary = "Get project by name", description = "Retrieve a project by its name")
    public ResponseEntity<ProjectResponse> getProjectByName(@PathVariable String name) {
        log.info("Fetching project with name: {}", name);
        ProjectResponse project = projectService.getProjectByName(name);
        return ResponseEntity.ok(project);
    }

    @GetMapping
    @Operation(summary = "Get all projects", description = "Retrieve a list of all projects")
    public ResponseEntity<List<ProjectResponse>> getAllProjects() {
        log.info("Fetching all projects");
        List<ProjectResponse> projects = projectService.getAllProjects();
        return ResponseEntity.ok(projects);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a project", description = "Update an existing project by its ID")
    public ResponseEntity<ProjectResponse> updateProject(
            @PathVariable Long id,
            @Valid @RequestBody ProjectRequest projectRequest) {
        log.info("Updating project with ID: {}", id);
        ProjectResponse updatedProject = projectService.updateProject(id, projectRequest);
        return ResponseEntity.ok(updatedProject);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a project", description = "Delete a project by its ID")
    public ResponseEntity<Void> deleteProject(@PathVariable Long id) {
        log.info("Deleting project with ID: {}", id);
        projectService.deleteProject(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/toggle-status")
    @Operation(summary = "Toggle project status", description = "Toggle project status between ACTIVE and INACTIVE")
    public ResponseEntity<ProjectResponse> toggleProjectStatus(@PathVariable Long id) {
        log.info("Toggling status for project with ID: {}", id);
        ProjectResponse updatedProject = projectService.toggleProjectStatus(id);
        return ResponseEntity.ok(updatedProject);
    }

    @GetMapping("/{id}/pom")
    @Operation(summary = "Get POM file content from default path",
            description = "Read the POM file from the project's default POM path")
    public ResponseEntity<?> getPomFileFromPath(@PathVariable Long id) {
        log.info("Attempting to read POM file for project ID: {}", id);

        // Get the project by ID
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with ID: " + id));

        // Check if project has a default POM path
        if (project.getDefaultPomPath() == null || project.getDefaultPomPath().isEmpty()) {
            log.warn("No default POM path set for project ID: {}", id);
            return ResponseEntity
                    .badRequest()
                    .body(Map.of("error", "No default POM path set for this project"));
        }

        // Construct the full path to pom.xml
        String pomPath = project.getDefaultPomPath();
        if (!pomPath.endsWith("pom.xml")) {
            pomPath = Paths.get(pomPath, "pom.xml").toString();
        }

        // Try to read the file
        try {
            Path path = Paths.get(pomPath);
            if (!Files.exists(path)) {
                log.warn("POM file not found at path: {}", pomPath);
                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "POM file not found at path: " + pomPath));
            }

            // Read the file content
            String content = Files.readString(path);

            // Return the content
            return ResponseEntity.ok(Map.of(
                    "content", content,
                    "path", pomPath
            ));
        } catch (IOException e) {
            log.error("Error reading POM file from path: {}", pomPath, e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error reading POM file: " + e.getMessage()));
        }
    }
} 