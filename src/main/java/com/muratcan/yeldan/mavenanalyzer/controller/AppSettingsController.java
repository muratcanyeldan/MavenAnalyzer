package com.muratcan.yeldan.mavenanalyzer.controller;

import com.muratcan.yeldan.mavenanalyzer.dto.AppSettingsRequest;
import com.muratcan.yeldan.mavenanalyzer.dto.AppSettingsResponse;
import com.muratcan.yeldan.mavenanalyzer.service.AppSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/settings")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Settings", description = "Endpoints for managing application settings")
public class AppSettingsController {

    private final AppSettingsService appSettingsService;

    @GetMapping
    @Operation(
            summary = "Get application settings",
            description = "Retrieve all application settings including license checking configuration"
    )
    public ResponseEntity<AppSettingsResponse> getSettings() {
        log.info("GET /settings - Fetching application settings");
        AppSettingsResponse settings = appSettingsService.getSettings();
        return ResponseEntity.ok(settings);
    }

    @PutMapping
    @Operation(
            summary = "Update application settings",
            description = "Update application settings including license checking configuration"
    )
    public ResponseEntity<AppSettingsResponse> updateSettings(@RequestBody AppSettingsRequest request) {
        log.info("PUT /settings - Updating application settings: {}", request);
        AppSettingsResponse updated = appSettingsService.updateSettings(request);
        return ResponseEntity.ok(updated);
    }
} 