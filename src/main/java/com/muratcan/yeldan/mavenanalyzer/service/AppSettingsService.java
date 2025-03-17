package com.muratcan.yeldan.mavenanalyzer.service;

import com.muratcan.yeldan.mavenanalyzer.dto.request.AppSettingsRequest;
import com.muratcan.yeldan.mavenanalyzer.dto.response.AppSettingsResponse;

import java.util.List;

public interface AppSettingsService {

    AppSettingsResponse getSettings();

    AppSettingsResponse updateSettings(AppSettingsRequest request);

    List<String> getRestrictedLicenses();

    boolean isLicenseCheckingEnabled();
}