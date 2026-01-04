package com.trkgrn.jobscheduler.modules.job.service;

import com.trkgrn.jobscheduler.modules.job.model.CronJobModel;

import java.util.List;
import java.util.Optional;

public interface CronJobService {
    List<CronJobModel> findAll();
    Optional<CronJobModel> findById(Long id);
    Optional<CronJobModel> findByCode(String code);
    CronJobModel save(CronJobModel cronJobModel);
    void deleteById(Long id);
    boolean existsById(Long id);
    CronJobModel runNow(Long id);
    List<String> getAvailableJobs();
}
