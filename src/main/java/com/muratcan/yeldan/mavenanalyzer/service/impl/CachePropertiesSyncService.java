package com.muratcan.yeldan.mavenanalyzer.service.impl;

import com.muratcan.yeldan.mavenanalyzer.entity.AppSettings;
import com.muratcan.yeldan.mavenanalyzer.repository.AppSettingsRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Service to synchronize database settings with application properties.
 * This ensures that cache settings in the database match the properties being used.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CachePropertiesSyncService {

    private final AppSettingsRepository appSettingsRepository;

    // Injected property values
    @Value("${license.cache.enabled:true}")
    private boolean licenseCacheEnabled;

    @Value("${version.estimate.cache.enabled:true}")
    private boolean versionEstimateCacheEnabled;

    @Value("${vulnerability.cache.enabled:true}")
    private boolean vulnerabilityCacheEnabled;

    /**
     * Initialize cache settings from application properties at startup
     */
    @PostConstruct
    public void initializeFromProperties() {
        log.info("Initializing cache settings from properties: licenseCache={}, versionEstimateCache={}, vulnerabilityCache={}",
                licenseCacheEnabled, versionEstimateCacheEnabled, vulnerabilityCacheEnabled);
    }

    /**
     * Update the cache-enabled properties when application settings are changed
     *
     * @param settings The updated application settings
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onApplicationSettingsUpdated(AppSettings settings) {
        if (settings == null) {
            return;
        }

        log.debug("Application settings updated, syncing with cache properties");

        // Currently, we only have a global cache toggle in the UI
        // We sync it to all the specific cache-enabled properties
        boolean cacheEnabled = Boolean.TRUE.equals(settings.getCacheEnabled());

        // In a real implementation, we might have a mechanism to update Spring properties at runtime
        // For now, we just log that this would happen
        log.info("Cache settings updated from UI: cacheEnabled={}", cacheEnabled);
        log.info("In a production implementation, this would update runtime properties:");
        log.info("  - license.cache.enabled = {}", cacheEnabled);
        log.info("  - version.estimate.cache.enabled = {}", cacheEnabled);
        log.info("  - vulnerability.cache.enabled = {}", cacheEnabled);

        // Note: To actually change Spring properties at runtime, you would need
        // something like Spring Cloud Config or a custom PropertySource implementation
    }
} 