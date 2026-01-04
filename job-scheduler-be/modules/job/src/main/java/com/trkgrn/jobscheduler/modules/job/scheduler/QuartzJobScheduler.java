package com.trkgrn.jobscheduler.modules.job.scheduler;

import com.trkgrn.jobscheduler.modules.job.model.CronJobModel;
import com.trkgrn.jobscheduler.modules.job.model.TriggerModel;
import com.trkgrn.jobscheduler.modules.job.registry.JobRegistry;
import com.trkgrn.jobscheduler.modules.job.repository.CronJobRepository;
import com.trkgrn.jobscheduler.modules.job.repository.TriggerRepository;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * Quartz scheduler integration for automatic job execution
 */
@Component
public class QuartzJobScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(QuartzJobScheduler.class);

    private final Scheduler scheduler;
    private final CronJobRepository cronJobRepository;
    private final TriggerRepository triggerRepository;
    private final JobRegistry jobRegistry;

    public QuartzJobScheduler(Scheduler scheduler, CronJobRepository cronJobRepository, TriggerRepository triggerRepository, JobRegistry jobRegistry) {
        this.scheduler = scheduler;
        this.cronJobRepository = cronJobRepository;
        this.triggerRepository = triggerRepository;
        this.jobRegistry = jobRegistry;
    }

    /**
     * Schedule a trigger for automatic execution
     */
    public void scheduleTrigger(TriggerModel triggerModel) throws SchedulerException {
        CronJobModel cronJob = triggerModel.getCronJob();
        if (cronJob == null || !cronJob.getEnabled()) {
            LOG.warn("Cannot schedule trigger for disabled or null CronJob: {}", triggerModel.getName());
            return;
        }

        // Use same job key for same CronJob, but unique trigger key
        String jobKey = "cronJob-" + cronJob.getId();
        String triggerKey = "trigger-" + triggerModel.getId();

        // Check if job already exists for this CronJob
        JobKey quartzJobKey = JobKey.jobKey(jobKey);
        JobDetail jobDetail;
        
        if (scheduler.checkExists(quartzJobKey)) {
            // Job exists, get it
            jobDetail = scheduler.getJobDetail(quartzJobKey);
        } else {
            // Create new Quartz job detail
            jobDetail = JobBuilder.newJob(CronJobQuartzJob.class)
                    .withIdentity(jobKey)
                    .usingJobData("cronJobId", cronJob.getId())
                    .build();
        }

        // Create Quartz trigger
        CronScheduleBuilder cronSchedule = CronScheduleBuilder.cronSchedule(triggerModel.getCronExpression());
        
        // Apply misfire instruction to the cron schedule
        switch (triggerModel.getMisfireInstruction()) {
            case "IGNORE_MISFIRE_POLICY":
                cronSchedule.withMisfireHandlingInstructionIgnoreMisfires();
                break;
            case "FIRE_ONCE_NOW":
                cronSchedule.withMisfireHandlingInstructionFireAndProceed();
                break;
            case "DO_NOTHING":
                cronSchedule.withMisfireHandlingInstructionDoNothing();
                break;
            default:
                cronSchedule.withMisfireHandlingInstructionDoNothing();
                break;
        }
        
        CronTrigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(triggerKey, "DEFAULT")
                .withSchedule(cronSchedule)
                .startAt(triggerModel.getStartTime() != null ? 
                        Date.from(triggerModel.getStartTime().atZoneSameInstant(ZoneId.systemDefault()).toInstant()) : 
                        new Date())
                .endAt(triggerModel.getEndTime() != null ? 
                        Date.from(triggerModel.getEndTime().atZoneSameInstant(ZoneId.systemDefault()).toInstant()) : 
                        null)
                .build();

        // Schedule the job and trigger
        if (scheduler.checkExists(quartzJobKey)) {
            // Job exists, just schedule the trigger
            scheduler.scheduleJob(trigger);
        } else {
            // New job, schedule both job and trigger
            scheduler.scheduleJob(jobDetail, trigger);
        }
        
        // Update trigger with Quartz key
        triggerModel.setQuartzTriggerKey(triggerKey);
        triggerRepository.save(triggerModel);

        LOG.info("Scheduled trigger {} for CronJob {} with expression: {}", 
                triggerModel.getName(), cronJob.getCode(), triggerModel.getCronExpression());
    }

    /**
     * Unschedule a trigger
     */
    public void unscheduleTrigger(TriggerModel triggerModel) throws SchedulerException {
        // Use trigger name instead of quartzTriggerKey since that's what's stored in Quartz
        String triggerKey = "trigger-" + triggerModel.getId();
        TriggerKey quartzTriggerKey = TriggerKey.triggerKey(triggerKey, "DEFAULT");
        
        if (scheduler.checkExists(quartzTriggerKey)) {
            scheduler.unscheduleJob(quartzTriggerKey);
            LOG.info("Unscheduled trigger: {}", triggerModel.getName());
        }
            
            // Check if there are other triggers for the same CronJob
            CronJobModel cronJob = triggerModel.getCronJob();
            if (cronJob != null) {
                String jobKey = "cronJob-" + cronJob.getId();
                JobKey quartzJobKey = JobKey.jobKey(jobKey);
                
                // Get all triggers for this job
                List<? extends Trigger> triggers = scheduler.getTriggersOfJob(quartzJobKey);
                
                // If no more triggers, delete the job
                if (triggers.isEmpty()) {
                    scheduler.deleteJob(quartzJobKey);
                    LOG.info("Deleted job for CronJob: {}", cronJob.getCode());
                }
            }
            
            triggerModel.setQuartzTriggerKey(null);
            triggerRepository.save(triggerModel);
        }


    /**
     * Pause a trigger
     */
    public void pauseTrigger(TriggerModel triggerModel) throws SchedulerException {
        // Use trigger name instead of quartzTriggerKey since that's what's stored in Quartz
        String triggerKey = "trigger-" + triggerModel.getId();
        TriggerKey quartzTriggerKey = TriggerKey.triggerKey(triggerKey, "DEFAULT");
        
        if (scheduler.checkExists(quartzTriggerKey)) {
            scheduler.pauseTrigger(quartzTriggerKey);
            LOG.info("Paused trigger: {}", triggerModel.getName());
        }
    }

    /**
     * Resume a trigger
     */
    public void resumeTrigger(TriggerModel triggerModel) throws SchedulerException {
        // Use trigger name instead of quartzTriggerKey since that's what's stored in Quartz
        String triggerKey = "trigger-" + triggerModel.getId();
        TriggerKey quartzTriggerKey = TriggerKey.triggerKey(triggerKey, "DEFAULT");
        
        if (scheduler.checkExists(quartzTriggerKey)) {
            scheduler.resumeTrigger(quartzTriggerKey);
            LOG.info("Resumed trigger: {}", triggerModel.getName());
        }
    }

    /**
     * Get next fire time for a trigger
     */
    public OffsetDateTime getNextFireTime(TriggerModel triggerModel) {
        try {
            // Use trigger name instead of quartzTriggerKey since that's what's stored in Quartz
            String triggerKey = "trigger-" + triggerModel.getId();
            TriggerKey quartzTriggerKey = TriggerKey.triggerKey(triggerKey, "DEFAULT");
            
            LOG.debug("Looking for trigger with key: {} in group: DEFAULT", triggerKey);
            
            if (scheduler.checkExists(quartzTriggerKey)) {
                Date nextFireTime = scheduler.getTrigger(quartzTriggerKey).getNextFireTime();
                if (nextFireTime != null) {
                    return nextFireTime.toInstant().atZone(ZoneId.systemDefault()).toOffsetDateTime();
                }
            } else {
                LOG.warn("Trigger not found in Quartz with key: {} in group: DEFAULT", triggerKey);
                // Let's try to find all triggers to see what's actually there
                debugAllTriggers();
            }
        } catch (SchedulerException e) {
            LOG.error("Error getting next fire time for trigger: {}", triggerModel.getName(), e);
        }
        return null;
    }

    /**
     * Debug method to list all triggers in Quartz
     */
    public void debugAllTriggers() {
        try {
            List<String> jobGroups = scheduler.getJobGroupNames();
            LOG.info("Quartz Job Groups: {}", jobGroups);
            
            for (String groupName : jobGroups) {
                Set<JobKey> jobKeys = scheduler.getJobKeys(GroupMatcher.jobGroupEquals(groupName));
                for (JobKey jobKey : jobKeys) {
                    LOG.info("Job: {} in group: {}", jobKey.getName(), jobKey.getGroup());
                    
                    List<? extends Trigger> triggers = scheduler.getTriggersOfJob(jobKey);
                    for (Trigger trigger : triggers) {
                        LOG.info("  Trigger: {} in group: {}", trigger.getKey().getName(), trigger.getKey().getGroup());
                    }
                }
            }
        } catch (SchedulerException e) {
            LOG.error("Error debugging triggers", e);
        }
    }

}

