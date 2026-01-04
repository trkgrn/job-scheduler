package com.trkgrn.jobscheduler.modules.job.service;

import org.quartz.SchedulerException;

public interface TriggerSyncService {
    void syncTriggersOnStartup();
    void syncTrigger(com.trkgrn.jobscheduler.modules.job.model.TriggerModel trigger) throws SchedulerException;
    void syncAllTriggers();
}
