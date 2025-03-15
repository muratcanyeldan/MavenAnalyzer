import React, { useState } from 'react';
import { 
  AppBar, 
  Toolbar, 
  Typography, 
  IconButton, 
  Box, 
  Menu, 
  MenuItem, 
  ListItemIcon, 
  ListItemText,
  Tooltip
} from '@mui/material';
import { Link as RouterLink } from 'react-router-dom';
import MenuIcon from '@mui/icons-material/Menu';
import HelpIcon from '@mui/icons-material/Help';
import AccountCircleIcon from '@mui/icons-material/AccountCircle';
import InfoIcon from '@mui/icons-material/Info';
import Brightness4Icon from '@mui/icons-material/Brightness4';
import Brightness7Icon from '@mui/icons-material/Brightness7';
import { useContext } from 'react';
import { ColorModeContext } from '../../App';

const Header = () => {
  const { mode, toggleColorMode } = useContext(ColorModeContext);
  
  // State for help menu
  const [helpAnchorEl, setHelpAnchorEl] = useState(null);
  const helpOpen = Boolean(helpAnchorEl);
  
  // State for user menu
  const [userAnchorEl, setUserAnchorEl] = useState(null);
  const userOpen = Boolean(userAnchorEl);
  
  // Event handlers for help menu
  const handleHelpClick = (event) => {
    setHelpAnchorEl(event.currentTarget);
  };
  
  const handleHelpClose = () => {
    setHelpAnchorEl(null);
  };
  
  // Event handlers for user menu
  const handleUserClick = (event) => {
    setUserAnchorEl(event.currentTarget);
  };
  
  const handleUserClose = () => {
    setUserAnchorEl(null);
  };
  
  return (
    <AppBar position="fixed" sx={{ zIndex: (theme) => theme.zIndex.drawer + 1 }}>
      <Toolbar>
        <IconButton
          color="inherit"
          aria-label="open drawer"
          edge="start"
          sx={{ mr: 2, display: { sm: 'none' } }}
        >
          <MenuIcon />
        </IconButton>
        
        <Typography
          variant="h6"
          component={RouterLink}
          to="/"
          sx={{ 
            color: 'white', 
            textDecoration: 'none',
            fontWeight: 'bold',
            flexGrow: 1
          }}
        >
          Maven Dependency Analyzer
        </Typography>

        <Box sx={{ display: 'flex', alignItems: 'center' }}>
          {/* Help Icon */}
          <Tooltip title="Help">
            <IconButton 
              color="inherit" 
              size="large"
              onClick={handleHelpClick}
              aria-controls={helpOpen ? "help-menu" : undefined}
              aria-haspopup="true"
              aria-expanded={helpOpen ? "true" : undefined}
            >
              <HelpIcon />
            </IconButton>
          </Tooltip>
          
          {/* Help Menu */}
          <Menu
            id="help-menu"
            anchorEl={helpAnchorEl}
            open={helpOpen}
            onClose={handleHelpClose}
            PaperProps={{
              elevation: 3,
              sx: { width: 250 }
            }}
            transformOrigin={{ horizontal: 'right', vertical: 'top' }}
            anchorOrigin={{ horizontal: 'right', vertical: 'bottom' }}
          >
            <MenuItem 
              onClick={() => {
                // More reliable way to access the Documentation dialog in the footer
                const footerLinks = document.querySelectorAll('footer a');
                const docLink = Array.from(footerLinks).find(link => 
                  link.textContent && link.textContent.includes('Documentation')
                );
                if (docLink) {
                  docLink.click();
                }
                handleHelpClose();
              }}
            >
              <ListItemIcon>
                <InfoIcon fontSize="small" />
              </ListItemIcon>
              <ListItemText>Documentation</ListItemText>
            </MenuItem>
            <MenuItem 
              onClick={() => {
                // Find the support link in footer
                const footerLinks = document.querySelectorAll('footer a');
                const supportLink = Array.from(footerLinks).find(link => 
                  link.textContent && link.textContent.includes('Support')
                );
                if (supportLink) {
                  supportLink.click();
                }
                handleHelpClose();
              }}
            >
              <ListItemIcon>
                <HelpIcon fontSize="small" />
              </ListItemIcon>
              <ListItemText>Contact Support</ListItemText>
            </MenuItem>
          </Menu>
          
          {/* User Profile Icon */}
          <Box sx={{ ml: 2 }}>
            <Tooltip title="Account settings">
              <IconButton 
                color="inherit" 
                size="large"
                onClick={handleUserClick}
                aria-controls={userOpen ? "user-menu" : undefined}
                aria-haspopup="true"
                aria-expanded={userOpen ? "true" : undefined}
              >
                <AccountCircleIcon />
              </IconButton>
            </Tooltip>
          </Box>
          
          {/* User Profile Menu */}
          <Menu
            id="user-menu"
            anchorEl={userAnchorEl}
            open={userOpen}
            onClose={handleUserClose}
            PaperProps={{
              elevation: 3,
              sx: { width: 220 }
            }}
            transformOrigin={{ horizontal: 'right', vertical: 'top' }}
            anchorOrigin={{ horizontal: 'right', vertical: 'bottom' }}
          >
            <MenuItem onClick={toggleColorMode}>
              <ListItemIcon>
                {mode === 'dark' ? <Brightness7Icon fontSize="small" /> : <Brightness4Icon fontSize="small" />}
              </ListItemIcon>
              <ListItemText primary="Toggle Theme" secondary={mode === 'dark' ? 'Switch to Light Mode' : 'Switch to Dark Mode'} />
            </MenuItem>
          </Menu>
        </Box>
      </Toolbar>
    </AppBar>
  );
};

export default Header; 