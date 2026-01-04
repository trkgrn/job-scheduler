import React, { useState, useEffect, useCallback } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  Button,
  TextField,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  FormControlLabel,
  Switch,
  Grid,
  Typography,
  Box,
  Alert,
  Chip,
  FormHelperText,
  Stepper,
  Step,
  StepLabel,
} from '@mui/material';
import { useForm, Controller } from 'react-hook-form';
import { useQuery } from 'react-query';
import { JobMetadata, JobParameter, ParameterType, CronJobModel, LogLevel } from '../../types';
import { jobMetadataApi } from '../../services/api';

interface DynamicJobFormProps {
  open: boolean;
  onClose: () => void;
  onSubmit: (data: Partial<CronJobModel>) => void;
  initialData?: Partial<CronJobModel>;
  title: string;
}


const DynamicJobForm: React.FC<DynamicJobFormProps> = ({
  open,
  onClose,
  onSubmit,
  initialData,
  title
}) => {
  const [jobMetadata, setJobMetadata] = useState<JobMetadata[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [activeStep, setActiveStep] = useState(0);
  
  const isEditMode = !!initialData && !!initialData.id;

  const formMethods = useForm<any>({
    mode: 'onChange'
  });

  const {
    control,
    handleSubmit,
    watch,
    reset,
    setValue,
    formState: { errors, isValid }
  } = formMethods;

  const selectedJobBeanName = watch('jobBeanName');

  const loadJobMetadata = useCallback(async () => {
    try {
      setLoading(true);
      const response = await jobMetadataApi.getAll();
      setJobMetadata(response.data);
    } catch (err) {
      setError('Error loading job metadata');
      console.error('Error loading job metadata:', err);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (open) {
      loadJobMetadata();
      if (initialData) {
        const formData = {
          ...initialData,
          parameters: initialData.parameters || {}
        };
        reset(formData);
        setActiveStep(1);
      } else {
        reset({
          enabled: true,
          retryCount: 0,
          maxRetries: 3,
          logLevel: 'INFO',
          parameters: {}
        });
        setActiveStep(0);
      }
    }
  }, [open, initialData, reset, loadJobMetadata]);

  const isFormValidForStep = () => {
    if (activeStep === 0) {
      return !!selectedJobBeanName;
    }
    
    if (activeStep === 1) {
      const name = watch('name');
      const jobBeanName = watch('jobBeanName');
      
      if (!name || !jobBeanName) {
        return false;
      }
      
      if (selectedJobData && selectedJobData.parameters) {
        for (const param of selectedJobData.parameters) {
          if (param.required) {
            const paramValue = watch(`parameters.${param.name}`);
            if (paramValue === null || paramValue === undefined || paramValue === '') {
              return false;
            }
            if (param.type === ParameterType.BOOLEAN && paramValue === false) {
              continue;
            }
            if (param.type === ParameterType.INTEGER && paramValue === 0) {
              continue;
            }
          }
        }
      }
      
      return true;
    }
    
    return true;
  };

  const { data: selectedJobData } = useQuery(
    ['selectedJobMetadata', selectedJobBeanName],
    async () => {
      if (!selectedJobBeanName) return null;
      const response = await jobMetadataApi.getByBeanName(selectedJobBeanName);
      return response.data;
    },
    { enabled: !!selectedJobBeanName }
  );

  useEffect(() => {
    if (selectedJobData) {
      const currentParameters = watch('parameters') || {};
      const defaultParams: Record<string, any> = { ...currentParameters };
      selectedJobData.parameters.forEach(param => {
        if (currentParameters[param.name] === undefined || 
            currentParameters[param.name] === null || 
            currentParameters[param.name] === '') {
          if (param.defaultValue) {
            switch (param.type) {
              case ParameterType.INTEGER:
                defaultParams[param.name] = parseInt(param.defaultValue) || 0;
                break;
              case ParameterType.BOOLEAN:
                defaultParams[param.name] = param.defaultValue === 'true';
                break;
              default:
                defaultParams[param.name] = param.defaultValue;
            }
          } else {
            switch (param.type) {
              case ParameterType.INTEGER:
                defaultParams[param.name] = 0;
                break;
              case ParameterType.BOOLEAN:
                defaultParams[param.name] = false;
                break;
              default:
                defaultParams[param.name] = '';
            }
          }
        }
      });
      
      setValue('parameters', defaultParams);
    }
  }, [selectedJobData, watch, setValue]);

  const renderParameterField = (param: JobParameter) => {
    const fieldName = `parameters.${param.name}`;
    const parameterErrors = errors.parameters as any;
    const hasError = parameterErrors && parameterErrors[param.name];
    const errorMessage = hasError ? parameterErrors[param.name]?.message : '';

    switch (param.type) {
      case ParameterType.STRING:
        return (
          <Controller
            key={param.name}
            name={fieldName}
            control={control}
            render={({ field }) => (
              <TextField
                {...field}
                fullWidth
                label={param.displayName}
                helperText={errorMessage || param.description}
                error={!!hasError}
                placeholder={param.defaultValue}
                multiline={param.name.includes('message') || param.name.includes('description')}
                rows={param.name.includes('message') || param.name.includes('description') ? 3 : 1}
              />
            )}
          />
        );

      case ParameterType.INTEGER:
        return (
          <Controller
            key={param.name}
            name={fieldName}
            control={control}
            render={({ field }) => (
              <TextField
                {...field}
                fullWidth
                type="number"
                label={param.displayName}
                helperText={errorMessage || param.description}
                error={!!hasError}
                placeholder={param.defaultValue}
                onChange={(e) => field.onChange(parseInt(e.target.value) || 0)}
              />
            )}
          />
        );

      case ParameterType.BOOLEAN:
        return (
          <Controller
            key={param.name}
            name={fieldName}
            control={control}
            render={({ field }) => (
              <FormControlLabel
                control={
                  <Switch
                    checked={field.value || false}
                    onChange={field.onChange}
                  />
                }
                label={
                  <Box>
                    <Typography variant="body2">{param.displayName}</Typography>
                    <Typography variant="caption" color={hasError ? 'error' : 'text.secondary'}>
                      {errorMessage || param.description}
                    </Typography>
                  </Box>
                }
              />
            )}
          />
        );

      case ParameterType.ENUM:
        const options = param.options ? param.options.split(',') : [];
        return (
          <Controller
            key={param.name}
            name={fieldName}
            control={control}
            render={({ field }) => (
              <FormControl fullWidth error={!!hasError}>
                <InputLabel>{param.displayName}</InputLabel>
                <Select
                  {...field}
                  label={param.displayName}
                >
                  {options.map((option) => (
                    <MenuItem key={option} value={option.trim()}>
                      {option.trim()}
                    </MenuItem>
                  ))}
                </Select>
                <FormHelperText>{errorMessage || param.description}</FormHelperText>
              </FormControl>
            )}
          />
        );

      case ParameterType.DATE:
        return (
          <Controller
            key={param.name}
            name={fieldName}
            control={control}
            render={({ field }) => (
              <TextField
                {...field}
                fullWidth
                type="date"
                label={param.displayName}
                helperText={errorMessage || param.description}
                error={!!hasError}
                InputLabelProps={{
                  shrink: true,
                }}
              />
            )}
          />
        );

      case ParameterType.JSON:
        return (
          <Controller
            key={param.name}
            name={fieldName}
            control={control}
            render={({ field }) => (
              <TextField
                {...field}
                fullWidth
                multiline
                rows={4}
                label={param.displayName}
                helperText={errorMessage || param.description}
                error={!!hasError}
                placeholder="Enter data in JSON format"
                onChange={(e) => {
                  try {
                    const jsonValue = JSON.parse(e.target.value);
                    field.onChange(jsonValue);
                  } catch {
                    field.onChange(e.target.value);
                  }
                }}
              />
            )}
          />
        );

      case ParameterType.TEXTAREA:
        return (
          <Controller
            key={param.name}
            name={fieldName}
            control={control}
            render={({ field }) => (
              <TextField
                {...field}
                fullWidth
                multiline
                rows={4}
                label={param.displayName}
                helperText={errorMessage || param.description}
                error={!!hasError}
                placeholder={param.defaultValue}
              />
            )}
          />
        );

      default:
        return null;
    }
  };

  const handleFormSubmit = (data: any) => {
    // Ensure parameters are properly structured
    const formData = {
      ...data,
      parameters: data.parameters || {}
    };
    
    onSubmit(formData);
    onClose();
  };

  const getLogLevelDescription = (level: LogLevel): string => {
    switch (level) {
      case LogLevel.TRACE:
        return 'All logs (most detailed)';
      case LogLevel.DEBUG:
        return 'Debug and above logs';
      case LogLevel.INFO:
        return 'Info and above logs (recommended)';
      case LogLevel.WARN:
        return 'Warning and error logs';
      case LogLevel.ERROR:
        return 'Error logs only';
      case LogLevel.OFF:
        return 'No logging';
      default:
        return '';
    }
  };

  const handleClose = () => {
    reset();
    setError(null);
    setActiveStep(0);
    onClose();
  };

  const handleNext = () => {
    setActiveStep((prevActiveStep) => prevActiveStep + 1);
  };

  const handleBack = () => {
    setActiveStep((prevActiveStep) => Math.max(0, prevActiveStep - 1));
  };


  const steps = [
    {
      label: 'Basic Information',
      description: 'Enter basic job information'
    },
    {
      label: 'Job Parameters',
      description: 'Configure job parameters'
    }
  ];

  return (
    <Dialog 
      open={open} 
      onClose={handleClose} 
      maxWidth="md" 
      fullWidth
      fullScreen={window.innerWidth < 600 ? true : false}
      sx={{
        '& .MuiDialog-paper': {
          margin: { xs: 0, sm: 32 },
          maxHeight: { xs: '100vh', sm: '90vh' }
        }
      }}
    >
        <DialogTitle sx={{ fontSize: { xs: '1.1rem', sm: '1.25rem' } }}>
          {title}
        </DialogTitle>
        <form onSubmit={handleSubmit(handleFormSubmit)}>
          <DialogContent sx={{ px: { xs: 2, sm: 3 } }}>
            {error && (
              <Alert severity="error" sx={{ mb: 2 }}>
                {error}
              </Alert>
            )}

            <Stepper 
              activeStep={activeStep} 
              orientation="horizontal" 
              sx={{ 
                mb: 3,
                '& .MuiStepLabel-label': {
                  fontSize: { xs: '0.75rem', sm: '0.875rem' }
                }
              }}
            >
              {steps.map((step, index) => (
                <Step key={step.label}>
                  <StepLabel
                    optional={
                      index === 1 ? (
                        <Typography variant="caption">{step.description}</Typography>
                      ) : null
                    }
                  >
                    {step.label}
                  </StepLabel>
                </Step>
              ))}
            </Stepper>

            {/* Step Content */}
            <Box sx={{ mb: 2 }}>
              {activeStep === 0 && (
                <Grid container spacing={2}>
                  {/* Basic Fields */}
                  <Grid item xs={12} sm={6}>
                    <Controller
                      name="name"
                      control={control}
                      render={({ field }) => (
                        <TextField
                          {...field}
                          fullWidth
                          label="Job Name"
                          error={!!errors.name}
                          helperText={errors.name?.message as string}
                        />
                      )}
                    />
                  </Grid>

                  <Grid item xs={12} sm={6}>
                    <Controller
                      name="jobBeanName"
                      control={control}
                      render={({ field }) => (
                        <FormControl fullWidth error={!!errors.jobBeanName}>
                          <InputLabel>Job Type</InputLabel>
                          <Select
                            {...field}
                            label="Job Type"
                            disabled={loading || isEditMode}
                          >
                            {jobMetadata.map((job) => (
                              <MenuItem key={job.beanName} value={job.beanName}>
                                <Box>
                                  <Typography variant="body2">{job.displayName}</Typography>
                                  <Typography variant="caption" color="text.secondary">
                                    {job.description}
                                  </Typography>
                                </Box>
                              </MenuItem>
                            ))}
                          </Select>
                          <FormHelperText>{errors.jobBeanName?.message as string}</FormHelperText>
                          {isEditMode && (
                            <FormHelperText>
                              Job type cannot be changed in edit mode
                            </FormHelperText>
                          )}
                        </FormControl>
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
                          fullWidth
                          label="Description"
                          multiline
                          rows={2}
                        />
                      )}
                    />
                  </Grid>

                  <Grid item xs={12} sm={6}>
                    <Controller
                      name="retryCount"
                      control={control}
                      render={({ field }) => (
                        <TextField
                          {...field}
                          fullWidth
                          type="number"
                          label="Retry Count"
                          error={!!errors.retryCount}
                          helperText={errors.retryCount?.message as string}
                        />
                      )}
                    />
                  </Grid>

                  <Grid item xs={12} sm={6}>
                    <Controller
                      name="maxRetries"
                      control={control}
                      render={({ field }) => (
                        <TextField
                          {...field}
                          fullWidth
                          type="number"
                          label="Max Retries"
                          error={!!errors.maxRetries}
                          helperText={errors.maxRetries?.message as string}
                        />
                      )}
                    />
                  </Grid>

                  <Grid item xs={12} sm={6}>
                    <Controller
                      name="logLevel"
                      control={control}
                      render={({ field }) => (
                        <FormControl fullWidth>
                          <InputLabel>Log Level</InputLabel>
                          <Select
                            {...field}
                            label="Log Level"
                          >
                            {Object.values(LogLevel).map((level) => (
                              <MenuItem key={level} value={level}>
                                <Box>
                                  <Typography variant="body2">{level}</Typography>
                                  <Typography variant="caption" color="text.secondary">
                                    {getLogLevelDescription(level)}
                                  </Typography>
                                </Box>
                              </MenuItem>
                            ))}
                          </Select>
                          <FormHelperText>
                            Only logs at this level and above will be recorded
                          </FormHelperText>
                        </FormControl>
                      )}
                    />
                  </Grid>

                  <Grid item xs={12}>
                    <Controller
                      name="enabled"
                      control={control}
                      render={({ field }) => (
                        <FormControlLabel
                          control={
                            <Switch
                              checked={field.value || false}
                              onChange={field.onChange}
                            />
                          }
                          label="Enabled"
                        />
                      )}
                    />
                  </Grid>
                </Grid>
              )}

              {activeStep === 1 && (
                <Box>
                  {selectedJobData ? (
                    <Box sx={{ mt: 2, p: 2, bgcolor: 'grey.50', borderRadius: 1 }}>
                      <Typography variant="h6" gutterBottom>
                        Job Parameters
                      </Typography>
                      <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                        {selectedJobData.description}
                      </Typography>
                      
                      <Grid container spacing={2}>
                        {selectedJobData.parameters.map((param) => (
                          <Grid item xs={12} sm={6} key={param.name}>
                            <Box sx={{ position: 'relative' }}>
                              {param.required && (
                                <Chip
                                  label="Required"
                                  size="small"
                                  color="primary"
                                  sx={{ position: 'absolute', top: -8, right: 0, zIndex: 1 }}
                                />
                              )}
                              {renderParameterField(param)}
                            </Box>
                          </Grid>
                        ))}
                      </Grid>
                    </Box>
                  ) : (
                    <Box sx={{ textAlign: 'center', py: 4 }}>
                      <Typography variant="body1" color="textSecondary">
                        Please select a job type first
                      </Typography>
                    </Box>
                  )}
                </Box>
              )}
            </Box>

            {/* Navigation Buttons */}
            <Box sx={{ display: 'flex', justifyContent: 'space-between', mt: 3 }}>
              <Button
                disabled={activeStep === 0}
                onClick={handleBack}
              >
                Back
              </Button>
              <Button
                variant="contained"
                disabled={!isFormValidForStep()}
                onClick={activeStep === steps.length - 1 ? handleSubmit(handleFormSubmit) : handleNext}
              >
                {activeStep === steps.length - 1 ? 'Finish' : 'Continue'}
              </Button>
            </Box>
          </DialogContent>
        </form>
      </Dialog>
  );
};

export default DynamicJobForm;
