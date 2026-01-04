package com.trkgrn.jobscheduler.modules.job.repository;

import com.trkgrn.jobscheduler.modules.job.model.CronJobModel;
import com.trkgrn.jobscheduler.modules.job.model.CronJobStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CronJobRepository extends JpaRepository<CronJobModel, Long> {

    Optional<CronJobModel> findByCode(String code);

    List<CronJobModel> findByEnabledTrue();

    List<CronJobModel> findByJobBeanName(String jobBeanName);

    @Query("SELECT c FROM CronJobModel c WHERE c.jobBeanName = :beanName AND c.enabled = true")
    List<CronJobModel> findEnabledByJobBeanName(@Param("beanName") String beanName);

    long countByEnabledTrue();

    long countByStatus(CronJobStatus status);

    /**
     * Find CronJob by ID with pessimistic write lock to prevent concurrent execution
     * This ensures only one execution (manual or scheduled) can run at a time
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CronJobModel c WHERE c.id = :id")
    Optional<CronJobModel> findByIdWithLock(@Param("id") Long id);
}

