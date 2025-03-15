import React, { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Typography,
  Paper,
  Box,
  Button,
  TextField,
  CircularProgress,
  Stepper,
  Step,
  StepLabel,
  Grid,
  Card,
  CardContent,
  Divider,
  Alert,
  Chip,
  IconButton,
} from '@mui/material';
import CloudUploadIcon from '@mui/icons-material/CloudUpload';
import CodeIcon from '@mui/icons-material/Code';
import DescriptionIcon from '@mui/icons-material/Description';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import api from '../services/api';
import { toast } from 'react-hot-toast';

const steps = ['Select Input Method', 'Configure Analysis', 'Review & Start'];

const NewAnalysis = () => {
  const { id: projectId } = useParams();
  const navigate = useNavigate();
  const fileInputRef = useRef(null);
  
  const [activeStep, setActiveStep] = useState(0);
  const [loading, setLoading] = useState(true);
  const [project, setProject] = useState(null);
  const [inputMethod, setInputMethod] = useState('file'); // 'file' or 'manual'
  const [pomFile, setPomFile] = useState(null);
  const [pomContent, setPomContent] = useState('');
  const [pomDirectoryPath, setPomDirectoryPath] = useState('');
  const [analysisOptions, setAnalysisOptions] = useState({
    checkVulnerabilities: true,
    checkLicenses: true,
    includeTransitive: true,
    notifyOnCompletion: false,
    resolveBomVersions: false,
  });
  const [error, setError] = useState(null);
  const [usesDefaultPomPath, setUsesDefaultPomPath] = useState(false);
  const [loadingPomContent, setLoadingPomContent] = useState(false);

  useEffect(() => {
    // Actually fetch the project data from the API
    const fetchProject = async () => {
      if (!projectId) {
        // No project ID provided, just set loading to false
        setProject(null);
        setLoading(false);
        return;
      }
      
      try {
        const response = await api.projects.getById(projectId);
        setProject(response.data);
        
        // If project has a defaultPomPath, use it
        if (response.data.defaultPomPath) {
          console.log('Found default POM path:', response.data.defaultPomPath);
          setPomDirectoryPath(response.data.defaultPomPath);
          setUsesDefaultPomPath(true);
          
          // Try to load POM content from the default path
          setLoadingPomContent(true);
          setError(null); // Clear any previous errors
          
          try {
            console.log('Attempting to load POM from default path...');
            const pomResponse = await api.projects.getPomFromDefaultPath(projectId);
            
            if (pomResponse.data && pomResponse.data.content) {
              console.log('Successfully loaded POM content, content length:', pomResponse.data.content.length);
              setPomContent(pomResponse.data.content);
              
              // Make sure we're properly setting the flag for using default path
              setUsesDefaultPomPath(true);
              
              // Skip to Configure Analysis step
              setActiveStep(1);
            } else {
              console.error('No content in POM response:', pomResponse);
              setError('No content found in POM file');
              setUsesDefaultPomPath(false);
            }
          } catch (err) {
            console.error('Error loading POM from default path:', err);
            const errorMessage = err.response?.data?.error || err.message || 'Failed to load POM file';
            setError(errorMessage);
            setUsesDefaultPomPath(false);
            // Don't reset active step - let user see the error
          }
        } else {
          console.log('No default POM path found for project');
        }
      } catch (error) {
        console.error('Error fetching project:', error);
        setError('Failed to load project details');
        setProject({
          id: projectId,
          name: 'Unknown Project',
          description: 'Project details could not be loaded.'
        });
      } finally {
        setLoading(false);
        setLoadingPomContent(false);
      }
    };

    fetchProject();
  }, [projectId]);

  const handleBack = () => {
    if (error) {
      setError(null);
    }
    
    // If going back from Configure Analysis to Select Input Method with POM content
    if (activeStep === 1 && pomContent) {
      // Set input method to manual to show the content in text area
      setInputMethod('manual');
    }
    
    setActiveStep((prevStep) => prevStep - 1);
  };

  const handleNext = () => {
    if (activeStep === 0 && usesDefaultPomPath && !pomContent) {
      // If we're using default POM path but don't have content yet, try loading it again
      setLoadingPomContent(true);
      setError(null);
      
      api.projects.getPomFromDefaultPath(projectId)
        .then(response => {
          if (response.data && response.data.content) {
            setPomContent(response.data.content);
            setActiveStep(1);
          } else {
            setError('No content found in POM file');
            setUsesDefaultPomPath(false);
          }
        })
        .catch(err => {
          console.error('Error loading POM from default path:', err);
          const errorMessage = err.response?.data?.error || err.message || 'Failed to load POM file';
          setError(errorMessage);
          setUsesDefaultPomPath(false);
        })
        .finally(() => {
          setLoadingPomContent(false);
        });
    } else {
      setActiveStep((prevStep) => prevStep + 1);
    }
  };

  const handleFileUpload = (event) => {
    const file = event.target.files[0];
    if (file) {
      setPomFile(file);
      // Read file content
      const reader = new FileReader();
      reader.onload = (e) => {
        setPomContent(e.target.result);
      };
      reader.readAsText(file);
    }
  };

  const handleFileClick = () => {
    fileInputRef.current.click();
  };

  const handleOptionChange = (option) => {
    setAnalysisOptions((prev) => ({
      ...prev,
      [option]: !prev[option],
    }));
  };

  const submitAnalysis = async () => {
    // Set active step to Review & Start Analysis
    setActiveStep(2);
    setLoading(true);
    setError(null);
    
    // If we don't have POM content but we do have a default path, try to load it
    if (!pomContent && usesDefaultPomPath && project?.defaultPomPath) {
      try {
        console.log('Attempting to load POM from default path before analysis...');
        const pomResponse = await api.projects.getPomFromDefaultPath(projectId);
        
        if (pomResponse.data && pomResponse.data.content) {
          console.log('Successfully loaded POM content for analysis');
          setPomContent(pomResponse.data.content);
        } else {
          console.error('No content in POM response:', pomResponse);
          setError('No content found in POM file');
          setLoading(false);
          return;
        }
      } catch (err) {
        console.error('Error loading POM from default path:', err);
        const errorMessage = err.response?.data?.error || err.message || 'Failed to load POM file';
        setError(errorMessage);
        setLoading(false);
        return;
      }
    }
    
    // Check if we have the POM content
    if (!pomContent) {
      setError('Please provide POM content before starting analysis');
      setLoading(false);
      return;
    }
    
    try {
      const analysisData = {
        pomContent: pomContent,
        checkVulnerabilities: analysisOptions.checkVulnerabilities,
        checkLicenses: analysisOptions.checkLicenses,
        includeTransitive: analysisOptions.includeTransitive,
        notifyOnCompletion: analysisOptions.notifyOnCompletion
      };
      
      // Only include projectId if it exists
      if (projectId) {
        analysisData.projectId = projectId;
      }
      
      // Add pomDirectoryPath if BOM resolution is enabled
      if (analysisOptions.resolveBomVersions && pomDirectoryPath.trim()) {
        analysisData.pomDirectoryPath = pomDirectoryPath;
      }
      
      console.log('Submitting analysis request:', analysisData);
      
      const response = await api.dependencyAnalysis.create(projectId, analysisData);
      
      toast.success('Analysis started successfully!');
      
      // Navigate to the analysis results page
      navigate(`/analysis/${response.data.id}`);
    } catch (err) {
      console.error('Error submitting analysis:', err);
      
      // Determine error message based on response
      let errorMessage = 'An unexpected error occurred. Please try again later.';
      
      if (err.response) {
        // Server responded with a non-2xx status
        if (err.response.status === 400) {
          errorMessage = 'Invalid request. Please check your POM file and try again.';
        } else if (err.response.status === 404) {
          errorMessage = 'Project not found. Please return to the projects page and try again.';
        } else if (err.response.data && err.response.data.message) {
          errorMessage = err.response.data.message;
        }
        
        console.error('Server error response:', err.response.data);
      } else if (err.request) {
        // Request was made but no response received
        errorMessage = 'No response from server. Please check your connection and try again.';
      }
      
      setError(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', mt: 10 }}>
        <CircularProgress />
      </Box>
    );
  }

  return (
    <>
      <Box sx={{ display: 'flex', alignItems: 'center', mb: 3 }}>
        <IconButton 
          onClick={() => navigate(`/projects/${projectId}`)} 
          sx={{ mr: 1 }}
        >
          <ArrowBackIcon />
        </IconButton>
        <Typography variant="h4" component="h1">
          New Dependency Analysis
        </Typography>
      </Box>
      
      <Paper sx={{ p: 3, mb: 4 }}>
        <Typography variant="subtitle1" gutterBottom>
          Project: <strong>{project?.name}</strong>
        </Typography>
        <Typography variant="body2" color="text.secondary" gutterBottom>
          {project?.description}
        </Typography>
        <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap', alignItems: 'center', mt: 1 }}>
          <Chip 
            icon={<CodeIcon />} 
            label="MAVEN" 
            size="small"
          />
          {project?.defaultPomPath && (
            <Chip 
              color="primary"
              label={`Default POM Path: ${project.defaultPomPath}`}
              size="small"
            />
          )}
          {loadingPomContent && (
            <Chip
              icon={<CircularProgress size={16} />}
              label="Loading POM file..."
              size="small"
            />
          )}
          {error && (
            <Alert severity="error" sx={{ mt: 1, width: '100%' }}>
              {error}
              <Button
                size="small"
                sx={{ mt: 1 }}
                onClick={() => {
                  setError(null);
                  setActiveStep(0);
                  setUsesDefaultPomPath(false);
                }}
              >
                Select Different Input Method
              </Button>
            </Alert>
          )}
        </Box>
      </Paper>
      
      <Stepper activeStep={activeStep} sx={{ mb: 4 }}>
        {steps.map((label) => (
          <Step key={label}>
            <StepLabel>{label}</StepLabel>
          </Step>
        ))}
      </Stepper>
      
      <Paper sx={{ p: 3 }}>
        {activeStep === 0 && (
          <>
            {loadingPomContent ? (
              <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', py: 5 }}>
                <CircularProgress size={40} sx={{ mb: 2 }} />
                <Typography>Loading POM file from default path...</Typography>
              </Box>
            ) : (
              <>
                <Typography variant="h6" gutterBottom>
                  Select Input Method
                </Typography>
                <Typography variant="body2" color="text.secondary" gutterBottom sx={{ mb: 3 }}>
                  Choose how you want to provide your project's POM file for analysis.
                </Typography>
                
                <Grid container spacing={3}>
                  <Grid item xs={12} md={6}>
                    <Card 
                      variant="outlined"
                      sx={{ 
                        cursor: 'pointer',
                        height: '100%',
                        bgcolor: inputMethod === 'file' ? 'rgba(33, 150, 243, 0.08)' : 'inherit',
                        border: inputMethod === 'file' ? '1px solid #2196f3' : '1px solid rgba(0, 0, 0, 0.12)',
                      }}
                      onClick={() => setInputMethod('file')}
                    >
                      <CardContent sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', p: 4 }}>
                        <CloudUploadIcon fontSize="large" color="primary" sx={{ mb: 2 }} />
                        <Typography variant="h6" gutterBottom>
                          Upload POM File
                        </Typography>
                        <Typography variant="body2" color="text.secondary" align="center">
                          Upload your project's pom.xml file from your computer.
                        </Typography>
                        
                        {inputMethod === 'file' && (
                          <Box sx={{ mt: 3, width: '100%' }}>
                            <input
                              ref={fileInputRef}
                              type="file"
                              accept=".xml"
                              style={{ display: 'none' }}
                              onChange={handleFileUpload}
                            />
                            <Button
                              variant="outlined"
                              startIcon={<DescriptionIcon />}
                              onClick={handleFileClick}
                              fullWidth
                            >
                              {pomFile ? pomFile.name : 'Select File'}
                            </Button>
                            {pomFile && (
                              <Box sx={{ mt: 2, display: 'flex', alignItems: 'center' }}>
                                <Chip 
                                  label={pomFile.name} 
                                  onDelete={() => {
                                    setPomFile(null);
                                    setPomContent('');
                                  }} 
                                />
                                <Typography variant="caption" color="text.secondary" sx={{ ml: 1 }}>
                                  {(pomFile.size / 1024).toFixed(2)} KB
                                </Typography>
                              </Box>
                            )}
                          </Box>
                        )}
                      </CardContent>
                    </Card>
                  </Grid>
                  
                  <Grid item xs={12} md={6}>
                    <Card 
                      variant="outlined"
                      sx={{ 
                        cursor: 'pointer',
                        height: '100%',
                        bgcolor: inputMethod === 'manual' ? 'rgba(33, 150, 243, 0.08)' : 'inherit',
                        border: inputMethod === 'manual' ? '1px solid #2196f3' : '1px solid rgba(0, 0, 0, 0.12)',
                      }}
                      onClick={() => setInputMethod('manual')}
                    >
                      <CardContent sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', p: 4 }}>
                        <CodeIcon fontSize="large" color="primary" sx={{ mb: 2 }} />
                        <Typography variant="h6" gutterBottom>
                          Paste POM Content
                        </Typography>
                        <Typography variant="body2" color="text.secondary" align="center">
                          Manually paste the content of your pom.xml file.
                        </Typography>
                        
                        {inputMethod === 'manual' && (
                          <TextField
                            multiline
                            rows={8}
                            fullWidth
                            placeholder="Paste your pom.xml content here..."
                            value={pomContent}
                            onChange={(e) => setPomContent(e.target.value)}
                            sx={{ mt: 3 }}
                          />
                        )}
                      </CardContent>
                    </Card>
                  </Grid>
                </Grid>
              </>
            )}
          </>
        )}
        
        {activeStep === 1 && (
          <>
            <Typography variant="h6" gutterBottom>
              Configure Analysis
            </Typography>
            <Typography variant="body2" color="text.secondary" gutterBottom sx={{ mb: 3 }}>
              Customize your dependency analysis options.
            </Typography>
            
            <Grid container spacing={3}>
              <Grid item xs={12} md={6}>
                <Card variant="outlined">
                  <CardContent>
                    <Typography variant="subtitle1" gutterBottom>
                      Analysis Options
                    </Typography>
                    <Divider sx={{ mb: 2 }} />
                    
                    <Grid container spacing={2} sx={{ mt: 3 }}>
                      {/* Vulnerability Check */}
                      <Grid item xs={12}>
                        <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2, alignItems: 'center' }}>
                          <Box>
                            <Typography variant="body2">Vulnerability Check</Typography>
                            <Typography variant="caption" color="text.secondary">
                              Scan dependencies for known security vulnerabilities
                            </Typography>
                          </Box>
                          <Button 
                            size="small" 
                            variant={analysisOptions.checkVulnerabilities ? "contained" : "outlined"} 
                            color={analysisOptions.checkVulnerabilities ? "primary" : "inherit"}
                            onClick={() => handleOptionChange('checkVulnerabilities')}
                          >
                            {analysisOptions.checkVulnerabilities ? "Enabled" : "Disabled"}
                          </Button>
                        </Box>
                      </Grid>
                      
                      {/* License Check */}
                      <Grid item xs={12}>
                        <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2, alignItems: 'center' }}>
                          <Box>
                            <Typography variant="body2">License Check</Typography>
                            <Typography variant="caption" color="text.secondary">
                              Identify license types and detect potential license issues
                            </Typography>
                          </Box>
                          <Button 
                            size="small" 
                            variant={analysisOptions.checkLicenses ? "contained" : "outlined"} 
                            color={analysisOptions.checkLicenses ? "primary" : "inherit"}
                            onClick={() => handleOptionChange('checkLicenses')}
                          >
                            {analysisOptions.checkLicenses ? "Enabled" : "Disabled"}
                          </Button>
                        </Box>
                      </Grid>
                      
                      {/* Transitive Dependencies */}
                      <Grid item xs={12}>
                        <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2, alignItems: 'center' }}>
                          <Box>
                            <Typography variant="body2">Transitive Dependencies</Typography>
                            <Typography variant="caption" color="text.secondary">
                              Include dependencies pulled in by your direct dependencies
                            </Typography>
                          </Box>
                          <Button 
                            size="small" 
                            variant={analysisOptions.includeTransitive ? "contained" : "outlined"} 
                            color={analysisOptions.includeTransitive ? "primary" : "inherit"}
                            onClick={() => handleOptionChange('includeTransitive')}
                          >
                            {analysisOptions.includeTransitive ? "Enabled" : "Disabled"}
                          </Button>
                        </Box>
                      </Grid>
                      
                      {/* Completion Notification */}
                      <Grid item xs={12}>
                        <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2, alignItems: 'center' }}>
                          <Box>
                            <Typography variant="body2">Completion Notification</Typography>
                            <Typography variant="caption" color="text.secondary">
                              Show a notification when the analysis is complete
                            </Typography>
                          </Box>
                          <Button 
                            size="small" 
                            variant={analysisOptions.notifyOnCompletion ? "contained" : "outlined"} 
                            color={analysisOptions.notifyOnCompletion ? "primary" : "inherit"}
                            onClick={() => handleOptionChange('notifyOnCompletion')}
                          >
                            {analysisOptions.notifyOnCompletion ? "Enabled" : "Disabled"}
                          </Button>
                        </Box>
                      </Grid>
                      
                      {/* Resolve BOM-managed versions */}
                      <Grid item xs={12}>
                        <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2, alignItems: 'center' }}>
                          <Box>
                            <Typography variant="body2">Resolve BOM-managed versions</Typography>
                            <Typography variant="caption" color="text.secondary">
                              Uses Maven to resolve actual versions of dependencies managed by BOMs
                            </Typography>
                          </Box>
                          <Button 
                            size="small" 
                            variant={analysisOptions.resolveBomVersions ? "contained" : "outlined"} 
                            color={analysisOptions.resolveBomVersions ? "primary" : "inherit"}
                            onClick={() => handleOptionChange('resolveBomVersions')}
                          >
                            {analysisOptions.resolveBomVersions ? "Enabled" : "Disabled"}
                          </Button>
                        </Box>
                      </Grid>
                    </Grid>
                    
                    {analysisOptions.resolveBomVersions && (
                      <Box sx={{ mt: 2 }}>
                        <Typography variant="body2" gutterBottom>
                          POM Directory Path
                        </Typography>
                        <TextField
                          fullWidth
                          size="small"
                          placeholder="Enter path to directory containing pom.xml"
                          value={pomDirectoryPath}
                          onChange={(e) => setPomDirectoryPath(e.target.value)}
                          helperText={
                            project?.defaultPomPath 
                              ? "Pre-filled with project's default path. You can modify it if needed."
                              : "Absolute path to directory where Maven commands can be executed (e.g., /path/to/your/maven/project)"
                          }
                        />
                        <Alert severity="info" sx={{ mt: 2, fontSize: '0.8rem' }}>
                          This feature uses Maven's help:evaluate plugin to resolve actual versions of dependencies managed by BOMs.
                          The directory must contain a valid pom.xml file and have Maven installed and available in the system path.
                          {project?.defaultPomPath && pomDirectoryPath !== project.defaultPomPath && (
                            <Box sx={{ mt: 1 }}>
                              <Button 
                                size="small" 
                                variant="outlined" 
                                onClick={() => setPomDirectoryPath(project.defaultPomPath)}
                              >
                                Reset to Default Path
                              </Button>
                            </Box>
                          )}
                        </Alert>
                      </Box>
                    )}
                  </CardContent>
                </Card>
              </Grid>
              
              <Grid item xs={12} md={6}>
                {pomContent ? (
                  <Card variant="outlined">
                    <CardContent>
                      <Typography variant="subtitle1" gutterBottom>
                        POM File Preview
                      </Typography>
                      <Divider sx={{ mb: 2 }} />
                      
                      <Box sx={{ 
                        maxHeight: 250, 
                        overflow: 'auto',
                        bgcolor: 'rgba(0, 0, 0, 0.03)',
                        borderRadius: 1,
                        p: 2,
                        fontFamily: 'monospace',
                        fontSize: '0.875rem',
                        whiteSpace: 'pre-wrap',
                      }}>
                        {pomContent ? pomContent.substring(0, 2000) + (pomContent.length > 2000 ? '...' : '') : 'No content to preview'}
                      </Box>
                    </CardContent>
                  </Card>
                ) : (
                  <Card variant="outlined">
                    <CardContent sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', height: '100%', p: 4 }}>
                      <Alert severity="info" sx={{ width: '100%' }}>
                        Please upload or paste your POM file content to see a preview.
                      </Alert>
                    </CardContent>
                  </Card>
                )}
              </Grid>
            </Grid>
          </>
        )}
        
        {activeStep === 2 && (
          <>
            <Typography variant="h6" gutterBottom align="center">
              {loading ? 'Analysis in Progress' : 'Review & Start Analysis'}
            </Typography>
            <Typography variant="body2" color="text.secondary" align="center" sx={{ mb: 2 }}>
              {!loading 
                ? 'Review your settings and click "Start Analysis" when ready.' 
                : analysisOptions.checkVulnerabilities 
                  ? 'Processing POM file and initializing vulnerability scanning. You will be redirected to the results page where scanning will continue in the background.' 
                  : 'Processing POM file and analyzing dependencies...'}
            </Typography>
            
            {loading && analysisOptions.checkVulnerabilities &&
              <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', mb: 4 }}>
                <CircularProgress size={60} sx={{ mb: 2 }} />
                <Box sx={{ width: '100%', maxWidth: 400 }}>
                  <Box sx={{ width: '100%', bgcolor: 'background.paper', borderRadius: 1, p: 1, mb: 1 }}>
                    <Box sx={{ height: 10, width: '100%', bgcolor: 'primary.lighter', borderRadius: 5, position: 'relative', overflow: 'hidden' }}>
                      <Box
                        sx={{
                          position: 'absolute',
                          left: 0,
                          top: 0,
                          height: '100%',
                          bgcolor: 'primary.main',
                          width: '30%',
                          borderRadius: 5,
                          animation: 'pulse 1.5s infinite ease-in-out',
                          '@keyframes pulse': {
                            '0%': { opacity: 0.6 },
                            '50%': { opacity: 1 },
                            '100%': { opacity: 0.6 }
                          }
                        }}
                      />
                    </Box>
                  </Box>
                  <Typography variant="caption" color="text.secondary" align="center" sx={{ display: 'block' }}>
                    <Box component="span" sx={{ fontWeight: 'bold', color: 'primary.main' }}>Security scanning in progress</Box> - This may take a moment as we check each dependency for vulnerabilities
                  </Typography>
                </Box>
              </Box>
            }
            
            {loading && !analysisOptions.checkVulnerabilities && (
              <Box sx={{ display: 'flex', justifyContent: 'center', mb: 4 }}>
                <CircularProgress />
              </Box>
            )}
            
            {!loading && (
              <>
                {error && (
                  <Alert severity="error" sx={{ mb: 3 }}>
                    {error}
                    {usesDefaultPomPath && (
                      <Box sx={{ mt: 2 }}>
                        <Typography variant="body2">
                          Please check that the file exists at the default path: <strong>{project?.defaultPomPath}</strong>
                        </Typography>
                        <Button 
                          variant="outlined" 
                          size="small" 
                          sx={{ mt: 1 }}
                          onClick={() => {
                            setUsesDefaultPomPath(false);
                            setActiveStep(0);
                          }}
                        >
                          Select Different Input Method
                        </Button>
                      </Box>
                    )}
                  </Alert>
                )}
                
                <Typography variant="h6" gutterBottom>
                  Review & Start Analysis
                </Typography>
                <Typography variant="body2" color="text.secondary" gutterBottom sx={{ mb: 3 }}>
                  Review your settings and start the dependency analysis.
                </Typography>
              </>
            )}
            
            <Grid container spacing={3}>
              <Grid item xs={12} md={6}>
                <Card variant="outlined">
                  <CardContent>
                    <Typography variant="subtitle1" gutterBottom>
                      Analysis Summary
                    </Typography>
                    <Divider sx={{ mb: 2 }} />
                    
                    <Box sx={{ mb: 2 }}>
                      <Typography variant="body2" color="text.secondary">Project:</Typography>
                      <Typography variant="body1">{project?.name}</Typography>
                    </Box>
                    
                    <Box sx={{ mb: 2 }}>
                      <Typography variant="body2" color="text.secondary">Input Method:</Typography>
                      <Typography variant="body1">
                        {usesDefaultPomPath 
                          ? 'Default POM Path' 
                          : inputMethod === 'file' ? 'Uploaded File' : 'Manual Input'
                        }
                        {inputMethod === 'file' && pomFile && (
                          <Typography variant="caption" sx={{ display: 'block' }}>
                            {pomFile.name} ({(pomFile.size / 1024).toFixed(2)} KB)
                          </Typography>
                        )}
                        {usesDefaultPomPath && project?.defaultPomPath && (
                          <Typography variant="caption" sx={{ display: 'block' }}>
                            Path: {project.defaultPomPath}
                          </Typography>
                        )}
                      </Typography>
                    </Box>
                    
                    <Box>
                      <Typography variant="body1" component="div" sx={{ mt: 2 }}>
                        <strong>Analysis Options:</strong>
                        <Box component="ul" sx={{ mt: 1, pl: 2 }}>
                          <li>• Vulnerability Check: <strong>{analysisOptions.checkVulnerabilities ? 'Enabled' : 'Disabled'}</strong></li>
                          <li>• License Check: <strong>{analysisOptions.checkLicenses ? 'Enabled' : 'Disabled'}</strong></li> 
                          <li>• Transitive Dependencies: <strong>{analysisOptions.includeTransitive ? 'Included' : 'Excluded'}</strong></li>
                          <li>• Completion Notification: <strong>{analysisOptions.notifyOnCompletion ? 'Enabled' : 'Disabled'}</strong></li>
                          {analysisOptions.resolveBomVersions && <li>• BOM Resolution: <strong>Enabled</strong></li>}
                        </Box>
                      </Typography>
                    </Box>
                  </CardContent>
                </Card>
              </Grid>
              
              <Grid item xs={12} md={6}>
                <Card variant="outlined">
                  <CardContent sx={{ display: 'flex', flexDirection: 'column', height: '100%', justifyContent: 'space-between' }}>
                    <Box>
                      <Typography variant="subtitle1" gutterBottom>
                        Start Analysis
                      </Typography>
                      <Divider sx={{ mb: 2 }} />
                      
                      <Typography variant="body2" paragraph>
                        When you click "Start Analysis", the system will:
                      </Typography>
                      <Box sx={{ pl: 2, mb: 3 }}>
                        <Typography variant="body2" gutterBottom>
                          1. Parse your POM file
                        </Typography>
                        <Typography variant="body2" gutterBottom>
                          2. Analyze all dependencies
                        </Typography>
                        <Typography variant="body2" gutterBottom>
                          3. Check for updates, vulnerabilities, and license issues
                        </Typography>
                        <Typography variant="body2" gutterBottom>
                          4. Generate reports and visualizations
                        </Typography>
                      </Box>
                      
                      {error && (
                        <Alert severity="error" sx={{ mb: 3 }}>
                          {error}
                        </Alert>
                      )}
                    </Box>
                    
                    <Box sx={{ display: 'flex', justifyContent: 'flex-end', mt: 3 }}>
                      <Button
                        variant="contained"
                        color="primary"
                        size="large"
                        onClick={submitAnalysis}
                        disabled={loading || !pomContent}
                        fullWidth
                        sx={{ 
                          py: 1.5,
                          bgcolor: 'primary.main',
                          '&:hover': { bgcolor: 'primary.dark' }
                        }}
                      >
                        {loading ? (
                          <Box sx={{ display: 'flex', alignItems: 'center' }}>
                            <CircularProgress size={24} color="inherit" sx={{ mr: 1 }} />
                            Starting Analysis...
                          </Box>
                        ) : (
                          'Start Analysis'
                        )}
                      </Button>
                    </Box>
                  </CardContent>
                </Card>
              </Grid>
            </Grid>
          </>
        )}
        
        <Box sx={{ display: 'flex', justifyContent: 'space-between', mt: 3 }}>
          <Button
            disabled={activeStep === 0}
            onClick={handleBack}
          >
            Back
          </Button>
          <Button
            variant="contained"
            onClick={handleNext}
            disabled={
              activeStep === steps.length - 1 ||
              (activeStep === 0 && !pomContent)
            }
          >
            Next
          </Button>
        </Box>
      </Paper>
    </>
  );
};

export default NewAnalysis; 