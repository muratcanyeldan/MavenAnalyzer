import axios from 'axios';

// Define API base URL with full URL path
const API_BASE_URL = process.env.NODE_ENV === 'production' 
  ? (process.env.REACT_APP_API_BASE_URL || 'http://localhost:8080/api')
  : 'http://localhost:8080/api'; // Always use absolute URL in development


// Create axios instance with default config
const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Add request interceptor for auth tokens, etc.
apiClient.interceptors.request.use(
  (config) => {
    // You can add auth tokens here when you implement authentication
    // const token = localStorage.getItem('token');
    // if (token) {
    //   config.headers.Authorization = `Bearer ${token}`;
    // }
    return config;
  },
  (error) => Promise.reject(error)
);

// Add response interceptor for error handling
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    // Handle common errors
    if (error.response) {
      // Server responded with error status
      console.error('API Error:', error.response.data);
      
      // Handle specific status codes
      switch (error.response.status) {
        case 401:
          // Handle unauthorized
          // Redirect to login or refresh token
          break;
        case 404:
          break;
        default:
          break;
      }
    } else if (error.request) {
      // Request made but no response
      console.error('No response received:', error.request);
    } else {
      // Error in request setup
      console.error('Request error:', error.message);
    }
    
    return Promise.reject(error);
  }
);

// API Methods
const api = {
  // Project endpoints
  projects: {
    getAll: () => apiClient.get('/projects'),
    getById: (id) => apiClient.get(`/projects/${id}`),
    create: (data) => apiClient.post('/projects', data),
    update: (id, data) => apiClient.put(`/projects/${id}`, data),
    delete: (id) => apiClient.delete(`/projects/${id}`),
    toggleStatus: (id) => apiClient.patch(`/projects/${id}/toggle-status`),
    getPomFromDefaultPath: (id) => apiClient.get(`/projects/${id}/pom`),
  },
  
  // Dependency analysis endpoints
  dependencyAnalysis: {
    getAll: () => apiClient.get('/analyses'),
    getById: (id) => apiClient.get(`/analyses/${id}`),
    getByProject: (projectId) => apiClient.get(`/analyses/project/${projectId}`),
    getLatestByProject: (projectId) => apiClient.get(`/analyses/project/${projectId}/latest`),
    create: (projectId, data) => apiClient.post(`/analyses/project/${projectId}`, data),
    getVulnerabilityStatus: (id) => apiClient.get(`/analyses/${id}/vulnerability-status`),
    updateDependencyVersion: (dependencyId, newVersion) => 
      apiClient.patch(`/analyses/dependencies/${dependencyId}/version?newVersion=${newVersion}`),
    downloadUpdatedPom: (analysisId) => {
      window.open(`${API_BASE_URL}/analyses/${analysisId}/updated-pom`, '_blank');
    },
    delete: (analysisId) => {
      // Ensure analysisId is a number
      const id = typeof analysisId === 'string' ? parseInt(analysisId, 10) : analysisId;
      
      if (isNaN(id)) {
        console.error('Invalid analysis ID for deletion:', analysisId);
        return Promise.reject(new Error('Invalid analysis ID format'));
      }
      

      return apiClient.delete(`/analyses/${id}`)
        .then(response => {
          return response;
        })
        .catch(error => {
          console.error('Delete request failed:', error);
          console.error('Request details:', {
            url: `${API_BASE_URL}/analyses/${id}`,
            method: 'DELETE',
            status: error.response?.status,
            statusText: error.response?.statusText,
            data: error.response?.data
          });
          throw error;
        });
    },
    uploadPom: (projectId, file) => {
      const formData = new FormData();
      formData.append('file', file);
      return apiClient.post(`/projects/${projectId}/pom-upload`, formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      });
    },
    // Initiates an analysis and handles immediate redirection to the analysis detail page
    startAnalysisAndRedirect: async (projectId, data, navigate, toastFn) => {
      try {
        let response;
        
        if (projectId) {
          // If we have a projectId, use the project-specific endpoint
          response = await apiClient.post(`/analyses/project/${projectId}`, data);
        } else {
          // If no projectId, use the general analyses endpoint
          response = await apiClient.post('/analyses', data);
        }
        
        const analysisId = response.data.id;
        if (!analysisId) {
          console.error('No analysis ID returned from API');
          if (toastFn) toastFn.error('Failed to start analysis: No analysis ID returned');
          return null;
        }

        // Show notification if provided
        if (toastFn) {
          toastFn.success('Analysis started! Redirecting to results page where vulnerability scanning will continue in the background.');
        }
        
        // Redirect immediately to the analysis detail page
        if (navigate) {
          navigate(`/analysis/${analysisId}`, { 
            state: { alert: { type: 'success', message: 'Analysis initiated successfully. Vulnerability scanning will continue in the background.' } }
          });
        }
        
        return response;
      } catch (error) {
        console.error('Error starting analysis:', error);
        if (toastFn) toastFn.error(`Failed to start analysis: ${error.message || 'Unknown error'}`);
        throw error;
      }
    }
  },
  
  // Dashboard endpoints
  dashboard: {
    getStats: () => apiClient.get('/dashboard/stats')
  },
  
  // Chart endpoints
  charts: {
    getDependencyUpdates: (analysisId) => apiClient.get(`/charts/dependency-updates/${analysisId}`),
    getVulnerabilities: (analysisId) => apiClient.get(`/charts/vulnerabilities/${analysisId}`),
    getLicenseDistribution: (analysisId) => apiClient.get(`/charts/license-distribution/${analysisId}`),
    getDependencyStatusData: (analysisId) => apiClient.get(`/charts/data/dependency-status/${analysisId}`),
    getVulnerabilityStatusData: (analysisId) => apiClient.get(`/charts/data/vulnerability-status/${analysisId}`),
    getVulnerabilitySeverityData: (analysisId) => apiClient.get(`/charts/data/vulnerability-severity/${analysisId}`),
    getLicenseDistributionData: (analysisId) => apiClient.get(`/charts/data/license-distribution/${analysisId}`),
  },
  
  // Report endpoints
  reports: {
    generateFullReport: (analysisId) => apiClient.get(`/reports/full/${analysisId}`),
    downloadReport: (fileName) => {
      window.open(`${API_BASE_URL}/reports/download/${fileName}`, '_blank');
    }
  },
  
  // Settings endpoints
  settings: {
    getSettings: () => {
      return apiClient.get('/settings');
    },
    updateSettings: (data) => {
      return apiClient.put('/settings', data);
    },
  },
  
  // Cache management endpoints
  cache: {
    clearAll: () => apiClient.delete('/api/cache'),
    clearVulnerabilities: () => apiClient.delete('/api/cache/vulnerability'),
    clearLicenses: () => apiClient.delete('/api/cache/license'),
    clearCharts: () => apiClient.delete('/api/cache/chart'),
    clearVersions: () => apiClient.delete('/api/cache/version'),
    getStatus: () => apiClient.get('/api/cache/status'),
    toggleCaching: (enabled) => apiClient.put(`/api/cache/toggle?enabled=${enabled}`)
  },
};

export default api; 