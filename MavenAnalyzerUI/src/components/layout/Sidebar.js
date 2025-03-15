import React from 'react';
import { 
  Drawer, 
  List, 
  ListItem, 
  ListItemButton,
  ListItemIcon, 
  ListItemText, 
  Toolbar,
  Divider,
  Box
} from '@mui/material';
import { useLocation, useNavigate } from 'react-router-dom';
import DashboardIcon from '@mui/icons-material/Dashboard';
import FolderIcon from '@mui/icons-material/Folder';
import HistoryIcon from '@mui/icons-material/History';
import AddCircleOutlineIcon from '@mui/icons-material/AddCircleOutline';
import BarChartIcon from '@mui/icons-material/BarChart';
import SettingsIcon from '@mui/icons-material/Settings';

const drawerWidth = 240;

const menuItems = [
  { text: 'Dashboard', icon: <DashboardIcon />, path: '/' },
  { text: 'Projects', icon: <FolderIcon />, path: '/projects' },
  { text: 'Analysis History', icon: <HistoryIcon />, path: '/analysis-history' },
  { text: 'Charts', icon: <BarChartIcon />, path: '/charts' },
  { text: 'Settings', icon: <SettingsIcon />, path: '/settings' },
];

const Sidebar = () => {
  const location = useLocation();
  const navigate = useNavigate();

  return (
    <Drawer
      variant="permanent"
      sx={{
        width: drawerWidth,
        flexShrink: 0,
        [`& .MuiDrawer-paper`]: { 
          width: drawerWidth, 
          boxSizing: 'border-box',
          display: { xs: 'none', sm: 'block' } 
        },
      }}
    >
      <Toolbar />
      <Box sx={{ overflow: 'auto', mt: 2 }}>
        <List>
          {menuItems.map((item) => (
            <ListItem key={item.text} disablePadding>
              <ListItemButton 
                selected={location.pathname === item.path}
                onClick={() => navigate(item.path)}
                sx={{
                  '&.Mui-selected': {
                    backgroundColor: 'rgba(33, 150, 243, 0.08)',
                    borderLeft: '4px solid #2196f3',
                  },
                  '&.Mui-selected:hover': {
                    backgroundColor: 'rgba(33, 150, 243, 0.12)',
                  },
                  borderLeft: '4px solid transparent',
                }}
              >
                <ListItemIcon sx={{ 
                  color: location.pathname === item.path ? 'primary.main' : 'inherit'
                }}>
                  {item.icon}
                </ListItemIcon>
                <ListItemText primary={item.text} />
              </ListItemButton>
            </ListItem>
          ))}
        </List>
        <Divider sx={{ my: 2 }} />
        <List>
          <ListItem disablePadding>
            <ListItemButton onClick={() => navigate('/projects', { state: { openNewProjectDialog: true } })}>
              <ListItemIcon>
                <AddCircleOutlineIcon color="secondary" />
              </ListItemIcon>
              <ListItemText primary="New Project" />
            </ListItemButton>
          </ListItem>
        </List>
      </Box>
    </Drawer>
  );
};

export default Sidebar; 