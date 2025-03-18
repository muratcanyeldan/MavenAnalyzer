# Maven Dependency Analyzer

A tool for checking updates for Maven dependencies in POM files. This application helps you identify outdated dependencies, track dependency update history, and visualize dependency status through charts.

## Features

- **POM Dependency Analysis**: Parse and analyze dependencies in POM files
- **Dependency Update Check**: Identify outdated dependencies
- **Vulnerability Detection**: Identify potentially vulnerable dependencies
- **Historical Tracking**: Track dependency status changes over time
- **Visualization**: Generate charts to visualize dependency status
- **Project Management**: Organize analyses by projects
- **Environment-Aware**: Automatically adapts to Docker or local environment
- **REST API**: Full-featured RESTful API for integration with other tools

## Technology Stack

- Java 21
- Spring Boot 3
- MySQL 8
- Redis
- JFreeChart
- Maven
- Docker

## Getting Started

### Prerequisites

- Java 21
- Docker and Docker Compose (for containerized deployment)
- Maven (optional, for local development)

### Running with Docker (Fully Containerized)

1. Clone the repository:
   ```
   git clone https://github.com/yourusername/maven-analyzer.git
   cd maven-analyzer
   ```

2. Build and run the application using Docker Compose:
   ```
   docker compose up -d
   ```

3. Access the application:
   - API: http://localhost:8080/api
   - UI: http://localhost:3000
   - Swagger UI: http://localhost:8080/swagger-ui/index.html

In Docker mode, the system automatically creates a temporary Maven project for each analysis, so there's no need for filesystem access to your local projects.

### Running in Hybrid Mode (Backend on Host, Other Services in Docker)

This approach is useful when you want to analyze Maven projects on your local filesystem:

1. Run UI, MySQL, and Redis with Docker Compose:
   ```
   docker compose -f docker-compose.local.yml up -d
   ```

2. Run the backend locally:
   ```
   ./mvnw spring-boot:run
   ```

3. Access the application:
   - API: http://localhost:8080/api
   - UI: http://localhost:3000
   - Swagger UI: http://localhost:8080/swagger-ui/index.html

## Environment-aware POM Handling

The application automatically detects whether it's running in a Docker container or on a local machine, and adapts its behavior accordingly:

### Docker Environment (Automatic Mode)

When running in Docker:
- The system automatically creates a temporary Maven project structure for each POM analysis
- No local file system paths are required from the user
- All dependencies are properly resolved using this temporary project structure
- The temporary project is automatically cleaned up after analysis
- The UI automatically adapts to hide file path input fields

### Local Environment (Hybrid Mode)

When running on a local machine:
- The UI shows options for providing a local file system path to the directory containing your POM file
- This allows for more accurate resolution of dependencies, especially those managed by BOMs
- If no path is provided but transitive dependencies are required, the application falls back to creating a temporary project
- You can analyze both local and remote POM files

This adaptive approach ensures the application works optimally in any deployment scenario.

## Usage Workflow

1. **Create Project** (optional): Create a project to organize related analyses
2. **Upload/Paste POM**: Either upload a POM file or paste its content
3. **Configure Analysis**: Choose analysis options (vulnerability checks, transitive dependencies, etc.)
4. **View Results**: See dependency status, available updates, and potential vulnerabilities
5. **Generate Reports**: Create visual reports of dependency status

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

### Environment Information

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/environment` | Get information about the runtime environment |

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
    "checkVulnerabilities": true,
    "pomDirectoryPath": "/optional/path/to/project" 
  }'
```

### Getting Analysis History

```bash
curl -X GET 'http://localhost:8080/api/analyses/project/1'
```

## How It Works

1. **Environment Detection**: The application detects whether it's running in Docker or on a local machine
2. **POM Handling**: 
   - In Docker: Creates a temporary Maven project with the provided POM content
   - On local machine: Can use paths to existing projects or create temporary projects
3. **POM Parsing**: Extracts dependency information including versions
4. **Version Checking**: Uses the Maven Repository API to check for newer versions
5. **Analysis**: Categorizes dependencies as up-to-date, outdated, or unidentified
6. **Vulnerability Scanning**: Checks dependencies against known vulnerabilities (demo feature)
7. **Visualization**: Generates charts showing the distribution of dependency statuses
8. **Notification**: Can send notifications when analysis completes

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgments

- This application uses the [Maven Repository API](https://central.sonatype.org/search/rest-api-guide/) for retrieving dependency information
- Charts are generated using [JFreeChart](https://www.jfree.org/jfreechart/) 