package com.trkgrn.jobscheduler.modules.job.service;

import com.trkgrn.jobscheduler.modules.job.model.TriggerModel;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface TriggerService {
    List<TriggerModel> findAll();
    Optional<TriggerModel> findById(Long id);
    List<TriggerModel> findByCronJobId(Long cronJobId);
    List<TriggerModel> findReadyToFire(OffsetDateTime now);
    TriggerModel create(TriggerModel triggerModel);
    TriggerModel update(Long id, TriggerModel triggerModel);
    void deleteById(Long id);
    TriggerModel pause(Long id);
    TriggerModel resume(Long id);
    OffsetDateTime getNextFireTime(Long id);
    void updateAllNextFireTimes();
}
