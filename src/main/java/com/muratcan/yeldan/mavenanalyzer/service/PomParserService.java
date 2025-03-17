package com.muratcan.yeldan.mavenanalyzer.service;

import com.muratcan.yeldan.mavenanalyzer.dto.DependencyInfo;

import java.util.List;

public interface PomParserService {

    /**
     * Parse dependencies from a POM file and control over transitive dependencies
     *
     * @param pomContent        The content of the POM file
     * @param pomDirectory      Optional directory containing the pom.xml file for resolving BOM versions
     * @param includeTransitive Whether to include transitive dependencies in the analysis
     * @return A list of dependencies
     */
    List<DependencyInfo> parsePomDependencies(String pomContent, String pomDirectory, boolean includeTransitive);
} 