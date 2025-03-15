import React, { useState, useEffect } from 'react';
import {
  Typography,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Button,
  Box,
  TextField,
  InputAdornment,
  IconButton,
  CircularProgress,
  Chip,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  FormControl,
  InputLabel,
  MenuItem,
  Select,
  Snackbar,
  Alert,
  Menu,
  ListItemIcon,
  Grid,
  FormControlLabel,
  Checkbox
} from '@mui/material';
import { Link as RouterLink, useNavigate, useLocation } from 'react-router-dom';
import AddIcon from '@mui/icons-material/Add';
import SearchIcon from '@mui/icons-material/Search';
import MoreVertIcon from '@mui/icons-material/MoreVert';
import FilterListIcon from '@mui/icons-material/FilterList';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import PlayArrowIcon from '@mui/icons-material/PlayArrow';
import PauseIcon from '@mui/icons-material/Pause';
import api from '../services/api';

const Projects = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const [loading, setLoading] = useState(true);
  const [projects, setProjects] = useState([]);
  const [search, setSearch] = useState('');
  const [openNewProject, setOpenNewProject] = useState(false);
  const [newProject, setNewProject] = useState({
    name: '',
    description: '',
    defaultPomPath: '',
  });
  const [snackbar, setSnackbar] = useState({
    open: false,
    message: '',
    severity: 'info'
  });
  // Add states for menu
  const [menuAnchorEl, setMenuAnchorEl] = useState(null);
  const [selectedProject, setSelectedProject] = useState(null);
  // Add state for edit dialog
  const [openEditProject, setOpenEditProject] = useState(false);
  const [editProjectData, setEditProjectData] = useState({
    name: '',
    description: '',
    defaultPomPath: ''
  });
  // Add state for delete confirmation dialog
  const [openDeleteDialog, setOpenDeleteDialog] = useState(false);
  // Add state for filter dialog and options
  const [openFilterDialog, setOpenFilterDialog] = useState(false);
  const [filterOptions, setFilterOptions] = useState({
    hasAnalysis: false,
    hasOutdated: false,
    hasVulnerable: false,
    status: 'all'
  });
  const [appliedFilters, setAppliedFilters] = useState({
    hasAnalysis: false,
    hasOutdated: false,
    hasVulnerable: false,
    status: 'all'
  });

  const closeSnackbar = () => {
    setSnackbar({ ...snackbar, open: false });
  };

  useEffect(() => {
    // Fetch projects from the API
    fetchProjects();
    
    // Check if we should open the new project dialog based on navigation state
    if (location.state && location.state.openNewProjectDialog) {
      setOpenNewProject(true);
      // Clear the state to avoid reopening the dialog on refresh
      navigate(location.pathname, { replace: true, state: {} });
    }
  }, [location, navigate]);

  const fetchProjects = async () => {
    setLoading(true);
    try {
      const response = await api.projects.getAll();
      console.log('API Response:', response.data);
      setProjects(response.data || []);
    } catch (error) {
      console.error('Error fetching projects:', error);
      setSnackbar({
        open: true,
        message: 'Failed to load projects. Please try again.',
        severity: 'error'
      });
      // Set empty array if error occurs
      setProjects([]);
    } finally {
      setLoading(false);
    }
  };

  const handleCreateProject = async () => {
    if (!newProject.name) {
      setSnackbar({
        open: true,
        message: 'Project name is required',
        severity: 'warning'
      });
      return;
    }

    try {
      console.log('Creating project with data:', newProject);
      const response = await api.projects.create(newProject);
      console.log('Create project response:', response.data);
      
      // Add the new project to the list
      setProjects([...projects, response.data]);
      
      // Reset form and close dialog
      setOpenNewProject(false);
      setNewProject({
        name: '',
        description: '',
        defaultPomPath: '',
      });
      
      setSnackbar({
        open: true,
        message: 'Project created successfully!',
        severity: 'success'
      });
      
      // Navigate to the new project
      navigate(`/projects/${response.data.id}`);
    } catch (error) {
      console.error('Error creating project:', error);
      setSnackbar({
        open: true,
        message: `Failed to create project: ${error.response?.data?.message || error.message}`,
        severity: 'error'
      });
    }
  };

  // Handler for opening menu
  const handleMenuOpen = (event, project) => {
    setMenuAnchorEl(event.currentTarget);
    setSelectedProject(project);
  };

  // Handler for closing menu
  const handleMenuClose = () => {
    setMenuAnchorEl(null);
    // Only clear selectedProject if we're not in the middle of a delete operation
    if (!openDeleteDialog) {
      setSelectedProject(null);
    }
  };

  // Handler for edit option
  const handleEditClick = () => {
    setEditProjectData({
      name: selectedProject.name,
      description: selectedProject.description,
      defaultPomPath: selectedProject.defaultPomPath
    });
    setOpenEditProject(true);
    handleMenuClose();
  };

  // Handler for delete option
  const handleDeleteClick = () => {
    setOpenDeleteDialog(true);
    // Close the menu but don't reset selectedProject yet
    setMenuAnchorEl(null);
  };

  // Handler for toggling project status
  const handleToggleStatus = async () => {
    try {
      const response = await api.projects.toggleStatus(selectedProject.id);
      // Update the project in the list
      setProjects(projects.map(p => 
        p.id === selectedProject.id ? response.data : p
      ));
      setSnackbar({
        open: true,
        message: `Project status changed to ${response.data.status === 'ACTIVE' ? 'active' : 'inactive'}`,
        severity: 'success'
      });
    } catch (error) {
      console.error('Error toggling project status:', error);
      setSnackbar({
        open: true,
        message: `Error changing project status: ${error.message || 'Unknown error'}`,
        severity: 'error'
      });
    }
    handleMenuClose();
  };

  // Handler for updating a project
  const handleUpdateProject = async () => {
    if (!editProjectData.name) {
      setSnackbar({
        open: true,
        message: 'Project name is required',
        severity: 'warning'
      });
      return;
    }

    try {
      const response = await api.projects.update(selectedProject.id, editProjectData);
      
      // Update the project in the list
      setProjects(projects.map(project => 
        project.id === selectedProject.id ? response.data : project
      ));
      
      // Close dialog and reset form
      setOpenEditProject(false);
      setEditProjectData({ name: '', description: '', defaultPomPath: '' });
      
      setSnackbar({
        open: true,
        message: 'Project updated successfully!',
        severity: 'success'
      });
    } catch (error) {
      console.error('Error updating project:', error);
      setSnackbar({
        open: true,
        message: `Failed to update project: ${error.response?.data?.message || error.message}`,
        severity: 'error'
      });
    }
  };

  // Add this handler for when the delete dialog is closed
  const handleCloseDeleteDialog = () => {
    setOpenDeleteDialog(false);
    setSelectedProject(null);
  };

  // Handler for deleting a project
  const handleDeleteProject = async () => {
    try {
      await api.projects.delete(selectedProject.id);
      
      // Remove the project from the list
      setProjects(projects.filter(project => project.id !== selectedProject.id));
      
      // Close dialog and clean up
      handleCloseDeleteDialog();
      
      setSnackbar({
        open: true,
        message: 'Project deleted successfully!',
        severity: 'success'
      });
    } catch (error) {
      console.error('Error deleting project:', error);
      setSnackbar({
        open: true,
        message: `Failed to delete project: ${error.response?.data?.message || error.message}`,
        severity: 'error'
      });
    }
  };

  // Handler for opening filter dialog
  const handleOpenFilterDialog = () => {
    setFilterOptions({...appliedFilters});
    setOpenFilterDialog(true);
  };

  // Handler for closing filter dialog without applying
  const handleCloseFilterDialog = () => {
    setOpenFilterDialog(false);
  };

  // Handler for applying filters
  const handleApplyFilters = () => {
    setAppliedFilters({...filterOptions});
    setOpenFilterDialog(false);
  };

  // Handler for clearing filters
  const handleClearFilters = () => {
    const defaultFilters = {
      hasAnalysis: false,
      hasOutdated: false,
      hasVulnerable: false,
      status: 'all'
    };
    setFilterOptions(defaultFilters);
    setAppliedFilters(defaultFilters);
    setOpenFilterDialog(false);
  };

  const filteredProjects = projects.filter(project => {
    // Apply text search filter
    const matchesSearch = (
      project.name?.toLowerCase().includes(search.toLowerCase()) ||
      project.description?.toLowerCase().includes(search.toLowerCase())
    );
    
    // Apply other filters
    const matchesAnalysis = !appliedFilters.hasAnalysis || (project.totalAnalyses && project.totalAnalyses > 0);
    const matchesOutdated = !appliedFilters.hasOutdated || (project.outdatedCount && project.outdatedCount > 0);
    const matchesVulnerable = !appliedFilters.hasVulnerable || (project.vulnerableCount && project.vulnerableCount > 0);
    const matchesStatus = appliedFilters.status === 'all' || 
      (appliedFilters.status === 'active' && (!project.status || project.status !== 'INACTIVE')) ||
      (appliedFilters.status === 'inactive' && project.status === 'INACTIVE');
    
    return matchesSearch && matchesAnalysis && matchesOutdated && matchesVulnerable && matchesStatus;
  });

  // Determine if any filters are applied
  const hasActiveFilters = 
    appliedFilters.hasAnalysis || 
    appliedFilters.hasOutdated || 
    appliedFilters.hasVulnerable || 
    appliedFilters.status !== 'all';

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', mt: 10 }}>
        <CircularProgress />
      </Box>
    );
  }

  return (
    <>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h4" component="h1">
          Projects
        </Typography>
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={() => setOpenNewProject(true)}
        >
          New Project
        </Button>
      </Box>

      <Paper sx={{ mb: 4, p: 2 }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
          <TextField
            placeholder="Search projects..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            variant="outlined"
            size="small"
            sx={{ width: '40%' }}
            InputProps={{
              startAdornment: (
                <InputAdornment position="start">
                  <SearchIcon />
                </InputAdornment>
              ),
            }}
          />
          <Button 
            startIcon={<FilterListIcon />} 
            onClick={handleOpenFilterDialog}
            color={hasActiveFilters ? "primary" : "inherit"}
            variant={hasActiveFilters ? "contained" : "outlined"}
          >
            Filters
            {hasActiveFilters && (
              <Chip 
                size="small" 
                label={
                  Object.values(appliedFilters).filter(f => f === true || f !== 'all').length
                } 
                sx={{ ml: 1, bgcolor: 'background.paper' }} 
              />
            )}
          </Button>
        </Box>

        <TableContainer>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Project Name</TableCell>
                <TableCell>Description</TableCell>
                <TableCell>Last Analysis</TableCell>
                <TableCell>Dependencies</TableCell>
                <TableCell>Status</TableCell>
                <TableCell align="right">Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {filteredProjects.length > 0 ? (
                filteredProjects.map((project) => (
                  <TableRow key={project.id} hover>
                    <TableCell>
                      <Typography
                        variant="subtitle2"
                        component={RouterLink}
                        to={`/projects/${project.id}`}
                        sx={{ textDecoration: 'none', color: 'primary.main', fontWeight: 'medium' }}
                      >
                        {project.name}
                      </Typography>
                    </TableCell>
                    <TableCell>{project.description || 'No description'}</TableCell>
                    <TableCell>{project.lastAnalysisDate || 'Never'}</TableCell>
                    <TableCell>
                      {project.dependencyCount > 0 ? (
                        <Box>
                          <Typography variant="body2">
                            {project.dependencyCount} total
                          </Typography>
                          <Typography variant="body2" color="warning.main">
                            {project.outdatedCount || 0} outdated
                          </Typography>
                        </Box>
                      ) : (
                        'No analysis'
                      )}
                    </TableCell>
                    <TableCell>
                      <Chip
                        label={project.status === 'INACTIVE' ? 'Inactive' : 'Active'}
                        color={project.status !== 'INACTIVE' ? 'success' : 'default'}
                        size="small"
                      />
                    </TableCell>
                    <TableCell align="right">
                      <Button
                        variant="outlined"
                        size="small"
                        component={RouterLink}
                        to={`/projects/${project.id}/new-analysis`}
                        sx={{ mr: 1 }}
                      >
                        Analyze
                      </Button>
                      <IconButton size="small" onClick={(e) => handleMenuOpen(e, project)}>
                        <MoreVertIcon />
                      </IconButton>
                    </TableCell>
                  </TableRow>
                ))
              ) : (
                <TableRow>
                  <TableCell colSpan={6} align="center">
                    No projects found
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </TableContainer>
      </Paper>

      {/* Create Project Dialog */}
      <Dialog open={openNewProject} onClose={() => setOpenNewProject(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Create New Project</DialogTitle>
        <DialogContent>
          <TextField
            autoFocus
            margin="dense"
            id="name"
            label="Project Name"
            type="text"
            required
            fullWidth
            value={newProject.name}
            onChange={(e) => setNewProject({ ...newProject, name: e.target.value })}
          />
          <TextField
            margin="dense"
            id="description"
            label="Description"
            type="text"
            fullWidth
            multiline
            rows={4}
            value={newProject.description}
            onChange={(e) => setNewProject({ ...newProject, description: e.target.value })}
          />
          <TextField
            margin="dense"
            id="defaultPomPath"
            label="Default POM Directory Path"
            type="text"
            fullWidth
            value={newProject.defaultPomPath}
            onChange={(e) => setNewProject({ ...newProject, defaultPomPath: e.target.value })}
            helperText="Set a default path where Maven can find and resolve BOM dependencies. This will be pre-filled when starting a new analysis."
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenNewProject(false)}>Cancel</Button>
          <Button onClick={handleCreateProject} variant="contained">Create</Button>
        </DialogActions>
      </Dialog>

      {/* Edit Project Dialog */}
      <Dialog open={openEditProject} onClose={() => setOpenEditProject(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Edit Project</DialogTitle>
        <DialogContent>
          <TextField
            autoFocus
            margin="dense"
            id="edit-name"
            label="Project Name"
            type="text"
            required
            fullWidth
            value={editProjectData.name}
            onChange={(e) => setEditProjectData({ ...editProjectData, name: e.target.value })}
          />
          <TextField
            margin="dense"
            id="edit-description"
            label="Description"
            type="text"
            fullWidth
            multiline
            rows={4}
            value={editProjectData.description}
            onChange={(e) => setEditProjectData({ ...editProjectData, description: e.target.value })}
          />
          <TextField
            margin="dense"
            id="edit-defaultPomPath"
            label="Default POM Directory Path"
            type="text"
            fullWidth
            value={editProjectData.defaultPomPath}
            onChange={(e) => setEditProjectData({ ...editProjectData, defaultPomPath: e.target.value })}
            helperText="Set a default path where Maven can find and resolve BOM dependencies. This will be pre-filled when starting a new analysis."
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenEditProject(false)}>Cancel</Button>
          <Button onClick={handleUpdateProject} variant="contained">Save</Button>
        </DialogActions>
      </Dialog>

      {/* Delete Confirmation Dialog */}
      <Dialog open={openDeleteDialog} onClose={handleCloseDeleteDialog}>
        <DialogTitle>Delete Project</DialogTitle>
        <DialogContent>
          <Typography>
            Are you sure you want to delete "{selectedProject?.name}"? This action cannot be undone.
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseDeleteDialog}>Cancel</Button>
          <Button onClick={handleDeleteProject} variant="contained" color="error">Delete</Button>
        </DialogActions>
      </Dialog>

      {/* Filter Dialog */}
      <Dialog open={openFilterDialog} onClose={handleCloseFilterDialog} maxWidth="sm" fullWidth>
        <DialogTitle>Filter Projects</DialogTitle>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 1 }}>
            <Grid item xs={12}>
              <Typography variant="subtitle1" gutterBottom>
                Project Status
              </Typography>
              <FormControl fullWidth size="small">
                <InputLabel id="status-filter-label">Status</InputLabel>
                <Select
                  labelId="status-filter-label"
                  id="status-filter"
                  value={filterOptions.status}
                  label="Status"
                  onChange={(e) => setFilterOptions({...filterOptions, status: e.target.value})}
                >
                  <MenuItem value="all">All Statuses</MenuItem>
                  <MenuItem value="active">Active</MenuItem>
                  <MenuItem value="inactive">Inactive</MenuItem>
                </Select>
              </FormControl>
            </Grid>
            
            <Grid item xs={12}>
              <Typography variant="subtitle1" gutterBottom>
                Dependency Filters
              </Typography>
              <FormControlLabel
                control={
                  <Checkbox 
                    checked={filterOptions.hasAnalysis} 
                    onChange={(e) => setFilterOptions({...filterOptions, hasAnalysis: e.target.checked})} 
                  />
                }
                label="Has Analysis"
              />
              <FormControlLabel
                control={
                  <Checkbox 
                    checked={filterOptions.hasOutdated} 
                    onChange={(e) => setFilterOptions({...filterOptions, hasOutdated: e.target.checked})} 
                  />
                }
                label="Has Outdated Dependencies"
              />
              <FormControlLabel
                control={
                  <Checkbox 
                    checked={filterOptions.hasVulnerable} 
                    onChange={(e) => setFilterOptions({...filterOptions, hasVulnerable: e.target.checked})} 
                  />
                }
                label="Has Vulnerabilities"
              />
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleClearFilters}>Clear All</Button>
          <Button onClick={handleCloseFilterDialog}>Cancel</Button>
          <Button onClick={handleApplyFilters} variant="contained">Apply</Button>
        </DialogActions>
      </Dialog>

      {/* Project Menu */}
      <Menu
        anchorEl={menuAnchorEl}
        open={Boolean(menuAnchorEl)}
        onClose={handleMenuClose}
      >
        <MenuItem onClick={handleEditClick}>
          <ListItemIcon>
            <EditIcon fontSize="small" />
          </ListItemIcon>
          Edit
        </MenuItem>
        <MenuItem onClick={handleToggleStatus}>
          <ListItemIcon>
            {selectedProject?.status === 'INACTIVE' ? (
              <PlayArrowIcon fontSize="small" color="success" />
            ) : (
              <PauseIcon fontSize="small" color="warning" />
            )}
          </ListItemIcon>
          {selectedProject?.status === 'INACTIVE' ? 'Activate' : 'Deactivate'}
        </MenuItem>
        <MenuItem onClick={handleDeleteClick}>
          <ListItemIcon>
            <DeleteIcon fontSize="small" color="error" />
          </ListItemIcon>
          Delete
        </MenuItem>
      </Menu>

      {/* Snackbar for notifications */}
      <Snackbar
        open={snackbar.open}
        autoHideDuration={6000}
        onClose={closeSnackbar}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
      >
        <Alert onClose={closeSnackbar} severity={snackbar.severity} sx={{ width: '100%' }}>
          {snackbar.message}
        </Alert>
      </Snackbar>
    </>
  );
};

export default Projects; 