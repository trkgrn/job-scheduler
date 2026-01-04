import React, { useState, useEffect } from 'react';
import {
  Box,
  Paper,
  Typography,
  Tabs,
  Tab,
  Button,
  LinearProgress,
} from '@mui/material';
import {
  Refresh as RefreshIcon,
  Add as AddIcon,
} from '@mui/icons-material';
import { useQuery, useQueryClient } from 'react-query';
import { useSidebar } from '../../contexts/SidebarContext';
import { cronJobApi, triggerApi, jobExecutionApi, jobMetadataApi } from '../../services/api';
import { CronJobModel, TriggerModel, JobExecution, LogEntry, JobMetadata } from '../../types';
import CronJobForm from '../CronJobs/CronJobForm';
import CronJobList from './CronJobList';
import CronJobTable from './CronJobTable';
import CronJobDetails from './CronJobDetails';
import TriggerTable from './TriggerTable';
import ExecutionTable from './ExecutionTable';
import LogsModal from './LogsModal';
import TriggerForm from './TriggerForm';
import QuickSelectModal from './QuickSelectModal';
import ResizableModal from './ResizableModal';

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
      id={`dashboard-tabpanel-${index}`}
      aria-labelledby={`dashboard-tab-${index}`}
      {...other}
    >
      {value === index && <Box sx={{ p: 3 }}>{children}</Box>}
    </div>
  );
}

