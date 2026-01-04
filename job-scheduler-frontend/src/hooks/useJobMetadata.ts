import { useQuery } from 'react-query';
import { jobMetadataApi } from '../services/api';
import { JobMetadata } from '../types';

export const useJobMetadata = () => {
  return useQuery<JobMetadata[]>('jobMetadata', () => 
    jobMetadataApi.getAll().then(response => response.data)
  );
};

export const useJobMetadataByBeanName = (beanName: string) => {
  return useQuery<JobMetadata>(
    ['jobMetadata', beanName],
    () => jobMetadataApi.getByBeanName(beanName).then(response => response.data),
    {
      enabled: !!beanName
    }
  );
};

export const useValidateJobParameters = (beanName: string, parameters: Record<string, any>) => {
  return useQuery(
    ['validateJobParameters', beanName, parameters],
    () => jobMetadataApi.validateParameters(beanName, parameters).then(response => response.data),
    {
      enabled: !!beanName && Object.keys(parameters).length > 0
    }
  );
};
