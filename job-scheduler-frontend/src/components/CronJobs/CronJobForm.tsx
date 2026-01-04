import React from 'react';
import { useMutation, useQueryClient } from 'react-query';
import { cronJobApi } from '../../services/api';
import { CronJobModel } from '../../types';
import DynamicJobForm from './DynamicJobForm';

interface CronJobFormProps {
  open: boolean;
  onClose: () => void;
  cronJob?: CronJobModel | null;
  onSuccess: (updatedCronJob?: CronJobModel) => void;
}

const CronJobForm: React.FC<CronJobFormProps> = ({ open, onClose, cronJob, onSuccess }) => {
  const queryClient = useQueryClient();
  const isEdit = !!cronJob;

  const createMutation = useMutation(cronJobApi.create, {
    onSuccess: (data) => {
      queryClient.invalidateQueries('cronJobs');
      onSuccess(data.data);
    },
    onError: (error: any) => {
      console.error('Failed to create CronJob:', error);
      alert(`Failed to create CronJob: ${error.response?.data?.message || error.message}`);
    },
  });

  const updateMutation = useMutation(
    (data: Partial<CronJobModel>) => cronJobApi.update(cronJob!.id!, data),
    {
      onSuccess: (data) => {
        queryClient.invalidateQueries('cronJobs');
        onSuccess(data.data);
      },
      onError: (error: any) => {
        console.error('Failed to update CronJob:', error);
        alert(`Failed to update CronJob: ${error.response?.data?.message || error.message}`);
      },
    }
  );

  const handleSubmit = (data: Partial<CronJobModel>) => {
    // Generate code if not provided
    if (!data.code) {
      data.code = data.name?.toLowerCase().replace(/\s+/g, '-') || 'cronjob-' + Date.now();
    }

    if (isEdit) {
      updateMutation.mutate(data);
    } else {
      createMutation.mutate(data);
    }
  };

  return (
    <DynamicJobForm
      open={open}
      onClose={onClose}
      onSubmit={handleSubmit}
      initialData={cronJob || undefined}
      title={isEdit ? 'Edit CronJob' : 'Create New CronJob'}
    />
  );
};

export default CronJobForm;