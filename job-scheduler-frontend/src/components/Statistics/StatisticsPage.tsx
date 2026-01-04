import React, { useState } from 'react';
import {
  Box,
  Grid,
  Card,
  CardContent,
  Typography,
  Paper,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Button,
  Alert,
  CircularProgress,
  Chip,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
} from '@mui/material';
import {
  Refresh as RefreshIcon,
  TrendingUp as TrendingUpIcon,
  TrendingDown as TrendingDownIcon,
  Schedule as ScheduleIcon,
  CheckCircle as SuccessIcon,
  Error as ErrorIcon,
  PlayArrow as PlayIcon,
  Pause as PauseIcon,
} from '@mui/icons-material';
import { useQuery } from 'react-query';
import { statsApi } from '../../services/api';
import { JobStats, StatsOverview, StatusDistribution, ExecutionTrendData, TopJob } from '../../types';
import {
  LineChart,
  Line,
  AreaChart,
  Area,
  BarChart,
  Bar,
  PieChart,
  Pie,
  Cell,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from 'recharts';

const StatisticsPage: React.FC = () => {
  const [timeRange, setTimeRange] = useState('7d');

  // Use optimized overview endpoint - all stats data in a single call
  const days = timeRange === '1d' ? 1 : timeRange === '7d' ? 7 : timeRange === '30d' ? 30 : 90;
  const { data: overviewResponse, isLoading: overviewLoading } = useQuery<StatsOverview>(
    ['statsOverview', days],
    () => statsApi.getOverviewStats(days, 10).then(res => res.data)
  );
  
  // Extract data from overview response
  const overview = overviewResponse;
  const jobStats = overview?.jobStats;
  const executionTrend: ExecutionTrendData[] = overview?.executionTrend || [];
  const jobStatusDistribution: StatusDistribution[] = overview?.jobStatusDistribution || [];
  const triggerStatusDistribution: StatusDistribution[] = overview?.triggerStatusDistribution || [];
  const topJobsByExecution: TopJob[] = overview?.topJobsByExecution || [];

  // Use data from overview response (already processed on backend)
  const getExecutionTrendData = () => {
    return executionTrend.map((item: ExecutionTrendData) => ({
      date: item.date,
      successful: item.successful,
      failed: item.failed,
      total: item.total,
    }));
  };

  const getJobStatusData = () => {
    return jobStatusDistribution.map((item: StatusDistribution) => ({
      name: item.status,
      value: item.count,
    }));
  };

  const getTriggerStatusData = () => {
    return triggerStatusDistribution.map((item: StatusDistribution) => ({
      name: item.status,
      value: item.count,
    }));
  };

  const getTopJobsByExecution = () => {
    return topJobsByExecution.map((item: TopJob) => ({
      jobName: item.jobName,
      executionCount: item.executionCount,
    }));
  };

  const formatExecutionTime = (milliseconds: number): string => {
    if (!milliseconds || milliseconds === 0) {
      return '0 ms';
    }
    
    // If less than 1000ms, show in milliseconds
    if (milliseconds < 1000) {
      return `${Math.round(milliseconds * 100) / 100} ms`;
    }
    
    // Convert to seconds
    const seconds = milliseconds / 1000;
    
    // If less than 60 seconds, show in seconds
    if (seconds < 60) {
      return `${Math.round(seconds * 100) / 100} sec`;
    }
    
    // If less than 3600 seconds (1 hour), show minutes and seconds
    if (seconds < 3600) {
      const minutes = Math.floor(seconds / 60);
      const remainingSeconds = Math.round(seconds % 60);
      return `${minutes} min ${remainingSeconds} sec`;
    }
    
    // If 1 hour or more, show hours and minutes
    const hours = Math.floor(seconds / 3600);
    const remainingMinutes = Math.floor((seconds % 3600) / 60);
    return `${hours} h ${remainingMinutes} min`;
  };

  const COLORS = ['#0088FE', '#00C49F', '#FFBB28', '#FF8042', '#8884D8'];

  if (overviewLoading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress />
      </Box>
    );
  }

  if (!overview || !jobStats) {
    return <Alert severity="error">Failed to load statistics</Alert>;
  }

  return (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h4">Statistics</Typography>
        <Box>
          <FormControl sx={{ minWidth: 120, mr: 2 }}>
            <InputLabel>Time Range</InputLabel>
            <Select
              value={timeRange}
              onChange={(e) => setTimeRange(e.target.value)}
              label="Time Range"
            >
              <MenuItem value="1d">Last 24h</MenuItem>
              <MenuItem value="7d">Last 7 days</MenuItem>
              <MenuItem value="30d">Last 30 days</MenuItem>
              <MenuItem value="90d">Last 90 days</MenuItem>
            </Select>
          </FormControl>
          <Button
            variant="outlined"
            startIcon={<RefreshIcon />}
            onClick={() => window.location.reload()}
          >
            Refresh
          </Button>
        </Box>
      </Box>

      {/* Overview Cards */}
      <Grid container spacing={3} sx={{ mb: 3 }}>
        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Box display="flex" alignItems="center">
                <ScheduleIcon color="primary" sx={{ mr: 2 }} />
                <Box>
                  <Typography color="textSecondary" gutterBottom>
                    Total Jobs
                  </Typography>
                  <Typography variant="h4">
                    {jobStats.totalJobs}
                  </Typography>
                </Box>
              </Box>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Box display="flex" alignItems="center">
                <PlayIcon color="primary" sx={{ mr: 2 }} />
                <Box>
                  <Typography color="textSecondary" gutterBottom>
                    Active Jobs
                  </Typography>
                  <Typography variant="h4">
                    {jobStats.activeJobs}
                  </Typography>
                </Box>
              </Box>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Box display="flex" alignItems="center">
                <SuccessIcon color="success" sx={{ mr: 2 }} />
                <Box>
                  <Typography color="textSecondary" gutterBottom>
                    Success Rate
                  </Typography>
                  <Typography variant="h4">
                    {jobStats.totalExecutions > 0 
                      ? Math.round((jobStats.successfulExecutions / jobStats.totalExecutions) * 100)
                      : 0}%
                  </Typography>
                </Box>
              </Box>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Box display="flex" alignItems="center">
                <ErrorIcon color="error" sx={{ mr: 2 }} />
                <Box>
                  <Typography color="textSecondary" gutterBottom>
                    Failed Jobs
                  </Typography>
                  <Typography variant="h4">
                    {jobStats.failedJobs}
                  </Typography>
                </Box>
              </Box>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* Charts */}
      <Grid container spacing={3}>
        {/* Execution Trend */}
        <Grid item xs={12} md={8}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Execution Trend {timeRange === '1d' ? '(Last 24h)' : timeRange === '7d' ? '(Last 7 Days)' : timeRange === '30d' ? '(Last 30 Days)' : '(Last 90 Days)'}
              </Typography>
              <ResponsiveContainer width="100%" height={300}>
                <AreaChart data={getExecutionTrendData()}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="date" />
                  <YAxis />
                  <Tooltip />
                  <Legend />
                  <Area type="monotone" dataKey="successful" stackId="1" stroke="#4caf50" fill="#4caf50" name="Successful" />
                  <Area type="monotone" dataKey="failed" stackId="1" stroke="#f44336" fill="#f44336" name="Failed" />
                </AreaChart>
              </ResponsiveContainer>
            </CardContent>
          </Card>
        </Grid>

        {/* Job Status Distribution */}
        <Grid item xs={12} md={4}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Job Status Distribution
              </Typography>
              <ResponsiveContainer width="100%" height={300}>
                <PieChart>
                  <Pie
                    data={getJobStatusData()}
                    cx="50%"
                    cy="50%"
                    labelLine={false}
                    label={({ name, percent }) => `${name} ${(percent * 100).toFixed(0)}%`}
                    outerRadius={80}
                    fill="#8884d8"
                    dataKey="value"
                  >
                    {getJobStatusData().map((entry: {name: string, value: number}, index: number) => (
                      <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                    ))}
                  </Pie>
                  <Tooltip />
                </PieChart>
              </ResponsiveContainer>
            </CardContent>
          </Card>
        </Grid>

        {/* Top Jobs by Execution */}
        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Top Jobs by Execution Count
              </Typography>
              <TableContainer>
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell>Job Name</TableCell>
                      <TableCell align="right">Executions</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {getTopJobsByExecution().map((job: {jobName: string, executionCount: number}, index: number) => (
                      <TableRow key={index}>
                        <TableCell>{job.jobName}</TableCell>
                        <TableCell align="right">
                          <Chip label={job.executionCount} size="small" />
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            </CardContent>
          </Card>
        </Grid>

        {/* Trigger Status */}
        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Trigger Status Distribution
              </Typography>
              <ResponsiveContainer width="100%" height={300}>
                <BarChart data={getTriggerStatusData()}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="name" />
                  <YAxis />
                  <Tooltip />
                  <Bar dataKey="value" fill="#8884d8" />
                </BarChart>
              </ResponsiveContainer>
            </CardContent>
          </Card>
        </Grid>

        {/* Performance Metrics */}
        <Grid item xs={12}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Performance Metrics
              </Typography>
              <Grid container spacing={2}>
                <Grid item xs={12} sm={6} md={3}>
                  <Paper sx={{ p: 2, textAlign: 'center' }}>
                    <Typography variant="h6" color="primary">
                      {formatExecutionTime(jobStats.averageExecutionTime)}
                    </Typography>
                    <Typography variant="body2" color="textSecondary">
                      Avg Execution Time
                    </Typography>
                  </Paper>
                </Grid>
                <Grid item xs={12} sm={6} md={3}>
                  <Paper sx={{ p: 2, textAlign: 'center' }}>
                    <Typography variant="h6" color="success">
                      {jobStats.successfulExecutions}
                    </Typography>
                    <Typography variant="body2" color="textSecondary">
                      Successful Executions
                    </Typography>
                  </Paper>
                </Grid>
                <Grid item xs={12} sm={6} md={3}>
                  <Paper sx={{ p: 2, textAlign: 'center' }}>
                    <Typography variant="h6" color="error">
                      {jobStats.failedExecutions}
                    </Typography>
                    <Typography variant="body2" color="textSecondary">
                      Failed Executions
                    </Typography>
                  </Paper>
                </Grid>
                <Grid item xs={12} sm={6} md={3}>
                  <Paper sx={{ p: 2, textAlign: 'center' }}>
                    <Typography variant="h6" color="info">
                      {jobStats.totalExecutions}
                    </Typography>
                    <Typography variant="body2" color="textSecondary">
                      Total Executions
                    </Typography>
                  </Paper>
                </Grid>
              </Grid>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Box>
  );
};

export default StatisticsPage;
