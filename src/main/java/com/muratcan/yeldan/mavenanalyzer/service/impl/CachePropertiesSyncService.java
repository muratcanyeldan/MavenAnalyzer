package com.muratcan.yeldan.mavenanalyzer.service.impl;

import com.muratcan.yeldan.mavenanalyzer.config.DynamicCacheProperties;
import com.muratcan.yeldan.mavenanalyzer.entity.AppSettings;
import com.muratcan.yeldan.mavenanalyzer.service.CacheManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
@RequiredArgsConstructor
@Slf4j
public class CachePropertiesSyncService {

    private final DynamicCacheProperties dynamicCacheProperties;
    private final CacheManagementService cacheManagementService;

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

        boolean cacheEnabled = Boolean.TRUE.equals(settings.getCacheEnabled());
        dynamicCacheProperties.updateAllCacheProperties(cacheEnabled);

        if (!cacheEnabled) {
            log.info("Caching was disabled, clearing all existing cache entries");
            cacheManagementService.clearAllCaches();
        }

        log.info("Cache settings successfully updated at runtime: enabled={}", cacheEnabled);
    }
} 