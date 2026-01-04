import React, { useState } from 'react';
import {
  Box,
  Typography,
  Card,
  CardContent,
  Grid,
  Chip,
  Button,
  Divider,
  FormControlLabel,
  Switch,
  Alert,
  CircularProgress,
  Tabs,
  Tab
} from '@mui/material';
import {
  Edit as EditIcon,
  PlayArrow as PlayIcon,
  Add as AddIcon
} from '@mui/icons-material';
import { useQuery } from 'react-query';
import { CronJobModel, JobMetadata, JobParameter, ParameterType } from '../../types';
import { jobMetadataApi } from '../../services/api';

interface TabPanelProps {
  children?: React.ReactNode;
  index: number;
  value: number;
}

function TabPanel(props: TabPanelProps) {
  const { children, value, index, ...other } = props;
  return (
    <div
      role="tabpanel"
      hidden={value !== index}
      id={`details-tabpanel-${index}`}
      aria-labelledby={`details-tab-${index}`}
      {...other}
    >
      {value === index && <Box sx={{ p: 0 }}>{children}</Box>}
    </div>
  );
}

interface CronJobDetailsProps {
  selectedCronJob: CronJobModel | null;
  onEditCronJob: () => void;
  onRunCronJob: () => void;
  onCreateNew: () => void;
}

