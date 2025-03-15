import React, { createContext, useMemo, useState } from 'react';
import { Route, Routes, Navigate } from 'react-router-dom';
import { createTheme, ThemeProvider, responsiveFontSizes } from '@mui/material/styles';
import CssBaseline from '@mui/material/CssBaseline';
import Box from '@mui/material/Box';
import { ToastContainer } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';

// Layout components
import Header from './components/layout/Header';
import Sidebar from './components/layout/Sidebar';
import Footer from './components/layout/Footer';

// Pages
import Dashboard from './pages/Dashboard';
import Projects from './pages/Projects';
import ProjectDetail from './pages/ProjectDetail';
import NewAnalysis from './pages/NewAnalysis';
import AnalysisHistory from './pages/AnalysisHistory';
import AnalysisDetail from './pages/AnalysisDetail';
import Charts from './pages/Charts';
import NotFound from './pages/NotFound';
import Settings from './pages/Settings';

// Styles
import { Container } from '@mui/material';

// Create Theme Context
export const ColorModeContext = createContext({ 
  toggleColorMode: () => {},
  mode: 'light'
});

function App() {
  // State to track the current theme mode
  const [mode, setMode] = useState('light');

  // Color mode toggle function
  const colorMode = useMemo(
    () => ({
      toggleColorMode: () => {
        setMode((prevMode) => (prevMode === 'light' ? 'dark' : 'light'));
      },
      mode,
    }),
    [mode],
  );

  // Create theme based on the current mode
  const theme = useMemo(
    () =>
      createTheme({
        palette: {
          mode,
          primary: {
            main: '#2196f3',
          },
          secondary: {
            main: '#f50057',
          },
          background: {
            default: mode === 'light' ? '#f5f7fa' : '#121212',
            paper: mode === 'light' ? '#ffffff' : '#1e1e1e',
          },
        },
        typography: {
          fontFamily: 'Roboto, Arial, sans-serif',
        },
        components: {
          MuiButton: {
            styleOverrides: {
              root: {
                borderRadius: 8,
              },
            },
          },
          MuiPaper: {
            styleOverrides: {
              root: {
                borderRadius: 8,
                boxShadow: '0 2px 10px rgba(0, 0, 0, 0.08)',
              },
            },
          },
        },
      }),
    [mode],
  );

  return (
    <ColorModeContext.Provider value={colorMode}>
      <ThemeProvider theme={responsiveFontSizes(theme)}>
        <CssBaseline />
        <Box sx={{ display: 'flex', flexDirection: 'column', minHeight: '100vh' }}>
          <Header />
          <Box sx={{ display: 'flex', flex: 1 }}>
            <Sidebar />
            <Box
              component="main"
              sx={{ flexGrow: 1, p: 3, overflow: 'auto' }}
            >
              <Container maxWidth="xl">
                <Routes>
                  <Route path="/" element={<Dashboard />} />
                  <Route path="/projects" element={<Projects />} />
                  <Route path="/projects/:id" element={<ProjectDetail />} />
                  <Route path="/projects/:id/new-analysis" element={<NewAnalysis />} />
                  <Route path="/new-analysis" element={<NewAnalysis />} />
                  <Route path="/analysis-history" element={<AnalysisHistory />} />
                  <Route path="/analysis/:id" element={<AnalysisDetail />} />
                  <Route path="/charts" element={<Charts />} />
                  <Route path="/settings" element={<Settings />} />
                  <Route path="/404" element={<NotFound />} />
                  <Route path="*" element={<Navigate to="/404" replace />} />
                </Routes>
              </Container>
            </Box>
          </Box>
          <Footer />
        </Box>
      </ThemeProvider>
      <ToastContainer position="top-right" autoClose={5000} />
    </ColorModeContext.Provider>
  );
}

export default App; 