package com.muratcan.yeldan.mavenanalyzer.service;

import java.util.Map;
import java.util.Optional;

/**
 * Service for running Maven commands and extracting information from a Maven project
 */
public interface MavenCommandService {

    /**
     * Extract Maven project properties from a pom.xml in the specified directory
     *
     * @param pomDirectoryPath the directory containing the pom.xml file
     * @return a map of property names to their values, or empty Optional if command fails
     */
    Optional<Map<String, String>> extractMavenProperties(String pomDirectoryPath);

    /**
     * Check if the provided path contains a valid pom.xml file
     *
     * @param pomDirectoryPath the directory to check
     * @return true if the directory contains a valid pom.xml, false otherwise
     */
    boolean isValidPomDirectory(String pomDirectoryPath);
} 