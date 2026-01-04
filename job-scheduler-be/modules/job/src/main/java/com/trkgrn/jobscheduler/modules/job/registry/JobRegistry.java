package com.trkgrn.jobscheduler.modules.job.registry;

import com.trkgrn.jobscheduler.modules.job.api.AbortableJob;
import com.trkgrn.jobscheduler.modules.job.api.Job;
import com.trkgrn.jobscheduler.modules.job.api.JobExecutionContext;
import com.trkgrn.jobscheduler.modules.job.api.JobResult;
import com.trkgrn.jobscheduler.modules.job.model.CronJobModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

/**
 * Registry to find and execute job beans by name
 * Supports abortable jobs
 */
@Service
public class JobRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(JobRegistry.class);

    private final ApplicationContext applicationContext;
    private final JobExecutionContext jobExecutionContext;

    public JobRegistry(ApplicationContext applicationContext, JobExecutionContext jobExecutionContext) {
        this.applicationContext = applicationContext;
        this.jobExecutionContext = jobExecutionContext;
    }

    /**
     * Find job bean by name and execute it with CronJobModel
     * Sets up execution context for abortable jobs
     * 
     * @param cronJobModel CronJobModel instance
     * @param executionId Job execution ID (for cancellation support)
     * @return Job execution result
     */
    public JobResult executeJob(CronJobModel cronJobModel, Long executionId) {
        try {
            String beanName = cronJobModel.getJobBeanName();
            Job<CronJobModel> job = applicationContext.getBean(beanName, Job.class);

            // Validate CronJobModel if job supports it
            if (!job.validateCronJobModel(cronJobModel)) {
                return new JobResult(false, "Invalid CronJobModel for job: " + beanName);
            }

            // Setup execution context for abortable jobs
            // AbortableJob extends AbstractJob and provides cancellation support
            if (job.isAbortable() && executionId != null) {
                job.setExecutionContext(jobExecutionContext);
                
                // AbortableJob has setCurrentExecutionId() method (not in AbstractJob)
                if (job instanceof AbortableJob) {
                    ((AbortableJob<CronJobModel>) job).setCurrentExecutionId(executionId);
                    LOG.debug("Setup execution context for abortable job: {} (execution ID: {})", beanName, executionId);
                }
            }

            return job.execute(cronJobModel);
        } catch (Exception e) {
            return new JobResult(false, "Failed to execute job: " + cronJobModel.getJobBeanName(), null, e);
        }
    }

}

