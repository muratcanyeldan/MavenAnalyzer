-- Projects table to store information about each project that is analyzed
CREATE TABLE projects (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_project_name UNIQUE (name)
);

-- Analysis table to store each analysis run
CREATE TABLE dependency_analyses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    pom_content LONGTEXT NOT NULL,
    analysis_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    total_dependencies INT NOT NULL,
    outdated_dependencies INT NOT NULL,
    up_to_date_dependencies INT NOT NULL,
    unidentified_dependencies INT NOT NULL,
    chart_path VARCHAR(512),
    CONSTRAINT fk_analysis_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);

-- Dependencies table to store information about each dependency in each analysis
CREATE TABLE dependencies (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    analysis_id BIGINT NOT NULL,
    group_id VARCHAR(255) NOT NULL,
    artifact_id VARCHAR(255) NOT NULL,
    current_version VARCHAR(100) NOT NULL,
    latest_version VARCHAR(100),
    is_outdated BOOLEAN DEFAULT FALSE,
    is_vulnerable BOOLEAN DEFAULT FALSE,
    versions_behind INT DEFAULT 0,
    vulnerable_count INT DEFAULT 0,
    CONSTRAINT fk_dependency_analysis FOREIGN KEY (analysis_id) REFERENCES dependency_analyses(id) ON DELETE CASCADE
);

-- Vulnerabilities table to store information about vulnerabilities for each dependency
CREATE TABLE vulnerabilities (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    dependency_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    severity VARCHAR(50),
    affected_versions VARCHAR(255),
    fixed_in_version VARCHAR(100),
    CONSTRAINT fk_vulnerability_dependency FOREIGN KEY (dependency_id) REFERENCES dependencies(id) ON DELETE CASCADE
);

-- Create index for faster queries
CREATE INDEX idx_project_id ON dependency_analyses(project_id);
CREATE INDEX idx_analysis_id ON dependencies(analysis_id);
CREATE INDEX idx_dependency_id ON vulnerabilities(dependency_id);
CREATE INDEX idx_group_artifact ON dependencies(group_id, artifact_id); 