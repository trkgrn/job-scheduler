import React, { useState } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Box,
  Typography,
  Tabs,
  Tab,
  Grid,
  Card,
  CardContent,
  CardActionArea,
  Chip,
  IconButton,
} from '@mui/material';
import {
  Close as CloseIcon,
  Schedule as ScheduleIcon,
  AccessTime as AccessTimeIcon,
  CalendarToday as CalendarTodayIcon,
  DateRange as DateRangeIcon,
  Event as EventIcon
} from '@mui/icons-material';

interface QuickSelectModalProps {
  open: boolean;
  onClose: () => void;
  onSelect: (expression: string) => void;
}

interface CronCategory {
  id: string;
  label: string;
  icon: React.ReactNode;
  expressions: {
    expression: string;
    description: string;
    label: string;
  }[];
}

const cronCategories: CronCategory[] = [
  {
    id: 'minutes',
    label: 'Minutes',
    icon: <ScheduleIcon />,
    expressions: [
      { expression: '0 */1 * * * ?', description: 'Every 1 minute', label: 'Every 1 min' },
      { expression: '0 */2 * * * ?', description: 'Every 2 minutes', label: 'Every 2 min' },
      { expression: '0 */3 * * * ?', description: 'Every 3 minutes', label: 'Every 3 min' },
      { expression: '0 */5 * * * ?', description: 'Every 5 minutes', label: 'Every 5 min' },
      { expression: '0 */10 * * * ?', description: 'Every 10 minutes', label: 'Every 10 min' },
      { expression: '0 */15 * * * ?', description: 'Every 15 minutes', label: 'Every 15 min' },
      { expression: '0 */20 * * * ?', description: 'Every 20 minutes', label: 'Every 20 min' },
      { expression: '0 */30 * * * ?', description: 'Every 30 minutes', label: 'Every 30 min' },
      { expression: '0 */45 * * * ?', description: 'Every 45 minutes', label: 'Every 45 min' }
    ]
  },
  {
    id: 'hours',
    label: 'Hours',
    icon: <AccessTimeIcon />,
    expressions: [
      { expression: '0 0 * * * ?', description: 'Every hour', label: 'Every hour' },
      { expression: '0 0 */2 * * ?', description: 'Every 2 hours', label: 'Every 2 hours' },
      { expression: '0 0 */3 * * ?', description: 'Every 3 hours', label: 'Every 3 hours' },
      { expression: '0 0 */4 * * ?', description: 'Every 4 hours', label: 'Every 4 hours' },
      { expression: '0 0 */6 * * ?', description: 'Every 6 hours', label: 'Every 6 hours' },
      { expression: '0 0 */8 * * ?', description: 'Every 8 hours', label: 'Every 8 hours' },
      { expression: '0 0 */12 * * ?', description: 'Every 12 hours', label: 'Every 12 hours' },
      { expression: '0 0 */18 * * ?', description: 'Every 18 hours', label: 'Every 18 hours' },
      { expression: '0 0 */24 * * ?', description: 'Every 24 hours', label: 'Every 24 hours' }
    ]
  },
  {
    id: 'daily',
    label: 'Daily',
    icon: <CalendarTodayIcon />,
    expressions: [
      { expression: '0 0 0 * * ?', description: 'Every day at midnight', label: 'Midnight' },
      { expression: '0 0 1 * * ?', description: 'Every day at 1:00 AM', label: '1:00 AM' },
      { expression: '0 0 3 * * ?', description: 'Every day at 3:00 AM', label: '3:00 AM' },
      { expression: '0 0 6 * * ?', description: 'Every day at 6:00 AM', label: '6:00 AM' },
      { expression: '0 0 9 * * ?', description: 'Every day at 9:00 AM', label: '9:00 AM' },
      { expression: '0 0 12 * * ?', description: 'Every day at 12:00 PM', label: '12:00 PM' },
      { expression: '0 0 15 * * ?', description: 'Every day at 3:00 PM', label: '3:00 PM' },
      { expression: '0 0 18 * * ?', description: 'Every day at 6:00 PM', label: '6:00 PM' },
      { expression: '0 0 21 * * ?', description: 'Every day at 9:00 PM', label: '9:00 PM' },
      { expression: '0 0 23 * * ?', description: 'Every day at 11:00 PM', label: '11:00 PM' }
    ]
  },
  {
    id: 'weekly',
    label: 'Weekly',
    icon: <DateRangeIcon />,
    expressions: [
      { expression: '0 0 0 ? * MON', description: 'Every Monday at midnight', label: 'Monday' },
      { expression: '0 0 0 ? * TUE', description: 'Every Tuesday at midnight', label: 'Tuesday' },
      { expression: '0 0 0 ? * WED', description: 'Every Wednesday at midnight', label: 'Wednesday' },
      { expression: '0 0 0 ? * THU', description: 'Every Thursday at midnight', label: 'Thursday' },
      { expression: '0 0 0 ? * FRI', description: 'Every Friday at midnight', label: 'Friday' },
      { expression: '0 0 0 ? * SAT', description: 'Every Saturday at midnight', label: 'Saturday' },
      { expression: '0 0 0 ? * SUN', description: 'Every Sunday at midnight', label: 'Sunday' },
      { expression: '0 0 0 ? * MON-FRI', description: 'Every weekday at midnight', label: 'Weekdays' },
      { expression: '0 0 0 ? * SAT,SUN', description: 'Every weekend at midnight', label: 'Weekends' },
      { expression: '0 0 0 ? * MON,WED,FRI', description: 'Mon, Wed, Fri at midnight', label: 'Mon-Wed-Fri' },
      { expression: '0 0 0 ? * TUE,THU', description: 'Tue, Thu at midnight', label: 'Tue-Thu' }
    ]
  },
  {
    id: 'monthly',
    label: 'Monthly',
    icon: <EventIcon />,
    expressions: [
      { expression: '0 0 0 1 * ?', description: 'First day of every month', label: '1st of month' },
      { expression: '0 0 0 5 * ?', description: '5th day of every month', label: '5th of month' },
      { expression: '0 0 0 10 * ?', description: '10th day of every month', label: '10th of month' },
      { expression: '0 0 0 15 * ?', description: '15th day of every month', label: '15th of month' },
      { expression: '0 0 0 20 * ?', description: '20th day of every month', label: '20th of month' },
      { expression: '0 0 0 25 * ?', description: '25th day of every month', label: '25th of month' },
      { expression: '0 0 0 L * ?', description: 'Last day of every month', label: 'Last day' },
      { expression: '0 0 0 L-1 * ?', description: 'Second to last day of month', label: '2nd to last' },
      { expression: '0 0 0 1,15 * ?', description: '1st and 15th of every month', label: '1st & 15th' },
      { expression: '0 0 0 1,15,30 * ?', description: '1st, 15th, 30th of month', label: '1st-15th-30th' }
    ]
  }
];