const CronJobDashboard: React.FC = () => {
  const { isSidebarOpen } = useSidebar();
  const [selectedCronJob, setSelectedCronJob] = useState<CronJobModel | null>(null);
  const [selectedCronJobs, setSelectedCronJobs] = useState<CronJobModel[]>([]);
  const [selectedExecution, setSelectedExecution] = useState<JobExecution | null>(null);
  const [tabValue, setTabValue] = useState(0);
  const [logs, setLogs] = useState<LogEntry[]>([]);
  const [isAutoRefresh, setIsAutoRefresh] = useState(false);
  const [executionPage, setExecutionPage] = useState(0);
  const [executionRowsPerPage, setExecutionRowsPerPage] = useState(10);
  const [filterStatus, setFilterStatus] = useState('');
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const [triggerDialogOpen, setTriggerDialogOpen] = useState(false);
  const [editingTrigger, setEditingTrigger] = useState<TriggerModel | null>(null);
  const [cronJobFormOpen, setCronJobFormOpen] = useState(false);
  const [editingCronJob, setEditingCronJob] = useState<CronJobModel | null>(null);
  const [quickSelectModalOpen, setQuickSelectModalOpen] = useState(false);
  const [logsModalOpen, setLogsModalOpen] = useState(false);
  const [detailModalOpen, setDetailModalOpen] = useState(false);
  const [viewMode, setViewMode] = useState<'list' | 'table'>('table');
  const [selectedCronExpression, setSelectedCronExpression] = useState<string>('');

  const queryClient = useQueryClient();

  // API Queries
  const { data: cronJobsResponse, isLoading: cronJobsLoading } = useQuery('cronJobs', cronJobApi.getAll);
  // Only fetch triggers when a cron job is selected
  const { data: triggersResponse } = useQuery(
    ['triggers', selectedCronJob?.id],
    async () => {
      if (!selectedCronJob?.id) {
        return { data: [] };
      }
      return await triggerApi.getByCronJobId(selectedCronJob.id);
    },
    { enabled: !!selectedCronJob?.id }
  );
  const { data: executionsResponse } = 
    useQuery(
      ['executions', selectedCronJob?.id, executionPage, executionRowsPerPage, filterStatus],
      async () => {
        if (!selectedCronJob?.id) {
          return { data: { content: [], totalElements: 0 } };
        }
        return await jobExecutionApi.getByCronJobIdPaginated(
          selectedCronJob.id,
          executionPage,
          executionRowsPerPage,
          filterStatus || undefined
        );
      },
      { enabled: !!selectedCronJob?.id }
    );

  const cronJobs = cronJobsResponse?.data || [];
  const triggers = triggersResponse?.data || [];
  const executions = executionsResponse?.data?.content || [];
  const totalExecutions = executionsResponse?.data?.totalElements || 0;

  // Fetch job metadata to check if job is abortable
  const { data: jobMetadataResponse, error: jobMetadataError } = useQuery(
    ['jobMetadata', selectedCronJob?.jobBeanName],
    async () => {
      if (!selectedCronJob?.jobBeanName) {
        return null;
      }
      try {
        const response = await jobMetadataApi.getByBeanName(selectedCronJob.jobBeanName);
        console.log('Job Metadata API Response:', response);
        // Response is DataResult<JobMetadata>, so we need to check response.data
        if (response && response.success && response.data) {
          return response.data;
        }
        return null;
      } catch (error: any) {
        // If 404, job not found - return null (not abortable)
        if (error.response?.status === 404) {
          console.warn('Job metadata not found for:', selectedCronJob.jobBeanName);
          return null;
        }
        console.error('Failed to fetch job metadata:', error);
        console.error('Error details:', error.response?.data);
        return null;
      }
    },
    { 
      enabled: !!selectedCronJob?.jobBeanName,
      retry: false // Don't retry on 404
    }
  );

  const jobMetadata: JobMetadata | null = jobMetadataResponse || null;
  const isAbortable = jobMetadata?.abortable || false;
  
  // Debug: Log abortable status
  useEffect(() => {
    if (selectedCronJob?.jobBeanName) {
      console.log('Job Bean Name:', selectedCronJob.jobBeanName);
      console.log('Job Metadata Response:', jobMetadataResponse);
      console.log('Job Metadata Error:', jobMetadataError);
      console.log('Job Metadata:', jobMetadata);
      console.log('Is Abortable:', isAbortable);
    }
  }, [selectedCronJob?.jobBeanName, jobMetadataResponse, jobMetadataError, jobMetadata, isAbortable]);

  // Triggers are already filtered by API call
  const cronJobTriggers = triggers;

  // Auto refresh for logs
  useEffect(() => {
    let interval: NodeJS.Timeout;
    if (isAutoRefresh && selectedExecution) {
      interval = setInterval(() => {
        fetchLogs(selectedExecution.id!);
      }, 2000);
    }
    return () => {
      if (interval) clearInterval(interval);
    };
  }, [isAutoRefresh, selectedExecution]);

  // Fetch logs when execution is selected
  const fetchLogs = async (executionId: number) => {
    try {
      const logsData = await jobExecutionApi.getLogs(executionId);
      setLogs(logsData.data || []);
    } catch (error) {
      console.error('Failed to fetch logs:', error);
    }
  };

  // Event handlers
  const handleSelectCronJob = (cronJob: CronJobModel) => {
    setSelectedCronJob(cronJob);
    setDetailModalOpen(true);
    setTabValue(0); // Switch to details tab
  };

  const handleSelectMultipleCronJobs = (cronJobs: CronJobModel[]) => {
    setSelectedCronJobs(cronJobs);
  };

  const handleDeleteMultipleCronJobs = async (cronJobs: CronJobModel[]) => {
    if (window.confirm(`Are you sure you want to delete ${cronJobs.length} cron job(s)?`)) {
      try {
        await Promise.all(cronJobs.map(cronJob => cronJobApi.delete(cronJob.id!)));
        queryClient.invalidateQueries('cronJobs');
        setSelectedCronJobs([]);
      } catch (error) {
        console.error('Failed to delete cron jobs:', error);
      }
    }
  };

  const handleEditCronJob = () => {
    setEditingCronJob(selectedCronJob);
    setCronJobFormOpen(true);
  };

  const handleRunCronJob = async (cronJob: CronJobModel) => {
    try {
      await cronJobApi.runNow(cronJob.id!);
      
      // Invalidate all execution queries to ensure the new execution is visible
      queryClient.invalidateQueries('executions');
      queryClient.invalidateQueries(['executions', cronJob.id]);
      queryClient.invalidateQueries('cronJobs');
      
      // If this cronJob is selected, refresh its executions and reset to first page
      if (selectedCronJob?.id === cronJob.id) {
        setExecutionPage(0);
        // Wait a bit for backend to commit the transaction, then refetch
        setTimeout(() => {
          queryClient.refetchQueries(['executions', cronJob.id, 0, executionRowsPerPage, filterStatus]);
        }, 500);
      } else {
        // If not selected, still refresh after a delay to ensure execution is visible when user selects it
        setTimeout(() => {
          queryClient.invalidateQueries(['executions', cronJob.id]);
        }, 500);
      }
    } catch (error: any) {
      console.error('Failed to run cron job:', error);
      // Show error message to user
      if (error.response?.data?.message) {
        alert(error.response.data.message);
      } else {
        alert('Failed to run cron job: ' + (error.message || 'Unknown error'));
      }
    }
  };

  const handleDeleteCronJob = async (cronJob: CronJobModel) => {
    if (window.confirm(`Are you sure you want to delete "${cronJob.name || cronJob.code}"?`)) {
      try {
        await cronJobApi.delete(cronJob.id!);
        queryClient.invalidateQueries('cronJobs');
        if (selectedCronJob?.id === cronJob.id) {
          setSelectedCronJob(null);
        }
      } catch (error) {
        console.error('Failed to delete cron job:', error);
      }
    }
  };

  const handleMenuOpen = (event: React.MouseEvent<HTMLElement>, cronJob: CronJobModel) => {
    setAnchorEl(event.currentTarget);
    setSelectedCronJob(cronJob);
  };

  const handleMenuClose = () => {
    setAnchorEl(null);
  };

  const handleTriggerDialogOpen = (trigger?: TriggerModel) => {
    setEditingTrigger(trigger || null);
    setTriggerDialogOpen(true);
  };

  const handleTriggerSubmit = async (triggerData: any) => {
    try {
      const triggerPayload = {
        ...triggerData,
        enabled: triggerData.enabled !== undefined ? triggerData.enabled : true
      };
      
      if (editingTrigger) {
        await triggerApi.update(editingTrigger.id!, triggerPayload);
      } else {
        await triggerApi.create({
          ...triggerPayload,
          cronJobId: selectedCronJob?.id,
        });
      }
      queryClient.invalidateQueries('triggers');
      setTriggerDialogOpen(false);
      setEditingTrigger(null);
    } catch (error) {
      console.error('Failed to save trigger:', error);
    }
  };

  const handleDeleteTrigger = async (triggerId: number) => {
    if (window.confirm('Are you sure you want to delete this trigger?')) {
      try {
        await triggerApi.delete(triggerId);
        queryClient.invalidateQueries('triggers');
      } catch (error) {
        console.error('Failed to delete trigger:', error);
      }
    }
  };

  const handleExecutionPageChange = (event: unknown, newPage: number) => {
    setExecutionPage(newPage);
  };

  const handleExecutionRowsPerPageChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    setExecutionRowsPerPage(parseInt(event.target.value, 10));
    setExecutionPage(0);
  };

  const handleStatusFilterChange = (status: string) => {
    setFilterStatus(status);
    setExecutionPage(0);
  };

  const handleViewLogs = async (execution: JobExecution) => {
    setSelectedExecution(execution);
    setLogsModalOpen(true);
    await fetchLogs(execution.id!);
  };

  const handleAbortExecution = async (execution: JobExecution) => {
    if (!window.confirm(`Are you sure you want to abort execution #${execution.id}?`)) {
      return;
    }
    
    try {
      await jobExecutionApi.cancel(execution.id!);
      // Refresh executions to show updated status
      queryClient.invalidateQueries(['executions', selectedCronJob?.id]);
      queryClient.refetchQueries(['executions', selectedCronJob?.id, executionPage, executionRowsPerPage, filterStatus]);
    } catch (error: any) {
      console.error('Failed to abort execution:', error);
      if (error.response?.data?.message) {
        alert(error.response.data.message);
      } else {
        alert('Failed to abort execution: ' + (error.message || 'Unknown error'));
      }
    }
  };

  const handleRefreshLogs = async () => {
    if (selectedExecution) {
      await fetchLogs(selectedExecution.id!);
    }
  };

  const handleDownloadLogs = () => {
    const logText = logs.map(log => 
      `[${log.timestamp}] ${log.level}: ${log.message}`
    ).join('\n');
    
    const blob = new Blob([logText], { type: 'text/plain' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `execution-${selectedExecution?.id}-logs.txt`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  };

  const handleQuickSelect = (expression: string) => {
    // Pass the selected expression to TriggerForm
    if (triggerDialogOpen) {
      // We need to pass this to TriggerForm somehow
      // For now, we'll use a state to store the selected expression
      setSelectedCronExpression(expression);
    }
    setQuickSelectModalOpen(false);
  };

  const handleRefreshCronJobDetails = () => {
    // Refresh all queries related to the selected cron job
    if (selectedCronJob?.id) {
      queryClient.invalidateQueries(['triggers', selectedCronJob.id]);
      queryClient.invalidateQueries(['executions', selectedCronJob.id]);
      queryClient.invalidateQueries(['jobMetadata', selectedCronJob.jobBeanName]);
    }
  };

  if (cronJobsLoading) {
    return <LinearProgress />;
  }

  return (
    <Box sx={{ height: '100vh', display: 'flex', flexDirection: 'column' }}>
      {/* Header */}
      <Paper sx={{ p: 2, mb: 2 }}>
        <Box display="flex" justifyContent="space-between" alignItems="center">
          <Typography variant="h4" component="h1">
            Cronjobs
          </Typography>
          <Box display="flex" gap={1}>
            <Button
              variant="outlined"
              startIcon={<RefreshIcon />}
              onClick={() => queryClient.invalidateQueries()}
            >
              Refresh
            </Button>
            <Button
              variant="contained"
              startIcon={<AddIcon />}
              onClick={() => setCronJobFormOpen(true)}
            >
              Create New CronJob
            </Button>
          </Box>
        </Box>
      </Paper>

      {/* Main Content */}
      <Box sx={{ 
        flex: 1, 
        display: 'flex', 
        flexDirection: 'column',
        minHeight: 0,
      }}>
        {/* CronJob Table */}
        <CronJobTable
          cronJobs={cronJobs}
          selectedCronJobs={selectedCronJobs}
          onSelectCronJob={handleSelectCronJob}
          onSelectMultipleCronJobs={handleSelectMultipleCronJobs}
          onEditCronJob={handleEditCronJob}
          onRunCronJob={handleRunCronJob}
          onDeleteCronJob={handleDeleteCronJob}
          onDeleteMultipleCronJobs={handleDeleteMultipleCronJobs}
          loading={cronJobsLoading}
        />

      </Box>

      {/* Modals */}
      {/* Detail Modal */}
      <ResizableModal
        open={detailModalOpen}
        onClose={() => setDetailModalOpen(false)}
        title={`Cron Job Details - ${selectedCronJob?.name || selectedCronJob?.code || 'Unknown'}`}
        initialWidth="100%"
        initialHeight="50vh"
        minWidth={800}
        minHeight={400}
        maxHeight="80vh"
        sidebarOpen={isSidebarOpen}
        actions={
          <Button
            variant="outlined"
            startIcon={<RefreshIcon />}
            onClick={handleRefreshCronJobDetails}
            size="small"
          >
            Refresh
          </Button>
        }
      >
        {selectedCronJob && (
          <Box sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
            {/* Tabs */}
            <Tabs
              value={tabValue}
              onChange={(e, newValue) => setTabValue(newValue)}
              sx={{ 
                borderBottom: 1, 
                borderColor: 'divider',
                px: 2,
                pt: 1
              }}
            >
              <Tab label="Details" />
              <Tab label="Triggers" />
              <Tab label="Executions" />
            </Tabs>

            {/* Tab Content */}
            <Box sx={{ flex: 1, overflow: 'auto', p: 2 }}>
              <TabPanel value={tabValue} index={0}>
                <CronJobDetails
                  selectedCronJob={selectedCronJob}
                  onEditCronJob={handleEditCronJob}
                  onRunCronJob={() => handleRunCronJob(selectedCronJob)}
                  onCreateNew={() => {
                    setEditingCronJob(null);
                    setSelectedCronJob(null);
                    setCronJobFormOpen(true);
                  }}
                />
              </TabPanel>

              <TabPanel value={tabValue} index={1}>
                <TriggerTable
                  triggers={cronJobTriggers}
                  onAddTrigger={() => handleTriggerDialogOpen()}
                  onEditTrigger={handleTriggerDialogOpen}
                  onDeleteTrigger={handleDeleteTrigger}
                />
              </TabPanel>

              <TabPanel value={tabValue} index={2}>
                <ExecutionTable
                  executions={executions}
                  page={executionPage}
                  rowsPerPage={executionRowsPerPage}
                  totalCount={totalExecutions}
                  filterStatus={filterStatus}
                  isAbortable={isAbortable}
                  onPageChange={handleExecutionPageChange}
                  onRowsPerPageChange={handleExecutionRowsPerPageChange}
                  onStatusFilterChange={handleStatusFilterChange}
                  onViewLogs={handleViewLogs}
                  onAbort={handleAbortExecution}
                />
              </TabPanel>
            </Box>
          </Box>
        )}
      </ResizableModal>

      <CronJobForm
        open={cronJobFormOpen}
        onClose={() => {
          setCronJobFormOpen(false);
          setEditingCronJob(null);
        }}
        cronJob={editingCronJob}
        onSuccess={(updatedCronJob) => {
          setCronJobFormOpen(false);
          setEditingCronJob(null);
          queryClient.invalidateQueries('cronJobs');
          
          // If we were editing the selected cron job, update it with the new data
          if (selectedCronJob && updatedCronJob && selectedCronJob.id === updatedCronJob.id) {
            setSelectedCronJob(updatedCronJob);
            
            // Also refetch the cron job details for the detail modal
            queryClient.invalidateQueries(['triggers', selectedCronJob.id]);
            queryClient.invalidateQueries(['executions', selectedCronJob.id]);
            queryClient.invalidateQueries(['jobMetadata', selectedCronJob.jobBeanName]);
          }
        }}
      />

      <TriggerForm
        open={triggerDialogOpen}
        onClose={() => setTriggerDialogOpen(false)}
        onSubmit={handleTriggerSubmit}
        editingTrigger={editingTrigger}
        onQuickSelect={() => setQuickSelectModalOpen(true)}
        selectedCronExpression={selectedCronExpression}
        onCronExpressionSelect={setSelectedCronExpression}
      />

      <QuickSelectModal
        open={quickSelectModalOpen}
        onClose={() => setQuickSelectModalOpen(false)}
        onSelect={handleQuickSelect}
      />

      <LogsModal
        open={logsModalOpen}
        onClose={() => setLogsModalOpen(false)}
        execution={selectedExecution}
        logs={logs}
        onRefresh={handleRefreshLogs}
        onDownload={handleDownloadLogs}
      />
    </Box>
  );
};

export default CronJobDashboard;