package com.trkgrn.jobscheduler.modules.job.service.impl;

import com.trkgrn.jobscheduler.modules.job.model.TriggerModel;
import com.trkgrn.jobscheduler.modules.job.repository.CronJobRepository;
import com.trkgrn.jobscheduler.modules.job.repository.TriggerRepository;
import com.trkgrn.jobscheduler.modules.job.scheduler.QuartzJobScheduler;
import com.trkgrn.jobscheduler.modules.job.service.TriggerSyncService;
import org.quartz.CronTrigger;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DefaultTriggerSyncService implements TriggerSyncService {
    
    private static final Logger LOG = LoggerFactory.getLogger(DefaultTriggerSyncService.class);
    
    private final Scheduler scheduler;
    private final CronJobRepository cronJobRepository;
    private final TriggerRepository triggerRepository;
    private final QuartzJobScheduler quartzJobScheduler;

    public DefaultTriggerSyncService(Scheduler scheduler, CronJobRepository cronJobRepository,
                              TriggerRepository triggerRepository, QuartzJobScheduler quartzJobScheduler) {
        this.scheduler = scheduler;
        this.cronJobRepository = cronJobRepository;
        this.triggerRepository = triggerRepository;
        this.quartzJobScheduler = quartzJobScheduler;
    }

    @Override
    @EventListener(ApplicationReadyEvent.class)
    public void syncTriggersOnStartup() {
        LOG.info("Starting trigger synchronization...");
        
        try {
            // Wait a bit for Quartz to be fully initialized
            Thread.sleep(2000);
            
            // Get all enabled triggers from database with eager loading
            List<TriggerModel> triggers = triggerRepository.findByEnabledTrueWithCronJob();
            LOG.info("Found {} enabled triggers to sync", triggers.size());
            
            int syncedCount = 0;
            int failedCount = 0;
            
            for (TriggerModel trigger : triggers) {
                try {
                    syncTriggerInNewTransaction(trigger);
                    syncedCount++;
                } catch (Exception e) {
                    LOG.error("Failed to sync trigger: {} - {}", trigger.getName(), e.getMessage());
                    failedCount++;
                }
            }
            
            LOG.info("Trigger synchronization completed. Synced: {}, Failed: {}", syncedCount, failedCount);
            
        } catch (Exception e) {
            LOG.error("Error during trigger synchronization", e);
        }
    }
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void syncTriggerInNewTransaction(TriggerModel trigger) throws SchedulerException {
        syncTrigger(trigger);
    }
    
    @Override
    public void syncTrigger(TriggerModel trigger) throws SchedulerException {
        if (trigger.getCronJob() == null) {
            LOG.warn("Trigger {} has no associated CronJob, skipping", trigger.getName());
            return;
        }
        
        // Use QuartzJobScheduler to ensure consistent key generation
        quartzJobScheduler.scheduleTrigger(trigger);
        
        LOG.info("Synced trigger: {} for CronJob: {}", trigger.getName(), trigger.getCronJob().getName());
    }
    
    private int getMisfireInstruction(String misfireInstruction) {
        if (misfireInstruction == null) {
            return CronTrigger.MISFIRE_INSTRUCTION_SMART_POLICY;
        }
        
        switch (misfireInstruction.toUpperCase()) {
            case "IGNORE_MISFIRE_POLICY":
                return CronTrigger.MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY;
            case "FIRE_ONCE_NOW":
                return CronTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW;
            case "DO_NOTHING":
                return CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING;
            case "SMART_POLICY":
            default:
                return CronTrigger.MISFIRE_INSTRUCTION_SMART_POLICY;
        }
    }
    
    @Override
    public void syncAllTriggers() {
        LOG.info("Manual trigger synchronization requested...");
        
        try {
            // Get all enabled triggers from database with eager loading
            List<TriggerModel> triggers = triggerRepository.findByEnabledTrueWithCronJob();
            LOG.info("Found {} enabled triggers to sync", triggers.size());
            
            int syncedCount = 0;
            int failedCount = 0;
            
            for (TriggerModel trigger : triggers) {
                try {
                    syncTriggerInNewTransaction(trigger);
                    syncedCount++;
                } catch (Exception e) {
                    LOG.error("Failed to sync trigger: {} - {}", trigger.getName(), e.getMessage());
                    failedCount++;
                }
            }
            
            LOG.info("Manual trigger synchronization completed. Synced: {}, Failed: {}", syncedCount, failedCount);
            
        } catch (Exception e) {
            LOG.error("Error during manual trigger synchronization", e);
        }
    }
}

