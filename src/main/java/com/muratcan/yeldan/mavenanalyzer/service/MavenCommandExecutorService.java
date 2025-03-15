package com.muratcan.yeldan.mavenanalyzer.service;

import java.util.Map;
import java.util.Optional;

/**
 * Service for executing Maven commands and parsing the output
 */
public interface MavenCommandExecutorService {

    /**
     * Execute Maven help:evaluate command to get the project properties
     *
     * @param pomDirectory The directory containing the pom.xml file
     * @return A map of property names to values
     */
    Map<String, String> getProjectProperties(String pomDirectory);

    /**
     * Resolve actual version of a dependency that is managed by BOM
     *
     * @param groupId      The Maven artifact group ID
     * @param artifactId   The Maven artifact ID
     * @param pomDirectory The directory containing the pom.xml file
     * @return An Optional containing the resolved version if found, empty otherwise
     */
    Optional<String> resolveManagedDependencyVersion(String groupId, String artifactId, String pomDirectory);
} 