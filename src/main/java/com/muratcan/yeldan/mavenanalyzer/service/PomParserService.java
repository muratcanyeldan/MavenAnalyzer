package com.muratcan.yeldan.mavenanalyzer.service;

import com.muratcan.yeldan.mavenanalyzer.dto.DependencyInfo;

import java.util.List;

/**
 * Service for parsing POM files
 */
public interface PomParserService {

    /**
     * Parse dependencies from a POM file
     *
     * @param pomContent The content of the POM file
     * @return A list of dependencies
     */
    List<DependencyInfo> parsePomDependencies(String pomContent);

    /**
     * Parse dependencies from a POM file and resolve BOM-managed dependencies if a POM directory is provided
     *
     * @param pomContent   The content of the POM file
     * @param pomDirectory Optional directory containing the pom.xml file for resolving BOM versions
     * @return A list of dependencies
     */
    List<DependencyInfo> parsePomDependencies(String pomContent, String pomDirectory);

    /**
     * Parse dependencies from a POM file and control over transitive dependencies
     *
     * @param pomContent        The content of the POM file
     * @param pomDirectory      Optional directory containing the pom.xml file for resolving BOM versions
     * @param includeTransitive Whether to include transitive dependencies in the analysis
     * @return A list of dependencies
     */
    List<DependencyInfo> parsePomDependencies(String pomContent, String pomDirectory, boolean includeTransitive);

    /**
     * Check if the content is a valid POM file
     *
     * @param pomContent The content to check
     * @return true if the content is a valid POM file, false otherwise
     */
    boolean isValidPom(String pomContent);
} 