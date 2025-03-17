package com.muratcan.yeldan.mavenanalyzer.controller;

import com.muratcan.yeldan.mavenanalyzer.dto.response.ReportResponse;
import com.muratcan.yeldan.mavenanalyzer.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Report Generation", description = "API endpoints for generating and downloading reports")
public class ReportController {

    private final ReportService reportService;

    @Value("${report.output.directory}")
    private String reportOutputDirectory;

    @GetMapping("/full/{analysisId}")
    @Operation(summary = "Generate full report", description = "Generate a full PDF report for a dependency analysis")
    public ResponseEntity<ReportResponse> generateFullReport(@PathVariable Long analysisId) {
        log.info("Generating full report for analysis ID: {}", analysisId);
        ReportResponse response = reportService.generateFullReport(analysisId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/download/{fileName}")
    @Operation(summary = "Download report", description = "Download a generated report file")
    public ResponseEntity<Resource> downloadReport(@PathVariable String fileName) {
        try {
            Path filePath = Paths.get(reportOutputDirectory).resolve(fileName);
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_PDF)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                        .body(resource);
            } else {
                log.error("Report file not found: {}", fileName);
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException e) {
            log.error("Error downloading report: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
} 