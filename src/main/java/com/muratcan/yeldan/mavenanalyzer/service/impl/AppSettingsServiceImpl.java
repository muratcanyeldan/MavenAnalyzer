package com.muratcan.yeldan.mavenanalyzer.service.impl;

import com.muratcan.yeldan.mavenanalyzer.dto.AppSettingsRequest;
import com.muratcan.yeldan.mavenanalyzer.dto.AppSettingsResponse;
import com.muratcan.yeldan.mavenanalyzer.entity.AppSettings;
import com.muratcan.yeldan.mavenanalyzer.repository.AppSettingsRepository;
import com.muratcan.yeldan.mavenanalyzer.service.AppSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppSettingsServiceImpl implements AppSettingsService {

    private static final String DEFAULT_SETTINGS_KEY = "application";
    private static final List<String> DEFAULT_RESTRICTED_LICENSES = Arrays.asList("GPL", "AGPL");

    private final AppSettingsRepository appSettingsRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(readOnly = true)
    public AppSettingsResponse getSettings() {
        AppSettings settings = getOrCreateSettings();
        return mapToResponse(settings);
    }

    @Override
    @Transactional
    public AppSettingsResponse updateSettings(AppSettingsRequest request) {
        AppSettings settings = getOrCreateSettings();
        boolean cacheSettingsChanged = false;

        // Update settings with request data
        if (request.getLicenseCheckingEnabled() != null) {
            settings.setLicenseCheckingEnabled(request.getLicenseCheckingEnabled());
        }

        if (request.getRestrictedLicenses() != null) {
            settings.getRestrictedLicenses().clear();
            settings.getRestrictedLicenses().addAll(request.getRestrictedLicenses());
        }

        if (request.getVulnerabilityCheckingEnabled() != null) {
            settings.setVulnerabilityCheckingEnabled(request.getVulnerabilityCheckingEnabled());
        }

        if (request.getVulnerabilityCheckDelay() != null) {
            settings.setVulnerabilityCheckDelay(request.getVulnerabilityCheckDelay());
        }

        if (request.getCacheEnabled() != null) {
            cacheSettingsChanged = !request.getCacheEnabled().equals(settings.getCacheEnabled());
            settings.setCacheEnabled(request.getCacheEnabled());
        }

        if (request.getCacheDurationHours() != null) {
            cacheSettingsChanged = cacheSettingsChanged || !request.getCacheDurationHours().equals(settings.getCacheDurationHours());
            settings.setCacheDurationHours(request.getCacheDurationHours());
        }

        if (request.getShowNotifications() != null) {
            settings.setShowNotifications(request.getShowNotifications());
        }

        settings = appSettingsRepository.save(settings);
        log.info("Updated application settings with ID: {}", settings.getId());

        // Publish event to notify cache properties sync service if cache settings changed
        if (cacheSettingsChanged) {
            log.debug("Cache settings changed, publishing settings updated event");
            eventPublisher.publishEvent(settings);
        }

        return mapToResponse(settings);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getRestrictedLicenses() {
        AppSettings settings = getOrCreateSettings();
        return new ArrayList<>(settings.getRestrictedLicenses());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isLicenseCheckingEnabled() {
        AppSettings settings = getOrCreateSettings();
        return Boolean.TRUE.equals(settings.getLicenseCheckingEnabled());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isLicenseRestricted(String license) {
        if (!isLicenseCheckingEnabled() || license == null) {
            return false;
        }

        List<String> restrictedLicenses = getRestrictedLicenses();

        // Consider "unknown" license as restricted
        if ("unknown".equalsIgnoreCase(license)) {
            return true;
        }

        // Normalize license name (to uppercase) and check if it contains any of the restricted licenses
        String normalizedLicense = license.toUpperCase();
        return restrictedLicenses.stream()
                .map(String::toUpperCase)
                .anyMatch(normalizedLicense::contains);
    }

    @Override
    @Transactional(readOnly = true)
    public int countLicenseIssues(List<String> licenses) {
        if (!isLicenseCheckingEnabled() || licenses == null || licenses.isEmpty()) {
            return 0;
        }

        return (int) licenses.stream()
                .filter(this::isLicenseRestricted)
                .count();
    }

    private AppSettings getOrCreateSettings() {
        Optional<AppSettings> settingsOpt = appSettingsRepository.findByKey(DEFAULT_SETTINGS_KEY);

        if (settingsOpt.isPresent()) {
            return settingsOpt.get();
        } else {
            // Create default settings
            AppSettings newSettings = AppSettings.builder()
                    .key(DEFAULT_SETTINGS_KEY)
                    .name("Application Settings")
                    .licenseCheckingEnabled(true)
                    .restrictedLicenses(new ArrayList<>(DEFAULT_RESTRICTED_LICENSES))
                    .vulnerabilityCheckingEnabled(true)
                    .vulnerabilityCheckDelay(0)
                    .cacheEnabled(true)
                    .cacheDurationHours(24)
                    .showNotifications(true)
                    .build();

            AppSettings savedSettings = appSettingsRepository.save(newSettings);
            log.info("Created default application settings with ID: {}", savedSettings.getId());

            return savedSettings;
        }
    }

    private AppSettingsResponse mapToResponse(AppSettings settings) {
        return AppSettingsResponse.builder()
                .id(settings.getId())
                .key(settings.getKey())
                .name(settings.getName())
                .licenseCheckingEnabled(settings.getLicenseCheckingEnabled())
                .restrictedLicenses(new ArrayList<>(settings.getRestrictedLicenses()))
                .vulnerabilityCheckingEnabled(settings.getVulnerabilityCheckingEnabled())
                .vulnerabilityCheckDelay(settings.getVulnerabilityCheckDelay())
                .cacheEnabled(settings.getCacheEnabled())
                .cacheDurationHours(settings.getCacheDurationHours())
                .showNotifications(settings.getShowNotifications())
                .updatedAt(settings.getUpdatedAt())
                .build();
    }
} 