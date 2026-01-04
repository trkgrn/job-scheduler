import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from 'react-query';
import { ThemeProvider, createTheme } from '@mui/material/styles';
import CssBaseline from '@mui/material/CssBaseline';
import { SidebarProvider } from './contexts/SidebarContext';
import DashboardLayout from './components/Layout/DashboardLayout';
import CronJobDashboard from './components/Dashboard/CronJobDashboard';
import StatisticsPage from './components/Statistics/StatisticsPage';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      retry: 1,
    },
  },
});

const theme = createTheme({
  palette: {
    mode: 'light',
    primary: {
      main: '#1976d2',
    },
    secondary: {
      main: '#dc004e',
    },
  },
  typography: {
    fontFamily: '"Roboto", "Helvetica", "Arial", sans-serif',
  },
  components: {
    MuiCard: {
      styleOverrides: {
        root: {
          boxShadow: '0 2px 8px rgba(0,0,0,0.1)',
        },
      },
    },
  },
});

const App: React.FC = () => {
  return (
    <QueryClientProvider client={queryClient}>
      <ThemeProvider theme={theme}>
        <CssBaseline />
        <SidebarProvider>
          <Router>
            <DashboardLayout>
              <Routes>
                <Route path="/" element={<CronJobDashboard />} />
                <Route path="/dashboard" element={<CronJobDashboard />} />
                <Route path="/stats" element={<StatisticsPage />} />
              </Routes>
            </DashboardLayout>
          </Router>
        </SidebarProvider>
      </ThemeProvider>
    </QueryClientProvider>
  );
};

export default App;
