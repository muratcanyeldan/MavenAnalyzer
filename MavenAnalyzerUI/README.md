# Maven Dependency Analyzer UI

A modern React-based web interface for the Maven Dependency Analyzer application.

## Features

- **Dashboard**: Overview of projects, dependencies, and their statuses
- **Project Management**: Create, view and manage Maven projects
- **Dependency Analysis**: Analyze Maven dependencies for updates, vulnerabilities, and license issues
- **File Upload**: Upload POM files for analysis
- **Visualizations**: View charts and graphs showing dependency status
- **Analysis History**: Track previous analyses and their results

## Getting Started

### Prerequisites

- Node.js (v14 or higher)
- npm (v6 or higher)

### Installation

1. Clone the repository:
   ```
   git clone https://github.com/yourusername/MavenAnalyzer.git
   cd MavenAnalyzerUI
   ```

2. Install dependencies:
   ```
   npm install
   ```

3. Start the development server:
   ```
   npm start
   ```

The application will start at http://localhost:3000.

### Configuration

The UI connects to the Maven Dependency Analyzer API. By default, it expects the API to be running at `http://localhost:8080`. You can change this by modifying the `proxy` setting in `package.json` or by setting the `REACT_APP_API_BASE_URL` environment variable.

## Build for Production

To build the application for production:

```
npm run build
```

This creates an optimized production build in the `build` folder that can be deployed to a web server.

## Technologies Used

- React 18
- React Router 6
- Material UI 5
- Axios for API communication
- Chart.js and React-Chartjs-2 for data visualization

## Project Structure

- `/src/components`: Reusable UI components
- `/src/pages`: Page components for each route
- `/src/services`: API services and utilities
- `/src/assets`: Static assets like images and icons

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the MIT License. 