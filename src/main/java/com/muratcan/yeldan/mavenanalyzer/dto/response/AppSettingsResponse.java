package com.muratcan.yeldan.mavenanalyzer.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppSettingsResponse {

    private Long id;
    private String key;
    private String name;
    private Boolean licenseCheckingEnabled;
    private List<String> restrictedLicenses;
    private Boolean vulnerabilityCheckingEnabled;
    private Integer vulnerabilityCheckDelay;
    private Boolean cacheEnabled;
    private Integer cacheDurationHours;
    private Boolean showNotifications;
    private LocalDateTime updatedAt;
} 