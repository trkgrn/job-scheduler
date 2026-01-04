import React, { useState } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  TextField,
  Box,
  Typography,
  MenuItem,
  IconButton,
  Tooltip,
  FormControlLabel,
  Switch,
  Collapse
} from '@mui/material';
import {
  Close as CloseIcon,
  ContentCopy as CopyIcon,
  Help as HelpIcon,
  ExpandLess as ExpandLessIcon
} from '@mui/icons-material';
import { useForm, Controller } from 'react-hook-form';
import { TriggerModel } from '../../types';

interface TriggerFormProps {
  open: boolean;
  onClose: () => void;
  onSubmit: (data: any) => void;
  editingTrigger?: TriggerModel | null;
  onQuickSelect: () => void;
  selectedCronExpression?: string;
  onCronExpressionSelect?: (expression: string) => void;
}

const TriggerForm: React.FC<TriggerFormProps> = ({
  open,
  onClose,
  onSubmit,
  editingTrigger,
  onQuickSelect,
  selectedCronExpression,
  onCronExpressionSelect
}) => {
  const [showCronHelp, setShowCronHelp] = useState(false);
  
  const { control, handleSubmit, setValue, reset } = useForm({
    defaultValues: {
      name: '',
      description: '',
      cronExpression: '',
      enabled: true,
      priority: 5,
      misfireInstruction: 'MISFIRE_INSTRUCTION_SMART_POLICY'
    }
  });

  React.useEffect(() => {
    if (editingTrigger) {
      setValue('name', editingTrigger.name || '');
      setValue('description', editingTrigger.description || '');
      setValue('cronExpression', editingTrigger.cronExpression || '');
      setValue('enabled', editingTrigger.enabled ?? true);
      setValue('priority', editingTrigger.priority || 5);
      setValue('misfireInstruction', editingTrigger.misfireInstruction || 'MISFIRE_INSTRUCTION_SMART_POLICY');
    } else {
      reset();
    }
  }, [editingTrigger, setValue, reset]);

  // Update cron expression when selected from quick select
  React.useEffect(() => {
    if (selectedCronExpression && onCronExpressionSelect) {
      setValue('cronExpression', selectedCronExpression);
      onCronExpressionSelect(''); // Clear the selected expression
    }
  }, [selectedCronExpression, setValue, onCronExpressionSelect]);

  const copyToClipboard = (text: string) => {
    navigator.clipboard.writeText(text);
  };

  const handleFormSubmit = (data: any) => {
    onSubmit(data);
    reset();
  };

  return (
    <Dialog 
      open={open} 
      onClose={onClose} 
      maxWidth="sm" 
      fullWidth
      fullScreen={window.innerWidth < 600}
      sx={{
        '& .MuiDialog-paper': {
          margin: { xs: 0, sm: 32 },
          maxHeight: { xs: '100vh', sm: '90vh' }
        }
      }}
    >
      <DialogTitle>
        <Box display="flex" justifyContent="space-between" alignItems="center">
          <Typography variant="h6" sx={{ fontSize: { xs: '1.1rem', sm: '1.25rem' } }}>
            {editingTrigger ? 'Edit Trigger' : 'Create Trigger'}
          </Typography>
          <IconButton onClick={onClose} size="small">
            <CloseIcon />
          </IconButton>
        </Box>
      </DialogTitle>
      
      <DialogContent sx={{ px: { xs: 2, sm: 3 } }}>
        <Box sx={{ pt: 1 }}>
          <Controller
            name="name"
            control={control}
            rules={{ required: 'Name is required' }}
            render={({ field, fieldState: { error } }) => (
              <TextField
                {...field}
                fullWidth
                label="Name"
                error={!!error}
                helperText={error?.message}
                sx={{ mb: 2 }}
              />
            )}
          />
          
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
                sx={{ mb: 2 }}
              />
            )}
          />
          
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
            <Typography variant="subtitle2">
              Cron Expression
            </Typography>
            <Tooltip title="Show cron expression help">
              <IconButton
                size="small"
                onClick={() => setShowCronHelp(!showCronHelp)}
                sx={{ ml: 1 }}
              >
                <HelpIcon fontSize="small" />
              </IconButton>
            </Tooltip>
          </Box>
          
          <Box sx={{ display: 'flex', gap: 1, mb: 2 }}>
            <Controller
              name="cronExpression"
              control={control}
              rules={{ required: 'Cron expression is required' }}
              render={({ field, fieldState: { error } }) => (
                <TextField
                  {...field}
                  fullWidth
                  label="Cron Expression"
                  placeholder="0 0 12 * * ?"
                  error={!!error}
                  InputProps={{
                    endAdornment: (
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
            <Button
              variant="outlined"
              onClick={onQuickSelect}
              sx={{ minWidth: 'auto', px: 2 }}
            >
              Quick Select
            </Button>
          </Box>
          
          {/* Collapsible Cron Expression Help */}
          <Collapse in={showCronHelp}>
            <Box sx={{ 
              mt: 2, 
              mb: 2,
              p: 2, 
              bgcolor: 'grey.50', 
              borderRadius: 1, 
              border: '1px solid',
              borderColor: 'grey.200'
            }}>
              <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 2 }}>
                <Typography variant="subtitle2" sx={{ fontWeight: 'medium' }}>
                  Cron Expression Format
                </Typography>
                <IconButton
                  size="small"
                  onClick={() => setShowCronHelp(false)}
                >
                  <ExpandLessIcon fontSize="small" />
                </IconButton>
              </Box>
              
              <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.5 }}>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                  <Typography variant="body2" sx={{ minWidth: '80px', fontWeight: 'medium' }}>
                    Example:
                  </Typography>
                  <Box sx={{ 
                    fontFamily: 'monospace', 
                    bgcolor: 'white', 
                    px: 1.5, 
                    py: 0.5, 
                    borderRadius: 0.5,
                    border: '1px solid',
                    borderColor: 'grey.300'
                  }}>
                    <Typography variant="body2" component="span" sx={{ fontFamily: 'monospace' }}>
                      0 0 12 * * ?
                    </Typography>
                  </Box>
                  <Typography variant="body2" color="textSecondary">
                    (Every day at 12:00 PM)
                  </Typography>
                </Box>
                
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                  <Typography variant="body2" sx={{ minWidth: '80px', fontWeight: 'medium' }}>
                    Format:
                  </Typography>
                  <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap' }}>
                    {[
                      { label: 'Second', value: '0' },
                      { label: 'Minute', value: '0' },
                      { label: 'Hour', value: '12' },
                      { label: 'Day', value: '*' },
                      { label: 'Month', value: '*' },
                      { label: 'Day of Week', value: '?' }
                    ].map((item, index) => (
                      <Box key={index} sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                        <Typography variant="caption" color="textSecondary">
                          {item.label}:
                        </Typography>
                        <Box sx={{ 
                          fontFamily: 'monospace', 
                          bgcolor: 'white', 
                          px: 1, 
                          py: 0.25, 
                          borderRadius: 0.25,
                          border: '1px solid',
                          borderColor: 'grey.300',
                          fontSize: '0.75rem'
                        }}>
                          {item.value}
                        </Box>
                      </Box>
                    ))}
                  </Box>
                </Box>
                
                <Typography variant="caption" color="textSecondary">
                  Use * for any value, ? for no specific value, and numbers for specific values
                </Typography>
              </Box>
            </Box>
          </Collapse>
          
          <Controller
            name="priority"
            control={control}
            render={({ field }) => (
              <TextField
                {...field}
                fullWidth
                label="Priority"
                type="number"
                inputProps={{ min: 1, max: 10 }}
                sx={{ mb: 2 }}
              />
            )}
          />
          
          <Controller
            name="misfireInstruction"
            control={control}
            render={({ field }) => (
              <TextField
                {...field}
                fullWidth
                select
                label="Misfire Instruction"
                sx={{ mb: 2 }}
              >
                <MenuItem value="MISFIRE_INSTRUCTION_SMART_POLICY">Smart Policy</MenuItem>
                <MenuItem value="MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY">Ignore Misfire</MenuItem>
                <MenuItem value="MISFIRE_INSTRUCTION_FIRE_NOW">Fire Now</MenuItem>
                <MenuItem value="MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_EXISTING_COUNT">Reschedule Next</MenuItem>
                <MenuItem value="MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_REMAINING_COUNT">Reschedule Next with Remaining</MenuItem>
                <MenuItem value="MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_EXISTING_REPEAT_COUNT">Reschedule Now</MenuItem>
                <MenuItem value="MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_REMAINING_REPEAT_COUNT">Reschedule Now with Remaining</MenuItem>
              </TextField>
            )}
          />
          
          <Controller
            name="enabled"
            control={control}
            render={({ field }) => (
              <FormControlLabel
                control={
                  <Switch
                    checked={field.value || false}
                    onChange={field.onChange}
                    color="primary"
                  />
                }
                label={
                  <Box>
                    <Typography variant="body1">Status</Typography>
                    <Typography variant="caption" color="textSecondary">
                      {field.value ? 'Enabled' : 'Disabled'}
                    </Typography>
                  </Box>
                }
                sx={{ mb: 2 }}
              />
            )}
          />
        </Box>
      </DialogContent>
      
      <DialogActions>
        <Button onClick={onClose}>Cancel</Button>
        <Button 
          variant="contained" 
          onClick={handleSubmit(handleFormSubmit)}
        >
          {editingTrigger ? 'Update' : 'Create'}
        </Button>
      </DialogActions>
    </Dialog>
  );
};

export default TriggerForm;
