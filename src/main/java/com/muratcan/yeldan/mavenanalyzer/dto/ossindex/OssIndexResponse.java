package com.muratcan.yeldan.mavenanalyzer.dto.ossindex;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OssIndexResponse {

    private String coordinates;
    private String description;
    private String reference;
    private List<OssIndexVulnerability> vulnerabilities;
} 