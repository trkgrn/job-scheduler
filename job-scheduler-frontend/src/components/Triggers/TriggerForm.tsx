import React, { useEffect, useState } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  TextField,
  FormControlLabel,
  Switch,
  Grid,
  Alert,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Box,
  Typography,
  Tabs,
  Tab,
  Card,
  CardContent,
  Chip,
  IconButton,
  Tooltip,
  Divider,
} from '@mui/material';
import { useForm, Controller } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import * as yup from 'yup';
import { useMutation, useQueryClient, useQuery } from 'react-query';
import { triggerApi, cronJobApi } from '../../services/api';
import { TriggerModel } from '../../types';
import {
  Schedule as ScheduleIcon,
  AccessTime as TimeIcon,
  CalendarToday as CalendarIcon,
  Refresh as RefreshIcon,
  ContentCopy as CopyIcon,
} from '@mui/icons-material';

interface TriggerFormProps {
  open: boolean;
  onClose: () => void;
  trigger?: TriggerModel | null;
  onSuccess: () => void;
}

interface TriggerFormData {
  name: string;
  cronJobId: number;
  cronExpression: string;
  description?: string;
  enabled: boolean;
  priority: number;
  misfireInstruction: string;
}

const schema = yup.object({
  name: yup.string().required('Name is required'),
  cronJobId: yup.number().required('CronJob is required'),
  cronExpression: yup.string().required('Cron Expression is required'),
  description: yup.string().optional(),
  enabled: yup.boolean().required(),
  priority: yup.number().min(0).max(10).required(),
  misfireInstruction: yup.string().required('Misfire Instruction is required'),
});

// Predefined cron expressions
const predefinedCronExpressions = [
  {
    category: 'Frequent',
    icon: <RefreshIcon />,
    expressions: [
      { name: 'Every minute', expression: '0 * * * * ?', description: 'Runs every minute' },
      { name: 'Every 2 minutes', expression: '0 */2 * * * ?', description: 'Runs every 2 minutes' },
      { name: 'Every 3 minutes', expression: '0 */3 * * * ?', description: 'Runs every 3 minutes' },
      { name: 'Every 5 minutes', expression: '0 */5 * * * ?', description: 'Runs every 5 minutes' },
      { name: 'Every 10 minutes', expression: '0 */10 * * * ?', description: 'Runs every 10 minutes' },
      { name: 'Every 15 minutes', expression: '0 */15 * * * ?', description: 'Runs every 15 minutes' },
      { name: 'Every 30 minutes', expression: '0 */30 * * * ?', description: 'Runs every 30 minutes' },
    ]
  },
  {
    category: 'Hourly',
    icon: <TimeIcon />,
    expressions: [
      { name: 'Every hour', expression: '0 0 * * * ?', description: 'Runs at the top of every hour' },
      { name: 'Every 2 hours', expression: '0 0 */2 * * ?', description: 'Runs every 2 hours' },
      { name: 'Every 4 hours', expression: '0 0 */4 * * ?', description: 'Runs every 4 hours' },
      { name: 'Every 6 hours', expression: '0 0 */6 * * ?', description: 'Runs every 6 hours' },
      { name: 'Every 8 hours', expression: '0 0 */8 * * ?', description: 'Runs every 8 hours' },
      { name: 'Every 12 hours', expression: '0 0 */12 * * ?', description: 'Runs every 12 hours' },
    ]
  },
  {
    category: 'Daily',
    icon: <CalendarIcon />,
    expressions: [
      { name: 'Daily at midnight', expression: '0 0 0 * * ?', description: 'Runs daily at midnight' },
      { name: 'Daily at 6 AM', expression: '0 0 6 * * ?', description: 'Runs daily at 6:00 AM' },
      { name: 'Daily at 9 AM', expression: '0 0 9 * * ?', description: 'Runs daily at 9:00 AM' },
      { name: 'Daily at noon', expression: '0 0 12 * * ?', description: 'Runs daily at noon' },
      { name: 'Daily at 6 PM', expression: '0 0 18 * * ?', description: 'Runs daily at 6:00 PM' },
      { name: 'Daily at 11 PM', expression: '0 0 23 * * ?', description: 'Runs daily at 11:00 PM' },
    ]
  },
  {
    category: 'Weekly',
    icon: <ScheduleIcon />,
    expressions: [
      { name: 'Weekly on Monday', expression: '0 0 9 ? * MON', description: 'Runs every Monday at 9:00 AM' },
      { name: 'Weekly on Friday', expression: '0 0 17 ? * FRI', description: 'Runs every Friday at 5:00 PM' },
      { name: 'Weekdays only', expression: '0 0 9 ? * MON-FRI', description: 'Runs weekdays at 9:00 AM' },
      { name: 'Weekends only', expression: '0 0 10 ? * SAT,SUN', description: 'Runs weekends at 10:00 AM' },
    ]
  },
  {
    category: 'Monthly',
    icon: <CalendarIcon />,
    expressions: [
      { name: 'Monthly on 1st', expression: '0 0 9 1 * ?', description: 'Runs monthly on the 1st at 9:00 AM' },
      { name: 'Monthly on 15th', expression: '0 0 9 15 * ?', description: 'Runs monthly on the 15th at 9:00 AM' },
      { name: 'Last day of month', expression: '0 0 9 L * ?', description: 'Runs on the last day of each month' },
    ]
  }
];

