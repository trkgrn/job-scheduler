package com.trkgrn.jobscheduler.modules.job.repository;

import com.trkgrn.jobscheduler.modules.job.model.JobExecutionModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface JobExecutionRepository extends JpaRepository<JobExecutionModel, Long> {
    
    List<JobExecutionModel> findByJobDefinitionId(Long jobDefinitionId);
    
    List<JobExecutionModel> findByStatus(JobExecutionModel.Status status);
    
    List<JobExecutionModel> findByIsActiveTrue();
    
    @Query("SELECT COUNT(je) FROM JobExecutionModel je WHERE je.jobDefinition.id = :jobId AND je.status = :status")
    Long countByJobDefinitionIdAndStatus(@Param("jobId") Long jobId, @Param("status") JobExecutionModel.Status status);
    
    @Query("SELECT COUNT(je) FROM JobExecutionModel je WHERE je.status = :status")
    Long countByStatus(@Param("status") JobExecutionModel.Status status);
    
    @Query("SELECT je.jobDefinition.id, COUNT(je) FROM JobExecutionModel je WHERE je.jobDefinition.id IS NOT NULL GROUP BY je.jobDefinition.id ORDER BY COUNT(je) DESC")
    List<Object[]> countExecutionsByJobId();
    
    @Query("SELECT je FROM JobExecutionModel je WHERE je.startedAt >= :startDate ORDER BY je.startedAt ASC")
    List<JobExecutionModel> findByStartedAtAfter(@Param("startDate") java.time.OffsetDateTime startDate);
    
    // Paginated queries
    Page<JobExecutionModel> findByJobDefinitionIdOrderByStartedAtDesc(Long jobDefinitionId, Pageable pageable);
    
    Page<JobExecutionModel> findByStatusOrderByStartedAtDesc(JobExecutionModel.Status status, Pageable pageable);
    
    Page<JobExecutionModel> findAllByOrderByStartedAtDesc(Pageable pageable);
    
    @Query("SELECT je FROM JobExecutionModel je WHERE je.jobDefinition.id = :jobId ORDER BY je.startedAt DESC")
    Page<JobExecutionModel> findByJobDefinitionIdWithPagination(@Param("jobId") Long jobId, Pageable pageable);
    
    @Query("SELECT je FROM JobExecutionModel je WHERE je.jobDefinition.id = :jobId AND je.status = :status ORDER BY je.startedAt DESC")
    Page<JobExecutionModel> findByJobDefinitionIdAndStatusWithPagination(@Param("jobId") Long jobId, @Param("status") JobExecutionModel.Status status, Pageable pageable);
    
    @Query("SELECT je FROM JobExecutionModel je WHERE je.jobDefinition.id = :jobId AND je.status = 'RUNNING'")
    List<JobExecutionModel> findRunningByJobDefinitionId(@Param("jobId") Long jobId);
}

