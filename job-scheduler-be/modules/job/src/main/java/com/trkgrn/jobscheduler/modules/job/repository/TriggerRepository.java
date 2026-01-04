package com.trkgrn.jobscheduler.modules.job.repository;

import com.trkgrn.jobscheduler.modules.job.model.TriggerModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface TriggerRepository extends JpaRepository<TriggerModel, Long> {

    List<TriggerModel> findByEnabledTrue();

    List<TriggerModel> findByCronJobId(Long cronJobId);

    @Query("SELECT t FROM TriggerModel t WHERE t.enabled = true AND t.nextFireTime <= :now")
    List<TriggerModel> findReadyToFire(@Param("now") OffsetDateTime now);

    @Query("SELECT t FROM TriggerModel t WHERE t.cronJob.id = :cronJobId AND t.enabled = true")
    List<TriggerModel> findEnabledByCronJobId(@Param("cronJobId") Long cronJobId);

    @Query("SELECT t FROM TriggerModel t LEFT JOIN FETCH t.cronJob WHERE t.enabled = true")
    List<TriggerModel> findByEnabledTrueWithCronJob();

    @Modifying
    @Query("DELETE FROM TriggerModel t WHERE t.cronJob.id = :cronJobId")
    void deleteByCronJobId(@Param("cronJobId") Long cronJobId);

    Optional<TriggerModel> findByQuartzTriggerKey(String quartzTriggerKey);
}