const CronJobDetails: React.FC<CronJobDetailsProps> = ({
  selectedCronJob,
  onEditCronJob,
  onRunCronJob,
  onCreateNew
}) => {
  const [detailsTabValue, setDetailsTabValue] = useState(0);

  // Use React Query for job metadata
  const { data: jobMetadata, isLoading: loading, error: queryError } = useQuery(
    ['jobMetadata', selectedCronJob?.jobBeanName],
    async () => {
      if (!selectedCronJob?.jobBeanName) {
        return null;
      }
      const response = await jobMetadataApi.getByBeanName(selectedCronJob.jobBeanName);
      return response.data;
    },
    { enabled: !!selectedCronJob?.jobBeanName }
  );

  const error = queryError ? 'Error loading job metadata' : null;

  const formatDateTime = (dateTime?: string) => {
    if (!dateTime) return '-';
    const date = new Date(dateTime);
    const dateStr = date.toLocaleDateString('en-US', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit'
    });
    const timeStr = date.toLocaleTimeString('en-US', {
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      hour12: false
    });
    const milliseconds = date.getMilliseconds().toString().padStart(3, '0');
    const timeWithMs = `${timeStr}.${milliseconds}`;
    return { date: dateStr, time: timeWithMs };
  };

  const getStatusColor = (status?: string) => {
    switch (status) {
      case 'RUNNING':
        return 'success';
      case 'PAUSED':
        return 'warning';
      case 'ERROR':
        return 'error';
      default:
        return 'default';
    }
  };

  const renderParameterValue = (param: JobParameter, value: any) => {
    if (value === undefined || value === null) {
      return <Typography variant="body2" color="textSecondary">N/A</Typography>;
    }

    switch (param.type) {
      case ParameterType.BOOLEAN:
        return (
          <FormControlLabel
            control={<Switch checked={Boolean(value)} disabled />}
            label={value ? 'Yes' : 'No'}
            sx={{ m: 0 }}
          />
        );

      case ParameterType.INTEGER:
        return (
          <Typography variant="body1" fontWeight="medium">
            {Number(value).toLocaleString()}
          </Typography>
        );

      case ParameterType.DATE:
        const dateValue = new Date(value);
        return (
          <Box>
            <Typography variant="body2" fontWeight="medium">
              {dateValue.toLocaleDateString('en-US', {
                year: 'numeric',
                month: '2-digit',
                day: '2-digit'
              })}
            </Typography>
            <Typography variant="caption" color="textSecondary">
              {dateValue.toLocaleTimeString('en-US', {
                hour: '2-digit',
                minute: '2-digit',
                second: '2-digit',
                hour12: false
              })}
            </Typography>
          </Box>
        );

      case ParameterType.JSON:
        return (
          <Box sx={{ 
            mt: 1, 
            p: 2, 
            bgcolor: 'grey.50', 
            borderRadius: 1, 
            border: '1px solid',
            borderColor: 'grey.200'
          }}>
            <Typography 
              variant="body2" 
              component="pre" 
              sx={{ 
                fontFamily: 'monospace', 
                fontSize: '0.875rem',
                whiteSpace: 'pre-wrap',
                wordBreak: 'break-word',
                margin: 0
              }}
            >
              {JSON.stringify(value, null, 2)}
            </Typography>
          </Box>
        );

      case ParameterType.TEXTAREA:
        return (
          <Box sx={{ 
            mt: 1, 
            p: 2, 
            bgcolor: 'grey.50', 
            borderRadius: 1, 
            border: '1px solid',
            borderColor: 'grey.200'
          }}>
            <Typography 
              variant="body2" 
              sx={{ 
                whiteSpace: 'pre-wrap',
                wordBreak: 'break-word',
                margin: 0
              }}
            >
              {String(value)}
            </Typography>
          </Box>
        );

      case ParameterType.ENUM:
        return (
          <Chip
            label={String(value)}
            color="primary"
            size="small"
          />
        );

      default:
        return (
          <Typography variant="body1" fontWeight="medium">
            {String(value)}
          </Typography>
        );
    }
  };

  if (!selectedCronJob) {
    return (
      <Box sx={{ p: 3, textAlign: 'center' }}>
        <Typography variant="h6" color="textSecondary" gutterBottom>
          No Cron Job Selected
        </Typography>
        <Typography variant="body2" color="textSecondary" sx={{ mb: 3 }}>
          Select a cron job from the list to view details
        </Typography>
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={onCreateNew}
        >
          Create New CronJob
        </Button>
      </Box>
    );
  }

  return (
    <Box sx={{ p: { xs: 1, sm: 2 } }}>
      {/* Header */}
      <Box 
        display="flex" 
        justifyContent="space-between" 
        alignItems="center" 
        mb={3}
        flexDirection={{ xs: 'column', sm: 'row' }}
        gap={{ xs: 2, sm: 0 }}
      >
        <Box sx={{ flex: 1, minWidth: 0 }}>
          <Typography 
            variant="h5" 
            fontWeight="bold" 
            gutterBottom
            sx={{ 
              fontSize: { xs: '1.25rem', sm: '1.5rem' },
              wordBreak: 'break-word'
            }}
          >
            {selectedCronJob.name}
          </Typography>
          <Box 
            display="flex" 
            alignItems="center" 
            gap={2}
            flexWrap="wrap"
          >
            <Chip
              label={selectedCronJob.status || 'UNKNOWN'}
              color={getStatusColor(selectedCronJob.status) as any}
              size="small"
              sx={{ 
                fontSize: { xs: '0.65rem', sm: '0.75rem' },
                height: { xs: 20, sm: 24 }
              }}
            />
            <Typography 
              variant="body2" 
              color="textSecondary"
              sx={{ 
                fontSize: { xs: '0.75rem', sm: '0.875rem' },
                wordBreak: 'break-word'
              }}
            >
              {selectedCronJob.description || 'No description'}
            </Typography>
          </Box>
        </Box>
        <Box display="flex" gap={1} flexWrap="wrap">
          <Button
            variant="outlined"
            startIcon={<EditIcon />}
            onClick={onEditCronJob}
            size="small"
            sx={{ minWidth: { xs: 'calc(50% - 4px)', sm: 'auto' } }}
          >
            Edit
          </Button>
          <Button
            variant="contained"
            startIcon={<PlayIcon />}
            onClick={onRunCronJob}
            size="small"
            sx={{ minWidth: { xs: 'calc(50% - 4px)', sm: 'auto' } }}
          >
            Run Now
          </Button>
        </Box>
      </Box>

      <Divider sx={{ mb: 3 }} />

      {/* Details Tabs */}
      <Box sx={{ borderBottom: 1, borderColor: 'divider', mb: 2 }}>
        <Tabs
          value={detailsTabValue}
          onChange={(e, newValue) => setDetailsTabValue(newValue)}
          aria-label="details tabs"
        >
          <Tab label="Basic Information" />
          <Tab label="Job Parameters" />
        </Tabs>
      </Box>

      <TabPanel value={detailsTabValue} index={0}>
        {/* Basic Information Tab */}
        <Grid container spacing={3}>
        {/* Basic Information */}
        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Basic Information
              </Typography>
              <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.5 }}>
                <Box>
                  <Typography variant="body2" color="textSecondary">
                    Job Code
                  </Typography>
                  <Typography variant="body1" fontWeight="medium">
                    {selectedCronJob.code || 'N/A'}
                  </Typography>
                </Box>
                <Box>
                  <Typography variant="body2" color="textSecondary">
                    Job Bean Name
                  </Typography>
                  <Typography variant="body1" fontWeight="medium">
                    {selectedCronJob.jobBeanName || 'N/A'}
                  </Typography>
                </Box>
                <Box>
                  <Typography variant="body2" color="textSecondary">
                    Log Level
                  </Typography>
                  <Typography variant="body1" fontWeight="medium">
                    {selectedCronJob.logLevel || 'N/A'}
                  </Typography>
                </Box>
                <Box>
                  <Typography variant="body2" color="textSecondary">
                    Enabled
                  </Typography>
                  <Chip
                    label={selectedCronJob.enabled ? 'Yes' : 'No'}
                    color={selectedCronJob.enabled ? 'success' : 'default'}
                    size="small"
                  />
                </Box>
              </Box>
            </CardContent>
          </Card>
        </Grid>

        {/* Last Execution */}
        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Last Execution
              </Typography>
              <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.5 }}>
                <Box>
                  <Typography variant="body2" color="textSecondary">
                    Last Start Time
                  </Typography>
                  {(() => {
                    const dateTime = formatDateTime(selectedCronJob.lastStartTime);
                    if (dateTime === '-') return <Typography variant="body1" fontWeight="medium">N/A</Typography>;
                    return (
                      <Box>
                        <Typography variant="body2" fontWeight="medium">
                          {dateTime.date}
                        </Typography>
                        <Typography variant="caption" color="textSecondary">
                          {dateTime.time}
                        </Typography>
                      </Box>
                    );
                  })()}
                </Box>
                <Box>
                  <Typography variant="body2" color="textSecondary">
                    Last End Time
                  </Typography>
                  {(() => {
                    const dateTime = formatDateTime(selectedCronJob.lastEndTime);
                    if (dateTime === '-') return <Typography variant="body1" fontWeight="medium">N/A</Typography>;
                    return (
                      <Box>
                        <Typography variant="body2" fontWeight="medium">
                          {dateTime.date}
                        </Typography>
                        <Typography variant="caption" color="textSecondary">
                          {dateTime.time}
                        </Typography>
                      </Box>
                    );
                  })()}
                </Box>
                <Box>
                  <Typography variant="body2" color="textSecondary">
                    Last Result
                  </Typography>
                  <Chip
                    label={selectedCronJob.lastResult || 'N/A'}
                    color={selectedCronJob.lastResult === 'SUCCESS' ? 'success' : 
                           selectedCronJob.lastResult === 'FAILED' ? 'error' : 'default'}
                    size="small"
                  />
                </Box>
              </Box>
            </CardContent>
          </Card>
        </Grid>

        {/* Retry Information */}
        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Retry Information
              </Typography>
              <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.5 }}>
                <Box>
                  <Typography variant="body2" color="textSecondary">
                    Current Retry Count
                  </Typography>
                  <Typography variant="body1" fontWeight="medium">
                    {selectedCronJob.retryCount || 0}
                  </Typography>
                </Box>
                <Box>
                  <Typography variant="body2" color="textSecondary">
                    Max Retries
                  </Typography>
                  <Typography variant="body1" fontWeight="medium">
                    {selectedCronJob.maxRetries || 0}
                  </Typography>
                </Box>
                <Box>
                  <Typography variant="body2" color="textSecondary">
                    Remaining Retries
                  </Typography>
                  <Typography variant="body1" fontWeight="medium">
                    {Math.max(0, (selectedCronJob.maxRetries || 0) - (selectedCronJob.retryCount || 0))}
                  </Typography>
                </Box>
              </Box>
            </CardContent>
          </Card>
        </Grid>

        {/* System Information */}
        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                System Information
              </Typography>
              <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.5 }}>
                <Box>
                  <Typography variant="body2" color="textSecondary">
                    Node ID
                  </Typography>
                  <Typography variant="body1" fontWeight="medium">
                    {selectedCronJob.nodeId || 'N/A'}
                  </Typography>
                </Box>
                <Box>
                  <Typography variant="body2" color="textSecondary">
                    Correlation ID
                  </Typography>
                  <Typography variant="body1" fontWeight="medium" sx={{ wordBreak: 'break-all' }}>
                    {selectedCronJob.correlationId || 'N/A'}
                  </Typography>
                </Box>
                <Box>
                  <Typography variant="body2" color="textSecondary">
                    Created At
                  </Typography>
                  {(() => {
                    const dateTime = formatDateTime(selectedCronJob.createdAt);
                    if (dateTime === '-') return <Typography variant="body1" fontWeight="medium">N/A</Typography>;
                    return (
                      <Box>
                        <Typography variant="body2" fontWeight="medium">
                          {dateTime.date}
                        </Typography>
                        <Typography variant="caption" color="textSecondary">
                          {dateTime.time}
                        </Typography>
                      </Box>
                    );
                  })()}
                </Box>
                <Box>
                  <Typography variant="body2" color="textSecondary">
                    Updated At
                  </Typography>
                  {(() => {
                    const dateTime = formatDateTime(selectedCronJob.updatedAt);
                    if (dateTime === '-') return <Typography variant="body1" fontWeight="medium">N/A</Typography>;
                    return (
                      <Box>
                        <Typography variant="body2" fontWeight="medium">
                          {dateTime.date}
                        </Typography>
                        <Typography variant="caption" color="textSecondary">
                          {dateTime.time}
                        </Typography>
                      </Box>
                    );
                  })()}
                </Box>
              </Box>
            </CardContent>
          </Card>
        </Grid>

        </Grid>
      </TabPanel>

      <TabPanel value={detailsTabValue} index={1}>
        {/* Job Parameters Tab */}
        <Card>
          <CardContent>
            <Typography variant="h6" gutterBottom>
              Job Parameters
            </Typography>
            
            {loading && (
              <Box display="flex" justifyContent="center" py={2}>
                <CircularProgress size={24} />
              </Box>
            )}

            {error && (
              <Alert severity="error" sx={{ mb: 2 }}>
                {error}
              </Alert>
            )}

            {!loading && !error && jobMetadata && jobMetadata.parameters.length > 0 && (
              <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                <Typography variant="body2" color="textSecondary" gutterBottom>
                  Job Parameters
                </Typography>
                <Typography variant="body2" color="textSecondary" sx={{ mb: 2 }}>
                  {jobMetadata.description}
                </Typography>
                
                <Grid container spacing={2}>
                  {jobMetadata.parameters.map((param) => (
                    <Grid item xs={12} sm={6} key={param.name}>
                      <Box sx={{ 
                        p: 2, 
                        border: '1px solid', 
                        borderColor: 'grey.200', 
                        borderRadius: 1,
                        bgcolor: 'grey.50'
                      }}>
                        <Box display="flex" alignItems="center" gap={1} mb={1}>
                          <Typography variant="subtitle2" fontWeight="medium">
                            {param.displayName}
                          </Typography>
                          {param.required && (
                            <Chip
                              label="Required"
                              size="small"
                              color="primary"
                            />
                          )}
                          <Chip
                            label={param.type}
                            size="small"
                            variant="outlined"
                          />
                        </Box>
                        
                        {param.description && (
                          <Typography variant="caption" color="textSecondary" display="block" sx={{ mb: 1 }}>
                            {param.description}
                          </Typography>
                        )}
                        
                        {renderParameterValue(param, selectedCronJob.parameters?.[param.name])}
                      </Box>
                    </Grid>
                  ))}
                </Grid>
              </Box>
            )}

            {!loading && !error && (!jobMetadata || jobMetadata.parameters.length === 0) && (
              <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.5 }}>
                <Typography variant="body2" color="textSecondary">
                  Parameters
                </Typography>
                <Box sx={{ 
                  mt: 1, 
                  p: 2, 
                  bgcolor: 'grey.50', 
                  borderRadius: 1, 
                  border: '1px solid',
                  borderColor: 'grey.200'
                }}>
                  <Typography 
                    variant="body2" 
                    component="pre" 
                    sx={{ 
                      fontFamily: 'monospace', 
                      fontSize: '0.875rem',
                      whiteSpace: 'pre-wrap',
                      wordBreak: 'break-word',
                      margin: 0
                    }}
                  >
                    {selectedCronJob.parameters ? 
                      JSON.stringify(selectedCronJob.parameters, null, 2) : 
                      'No parameters configured'
                    }
                  </Typography>
                </Box>
              </Box>
            )}
          </CardContent>
        </Card>
      </TabPanel>
    </Box>
  );
};

export default CronJobDetails;
