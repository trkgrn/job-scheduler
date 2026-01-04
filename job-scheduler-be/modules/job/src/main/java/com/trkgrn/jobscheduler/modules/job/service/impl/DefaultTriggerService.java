package com.trkgrn.jobscheduler.modules.job.service.impl;

import com.trkgrn.jobscheduler.modules.job.model.TriggerModel;
import com.trkgrn.jobscheduler.modules.job.repository.CronJobRepository;
import com.trkgrn.jobscheduler.modules.job.repository.TriggerRepository;
import com.trkgrn.jobscheduler.modules.job.scheduler.QuartzJobScheduler;
import com.trkgrn.jobscheduler.modules.job.service.TriggerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class DefaultTriggerService implements TriggerService {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultTriggerService.class);

    private final TriggerRepository triggerRepository;
    private final CronJobRepository cronJobRepository;
    private final QuartzJobScheduler quartzJobScheduler;

    public DefaultTriggerService(TriggerRepository triggerRepository, 
                         CronJobRepository cronJobRepository,
                         QuartzJobScheduler quartzJobScheduler) {
        this.triggerRepository = triggerRepository;
        this.cronJobRepository = cronJobRepository;
        this.quartzJobScheduler = quartzJobScheduler;
    }

    @Override
    public List<TriggerModel> findAll() {
        return triggerRepository.findAll();
    }

    @Override
    public Optional<TriggerModel> findById(Long id) {
        return triggerRepository.findById(id);
    }

    @Override
    public List<TriggerModel> findByCronJobId(Long cronJobId) {
        return triggerRepository.findByCronJobId(cronJobId);
    }

    @Override
    public List<TriggerModel> findReadyToFire(OffsetDateTime now) {
        return triggerRepository.findReadyToFire(now);
    }

    @Override
    @Transactional
    public TriggerModel create(TriggerModel triggerModel) {
        // Validate cron job exists - we need to get cronJobId from DTO
        // This method should be called with cronJobId set in the entity
        if (triggerModel.getCronJob() == null || triggerModel.getCronJob().getId() == null) {
            throw new RuntimeException("CronJob is required");
        }

        if (!cronJobRepository.existsById(triggerModel.getCronJob().getId())) {
            throw new RuntimeException("CronJob not found");
        }

        // Generate name if not provided
        if (triggerModel.getName() == null || triggerModel.getName().trim().isEmpty()) {
            String generatedName = "Trigger-" + triggerModel.getCronJob().getCode() + "-" + System.currentTimeMillis();
            triggerModel.setName(generatedName);
        }

        // Save trigger
        TriggerModel savedTrigger = triggerRepository.save(triggerModel);

        // Schedule with Quartz if enabled
        if (savedTrigger.getEnabled()) {
            try {
                quartzJobScheduler.scheduleTrigger(savedTrigger);
                // Get next fire time after scheduling
                OffsetDateTime nextFireTime = quartzJobScheduler.getNextFireTime(savedTrigger);
                if (nextFireTime != null) {
                    savedTrigger.setNextFireTime(nextFireTime);
                    savedTrigger = triggerRepository.save(savedTrigger);
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to schedule trigger: " + e.getMessage());
            }
        }

        return savedTrigger;
    }

    @Override
    @Transactional
    public TriggerModel update(Long id, TriggerModel triggerModel) {
        if (!triggerRepository.existsById(id)) {
            throw new RuntimeException("Trigger not found with id: " + id);
        }

        triggerModel.setId(id);
        TriggerModel savedTrigger = triggerRepository.save(triggerModel);

        // Reschedule with Quartz
        try {
            quartzJobScheduler.unscheduleTrigger(savedTrigger);
            if (savedTrigger.getEnabled()) {
                quartzJobScheduler.scheduleTrigger(savedTrigger);
                // Get next fire time after rescheduling
                OffsetDateTime nextFireTime = quartzJobScheduler.getNextFireTime(savedTrigger);
                if (nextFireTime != null) {
                    savedTrigger.setNextFireTime(nextFireTime);
                    savedTrigger = triggerRepository.save(savedTrigger);
                }
            } else {
                // If disabled, clear next fire time
                savedTrigger.setNextFireTime(null);
                savedTrigger = triggerRepository.save(savedTrigger);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to reschedule trigger: " + e.getMessage());
        }

        return savedTrigger;
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        Optional<TriggerModel> triggerOpt = triggerRepository.findById(id);
        if (triggerOpt.isEmpty()) {
            throw new RuntimeException("Trigger not found with id: " + id);
        }

        TriggerModel trigger = triggerOpt.get();
        try {
            quartzJobScheduler.unscheduleTrigger(trigger);
        } catch (Exception e) {
            // Log error but continue with deletion
        }
        triggerRepository.deleteById(id);
    }

    @Override
    @Transactional
    public TriggerModel pause(Long id) {
        TriggerModel trigger = triggerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Trigger not found with id: " + id));

        trigger.setEnabled(false);
        trigger = triggerRepository.save(trigger);
        
        try {
            quartzJobScheduler.pauseTrigger(trigger);
        } catch (Exception e) {
            throw new RuntimeException("Failed to pause trigger: " + e.getMessage());
        }

        return trigger;
    }

    @Override
    @Transactional
    public TriggerModel resume(Long id) {
        TriggerModel trigger = triggerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Trigger not found with id: " + id));

        trigger.setEnabled(true);
        trigger = triggerRepository.save(trigger);
        
        try {
            quartzJobScheduler.resumeTrigger(trigger);
            // Get next fire time after resuming
            OffsetDateTime nextFireTime = quartzJobScheduler.getNextFireTime(trigger);
            if (nextFireTime != null) {
                trigger.setNextFireTime(nextFireTime);
                trigger = triggerRepository.save(trigger);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to resume trigger: " + e.getMessage());
        }

        return trigger;
    }

    @Override
    public OffsetDateTime getNextFireTime(Long id) {
        TriggerModel trigger = triggerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Trigger not found with id: " + id));

        try {
            return quartzJobScheduler.getNextFireTime(trigger);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get next fire time: " + e.getMessage());
        }
    }

    /**
     * Update next fire time for all enabled triggers
     */
    @Override
    @Transactional
    public void updateAllNextFireTimes() {
        List<TriggerModel> enabledTriggers = triggerRepository.findByEnabledTrueWithCronJob();
        for (TriggerModel trigger : enabledTriggers) {
            try {
                OffsetDateTime nextFireTime = quartzJobScheduler.getNextFireTime(trigger);
                if (nextFireTime != null) {
                    trigger.setNextFireTime(nextFireTime);
                    triggerRepository.save(trigger);
                }
            } catch (Exception e) {
                LOG.error("Failed to update next fire time for trigger: {}", trigger.getName(), e);
            }
        }
    }
}

