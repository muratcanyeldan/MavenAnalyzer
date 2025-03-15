package com.muratcan.yeldan.mavenanalyzer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppSettingsRequest {

    private Boolean licenseCheckingEnabled;
    private List<String> restrictedLicenses;
    private Boolean vulnerabilityCheckingEnabled;
    private Integer vulnerabilityCheckDelay;
    private Boolean cacheEnabled;
    private Integer cacheDurationHours;
    private Boolean showNotifications;
} 