const TriggerForm: React.FC<TriggerFormProps> = ({ open, onClose, trigger, onSuccess }) => {
  const queryClient = useQueryClient();
  const isEdit = !!trigger;
  const [cronTabValue, setCronTabValue] = useState(0);

  const { data: cronJobsResponse } = useQuery('cronJobs', cronJobApi.getAll);
  const cronJobs = Array.isArray(cronJobsResponse?.data) ? cronJobsResponse.data : [];

  const {
    control,
    handleSubmit,
    reset,
    setValue,
    formState: { errors },
  } = useForm<TriggerFormData>({
    resolver: yupResolver(schema),
    defaultValues: {
      name: '',
      cronJobId: 0,
      cronExpression: '',
      description: '',
      enabled: true,
      priority: 5,
      misfireInstruction: 'MISFIRE_INSTRUCTION_SMART_POLICY',
    },
  });

  const createMutation = useMutation(triggerApi.create, {
    onSuccess: () => {
      queryClient.invalidateQueries('triggers');
      onSuccess();
    },
  });

  const updateMutation = useMutation(
    (data: TriggerFormData) => triggerApi.update(trigger!.id!, data),
    {
      onSuccess: () => {
        queryClient.invalidateQueries('triggers');
        onSuccess();
      },
    }
  );

  useEffect(() => {
    if (trigger) {
      reset({
        name: trigger.name || '',
        cronJobId: trigger.cronJobId || 0,
        cronExpression: trigger.cronExpression || '',
        description: trigger.description || '',
        enabled: trigger.enabled ?? true,
        priority: trigger.priority || 5,
        misfireInstruction: trigger.misfireInstruction || 'MISFIRE_INSTRUCTION_SMART_POLICY',
      });
    } else {
      reset({
        name: '',
        cronJobId: 0,
        cronExpression: '',
        description: '',
        enabled: true,
        priority: 5,
        misfireInstruction: 'MISFIRE_INSTRUCTION_SMART_POLICY',
      });
    }
  }, [trigger, reset]);

  const onSubmit = (data: TriggerFormData) => {
    if (isEdit) {
      updateMutation.mutate(data);
    } else {
      createMutation.mutate(data);
    }
  };

  const isLoading = createMutation.isLoading || updateMutation.isLoading;
  const error = createMutation.error || updateMutation.error;

  const getErrorMessage = (error: unknown): string => {
    if (error instanceof Error) {
      return error.message;
    }
    if (typeof error === 'string') {
      return error;
    }
    return 'An error occurred';
  };

  const hasError = Boolean(error);

  const handlePredefinedCronSelect = (expression: string) => {
    // Update the form field directly
    setValue('cronExpression', expression);
  };

  const copyToClipboard = (text: string) => {
    navigator.clipboard.writeText(text);
  };

  const misfireInstructions = [
    { value: 'MISFIRE_INSTRUCTION_SMART_POLICY', label: 'Smart Policy' },
    { value: 'MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY', label: 'Ignore Misfire' },
    { value: 'MISFIRE_INSTRUCTION_FIRE_ONCE_NOW', label: 'Fire Once Now' },
    { value: 'MISFIRE_INSTRUCTION_DO_NOTHING', label: 'Do Nothing' },
  ];

  return (
    <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
      <DialogTitle>{isEdit ? 'Edit Trigger' : 'Create Trigger'}</DialogTitle>
      <form onSubmit={handleSubmit(onSubmit)}>
        <DialogContent>
          {hasError && (
            <Alert severity="error" sx={{ mb: 2 }}>
              {getErrorMessage(error)}
            </Alert>
          )}
          
          <Grid container spacing={2}>
            <Grid item xs={12} sm={6}>
              <Controller
                name="name"
                control={control}
                render={({ field }) => (
                  <TextField
                    {...field}
                    label="Trigger Name"
                    fullWidth
                    error={!!errors.name}
                    helperText={errors.name?.message || 'A unique name for this trigger'}
                    placeholder="e.g., Daily Backup Trigger"
                  />
                )}
              />
            </Grid>
            
            <Grid item xs={12} sm={6}>
              <Controller
                name="cronJobId"
                control={control}
                render={({ field }) => (
                  <FormControl fullWidth error={!!errors.cronJobId}>
                    <InputLabel>CronJob</InputLabel>
                    <Select
                      {...field}
                      label="CronJob"
                      disabled={isEdit}
                    >
                      {cronJobs.map((cronJob) => (
                        <MenuItem key={cronJob.id} value={cronJob.id}>
                          <Box>
                            <Typography variant="body1">{cronJob.name}</Typography>
                            <Typography variant="caption" color="textSecondary">
                              {cronJob.code} - {cronJob.jobBeanName}
                            </Typography>
                          </Box>
                        </MenuItem>
                      ))}
                    </Select>
                    {errors.cronJobId && (
                      <Typography variant="caption" color="error">
                        {errors.cronJobId.message}
                      </Typography>
                    )}
                  </FormControl>
                )}
              />
            </Grid>
            
            <Grid item xs={12}>
              <Typography variant="h6" gutterBottom>
                Cron Expression
              </Typography>
              <Box sx={{ borderBottom: 1, borderColor: 'divider' }}>
                <Tabs value={cronTabValue} onChange={(e, newValue) => setCronTabValue(newValue)}>
                  <Tab label="Manual Entry" />
                  <Tab label="Quick Select" />
                </Tabs>
              </Box>

              {cronTabValue === 0 && (
                <Box sx={{ mt: 2 }}>
                  <Controller
                    name="cronExpression"
                    control={control}
                    render={({ field }) => (
                      <TextField
                        {...field}
                        label="Cron Expression"
                        fullWidth
                        error={!!errors.cronExpression}
                        helperText={errors.cronExpression?.message || 'e.g., 0 0 12 * * ? (daily at noon)'}
                        placeholder="0 0 12 * * ?"
                        InputProps={{
                          endAdornment: field.value && (
                            <Tooltip title="Copy to clipboard">
                              <IconButton
                                size="small"
                                onClick={() => copyToClipboard(field.value)}
                              >
                                <CopyIcon fontSize="small" />
                              </IconButton>
                            </Tooltip>
                          ),
                        }}
                      />
                    )}
                  />
                  <Typography variant="caption" color="textSecondary" sx={{ mt: 1, display: 'block' }}>
                    Format: second minute hour day month day-of-week
                  </Typography>
                </Box>
              )}

              {cronTabValue === 1 && (
                <Box sx={{ mt: 2 }}>
                  {predefinedCronExpressions.map((category, categoryIndex) => (
                    <Card key={categoryIndex} sx={{ mb: 2 }}>
                      <CardContent>
                        <Box display="flex" alignItems="center" mb={2}>
                          {category.icon}
                          <Typography variant="h6" sx={{ ml: 1 }}>
                            {category.category}
                          </Typography>
                        </Box>
                        <Grid container spacing={1}>
                          {category.expressions.map((expr, exprIndex) => (
                            <Grid item xs={12} sm={6} md={4} key={exprIndex}>
                              <Card
                                variant="outlined"
                                sx={{
                                  cursor: 'pointer',
                                  '&:hover': {
                                    backgroundColor: 'action.hover',
                                  },
                                }}
                                onClick={() => handlePredefinedCronSelect(expr.expression)}
                              >
                                <CardContent sx={{ p: 2 }}>
                                  <Box display="flex" justifyContent="space-between" alignItems="flex-start">
                                    <Box>
                                      <Typography variant="subtitle2" gutterBottom>
                                        {expr.name}
                                      </Typography>
                                      <Typography variant="caption" color="textSecondary">
                                        {expr.description}
                                      </Typography>
                                    </Box>
                                    <IconButton
                                      size="small"
                                      onClick={(e) => {
                                        e.stopPropagation();
                                        copyToClipboard(expr.expression);
                                      }}
                                    >
                                      <CopyIcon fontSize="small" />
                                    </IconButton>
                                  </Box>
                                  <Box sx={{ mt: 1 }}>
                                    <Chip
                                      label={expr.expression}
                                      size="small"
                                      variant="outlined"
                                      sx={{ fontFamily: 'monospace' }}
                                    />
                                  </Box>
                                </CardContent>
                              </Card>
                            </Grid>
                          ))}
                        </Grid>
                      </CardContent>
                    </Card>
                  ))}
                </Box>
              )}
            </Grid>
            
            <Grid item xs={12} sm={6}>
              <Controller
                name="priority"
                control={control}
                render={({ field }) => (
                  <TextField
                    {...field}
                    label="Priority"
                    type="number"
                    fullWidth
                    error={!!errors.priority}
                    helperText={errors.priority?.message || '0-10, higher number = higher priority'}
                    inputProps={{ min: 0, max: 10 }}
                  />
                )}
              />
            </Grid>
            
            <Grid item xs={12}>
              <Controller
                name="description"
                control={control}
                render={({ field }) => (
                  <TextField
                    {...field}
                    label="Description"
                    fullWidth
                    multiline
                    rows={3}
                    error={!!errors.description}
                    helperText={errors.description?.message}
                  />
                )}
              />
            </Grid>
            
            <Grid item xs={12} sm={6}>
              <Controller
                name="enabled"
                control={control}
                render={({ field }) => (
                  <FormControlLabel
                    control={
                      <Switch
                        checked={field.value}
                        onChange={field.onChange}
                      />
                    }
                    label="Enabled"
                  />
                )}
              />
            </Grid>
            
            <Grid item xs={12} sm={6}>
              <Controller
                name="misfireInstruction"
                control={control}
                render={({ field }) => (
                  <FormControl fullWidth error={!!errors.misfireInstruction}>
                    <InputLabel>Misfire Instruction</InputLabel>
                    <Select
                      {...field}
                      label="Misfire Instruction"
                    >
                      {misfireInstructions.map((instruction) => (
                        <MenuItem key={instruction.value} value={instruction.value}>
                          {instruction.label}
                        </MenuItem>
                      ))}
                    </Select>
                    {errors.misfireInstruction && (
                      <Typography variant="caption" color="error">
                        {errors.misfireInstruction.message}
                      </Typography>
                    )}
                  </FormControl>
                )}
              />
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={onClose} disabled={isLoading}>
            Cancel
          </Button>
          <Button type="submit" variant="contained" disabled={isLoading}>
            {isLoading ? 'Saving...' : isEdit ? 'Update' : 'Create'}
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  );
};

export default TriggerForm;
