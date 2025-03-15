package com.muratcan.yeldan.mavenanalyzer.dto.ossindex;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for the OSS Index API to check for vulnerabilities
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OssIndexRequest {

    private List<String> coordinates;
} 