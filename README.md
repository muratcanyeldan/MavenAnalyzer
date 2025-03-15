# Maven Dependency Analyzer

A tool for checking updates for Maven dependencies in POM files. This application helps you identify outdated dependencies, track dependency update history, and visualize dependency status through charts.

## Features

- **POM Dependency Analysis**: Parse and analyze dependencies in POM files
- **Dependency Update Check**: Identify outdated dependencies and see how many versions behind they are
- **Vulnerability Detection**: Identify potentially vulnerable dependencies (demonstration feature)
- **Historical Tracking**: Track dependency status changes over time
- **Visualization**: Generate charts to visualize dependency status
- **Project Management**: Organize analyses by projects
- **REST API**: Full-featured RESTful API for integration with other tools

## Technology Stack

- Java 21
- Spring Boot 3.4.x
- MySQL 8.0
- JFreeChart 1.5.4 (for visualization)
- Maven
- Docker

## Getting Started

### Prerequisites

- Java 21
- Docker and Docker Compose (for containerized deployment)
- Maven (optional, for local development)

### Running with Docker

1. Clone the repository:
   ```
   git clone https://github.com/yourusername/maven-analyzer.git
   cd maven-analyzer
   ```

2. Build and run the application using Docker Compose:
   ```
   docker-compose up -d
   ```

3. Access the application:
   - API: http://localhost:8080/api
   - Swagger UI: http://localhost:8080/api/swagger-ui.html

### Running Locally

1. Clone the repository:
   ```
   git clone https://github.com/yourusername/maven-analyzer.git
   cd maven-analyzer
   ```

2. Make sure MySQL is running and update `application.properties` with your database configuration.

3. Build and run the application:
   ```
   ./mvnw spring-boot:run
   ```

## API Endpoints

### Project Management

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/projects` | Create a new project |
| GET | `/api/projects` | Get all projects |
| GET | `/api/projects/{id}` | Get a project by ID |
| GET | `/api/projects/name/{name}` | Get a project by name |
| PUT | `/api/projects/{id}` | Update a project |
| DELETE | `/api/projects/{id}` | Delete a project |

### Dependency Analysis

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/analyses` | Analyze dependencies in a POM file |
| GET | `/api/analyses/{id}` | Get analysis results by ID |
| GET | `/api/analyses/project/{projectId}` | Get analysis history for a project |
| GET | `/api/analyses/project/{projectId}/latest` | Get the latest analysis for a project |
| DELETE | `/api/analyses/{id}` | Delete an analysis |

## Usage Examples

### Creating a Project

```bash
curl -X POST 'http://localhost:8080/api/projects' \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "My Project",
    "description": "Sample project for testing"
  }'
```

### Analyzing Dependencies

```bash
curl -X POST 'http://localhost:8080/api/analyses' \
  -H 'Content-Type: application/json' \
  -d '{
    "projectId": 1,
    "pomContent": "<?xml version=\"1.0\" encoding=\"UTF-8\"?><project>...</project>",
    "checkVulnerabilities": true
  }'
```

### Getting Analysis History

```bash
curl -X GET 'http://localhost:8080/api/analyses/project/1'
```

## How It Works

1. **POM Parsing**: The application parses POM files and extracts dependency information, including versions.
2. **Version Checking**: It uses the Maven Repository API to check for newer versions of dependencies.
3. **Analysis**: It categorizes dependencies as up-to-date, outdated, or unidentified.
4. **Visualization**: It generates charts showing the distribution of dependency statuses.
5. **History**: It tracks changes in dependency status over time.

## Known Limitations

- Version comparison is simplified and may not fully match Maven's resolution logic in complex cases
- Vulnerability detection is a demonstration feature and not based on real security databases
- The application does not currently handle multi-module Maven projects

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgments

- This application uses the [Maven Repository API](https://central.sonatype.org/search/rest-api-guide/) for retrieving dependency information.
- Charts are generated using [JFreeChart](https://www.jfree.org/jfreechart/). 