package com.muratcan.yeldan.mavenanalyzer.service;

import com.muratcan.yeldan.mavenanalyzer.dto.AppSettingsRequest;
import com.muratcan.yeldan.mavenanalyzer.dto.AppSettingsResponse;

import java.util.List;

public interface AppSettingsService {

    AppSettingsResponse getSettings();

    AppSettingsResponse updateSettings(AppSettingsRequest request);

    List<String> getRestrictedLicenses();

    boolean isLicenseCheckingEnabled();

    boolean isLicenseRestricted(String license);

    int countLicenseIssues(List<String> licenses);
} 