const QuickSelectModal: React.FC<QuickSelectModalProps> = ({
  open,
  onClose,
  onSelect
}) => {
  const [selectedTab, setSelectedTab] = useState(0);

  const handleSelect = (expression: string) => {
    onSelect(expression);
    onClose();
  };

  const handleTabChange = (event: React.SyntheticEvent, newValue: number) => {
    setSelectedTab(newValue);
  };

  const selectedCategory = cronCategories[selectedTab];

  return (
    <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
      <DialogTitle>
        <Box display="flex" justifyContent="space-between" alignItems="center">
          <Box display="flex" alignItems="center" gap={1}>
            <ScheduleIcon color="primary" />
            <Typography variant="h6">Quick Select Cron Expression</Typography>
          </Box>
          <IconButton onClick={onClose} size="small">
            <CloseIcon />
          </IconButton>
        </Box>
      </DialogTitle>
      
      <DialogContent sx={{ p: 0 }}>
        <Box sx={{ borderBottom: 1, borderColor: 'divider' }}>
          <Tabs 
            value={selectedTab} 
            onChange={handleTabChange} 
            variant="scrollable"
            scrollButtons="auto"
            sx={{ px: 2 }}
          >
            {cronCategories.map((category, index) => (
              <Tab
                key={category.id}
                icon={<>{category.icon}</>}
                label={category.label}
                iconPosition="start"
                sx={{ minHeight: 48 }}
              />
            ))}
          </Tabs>
        </Box>
        
        <Box sx={{ p: 3 }}>
          <Typography variant="body2" color="textSecondary" sx={{ mb: 3 }}>
            Select a predefined cron expression from the {selectedCategory.label.toLowerCase()} category:
          </Typography>
          
          <Grid container spacing={2}>
            {selectedCategory.expressions.map((item, index) => (
              <Grid item xs={12} sm={6} md={4} key={index}>
                <Card 
                  sx={{ 
                    height: '100%',
                    transition: 'all 0.2s ease-in-out',
                    '&:hover': {
                      transform: 'translateY(-2px)',
                      boxShadow: 3
                    }
                  }}
                >
                  <CardActionArea 
                    onClick={() => handleSelect(item.expression)}
                    sx={{ height: '100%' }}
                  >
                    <CardContent>
                      <Box display="flex" alignItems="center" gap={1} mb={1}>
                        <Chip 
                          label={item.label} 
                          size="small" 
                          color="primary" 
                          variant="outlined"
                        />
                      </Box>
                      <Typography 
                        variant="body2" 
                        fontFamily="monospace" 
                        fontWeight="medium"
                        sx={{ mb: 1 }}
                      >
                        {item.expression}
                      </Typography>
                      <Typography variant="caption" color="textSecondary">
                        {item.description}
                      </Typography>
                    </CardContent>
                  </CardActionArea>
                </Card>
              </Grid>
            ))}
          </Grid>
        </Box>
      </DialogContent>
      
      <DialogActions sx={{ p: 2 }}>
        <Button onClick={onClose} variant="outlined">
          Cancel
        </Button>
      </DialogActions>
    </Dialog>
  );
};

export default QuickSelectModal;
