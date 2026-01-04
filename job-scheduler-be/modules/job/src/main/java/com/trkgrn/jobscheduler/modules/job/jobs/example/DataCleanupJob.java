package com.trkgrn.jobscheduler.modules.job.jobs.example;

import com.trkgrn.jobscheduler.modules.job.annotation.JobComponent;
import com.trkgrn.jobscheduler.modules.job.annotation.JobParameter;
import com.trkgrn.jobscheduler.modules.job.annotation.ParameterType;
import com.trkgrn.jobscheduler.modules.job.api.AbstractJob;
import com.trkgrn.jobscheduler.modules.job.api.JobResult;
import com.trkgrn.jobscheduler.modules.job.model.CronJobModel;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Data cleanup job that removes old or unnecessary data
 * Demonstrates different parameter types and validation
 */
@JobComponent(
    displayName = "Data Cleanup Job",
    description = "Cleans up old data from various tables to maintain database performance",
    category = "MAINTENANCE",
    parameters = {
        @JobParameter(
            name = "cleanupType",
            type = ParameterType.ENUM,
            displayName = "Cleanup Type",
            description = "Type of data to clean up",
            required = true,
            options = "LOGS,ORDERS,PRODUCTS,USERS,ALL"
        ),
        @JobParameter(
            name = "retentionDays",
            type = ParameterType.INTEGER,
            displayName = "Retention Days",
            description = "Number of days to retain data",
            required = true,
            validation = "min:1,max:3650"
        ),
        @JobParameter(
            name = "dryRun",
            type = ParameterType.BOOLEAN,
            displayName = "Dry Run",
            description = "Preview what would be deleted without actually deleting",
            required = false,
            defaultValue = "true"
        ),
        @JobParameter(
            name = "batchSize",
            type = ParameterType.INTEGER,
            displayName = "Batch Size",
            description = "Number of records to process per batch",
            required = false,
            defaultValue = "1000",
            validation = "min:100,max:10000"
        ),
        @JobParameter(
            name = "backupBeforeDelete",
            type = ParameterType.BOOLEAN,
            displayName = "Backup Before Delete",
            description = "Create backup before deleting data",
            required = false,
            defaultValue = "false"
        ),
        @JobParameter(
            name = "backupLocation",
            type = ParameterType.STRING,
            displayName = "Backup Location",
            description = "Location to store backup files",
            required = false,
            defaultValue = "/backups/cleanup"
        ),
        @JobParameter(
            name = "notificationEmail",
            type = ParameterType.STRING,
            displayName = "Notification Email",
            description = "Email to send cleanup report",
            required = false
        ),
        @JobParameter(
            name = "cleanupSchedule",
            type = ParameterType.JSON,
            displayName = "Cleanup Schedule",
            description = "Detailed schedule for different data types",
            required = false
        ),
        @JobParameter(
            name = "excludeTables",
            type = ParameterType.TEXTAREA,
            displayName = "Exclude Tables",
            description = "Comma-separated list of tables to exclude from cleanup",
            required = false
        )
    }
)
@Component("dataCleanupJob")
public class DataCleanupJob extends AbstractJob<CronJobModel> {

    private static final Logger LOG = LoggerFactory.getLogger(DataCleanupJob.class);

    @NotNull
    @Override
    public String getJobName() {
        return "Data Cleanup Job";
    }

    @Override
    public String getDescription() {
        return "Cleans up old data to maintain database performance";
    }

    @Override
    public boolean validateCronJobModel(CronJobModel cronJobModel) {
        Map<String, Object> params = cronJobModel.getParameters();
        if (params == null) {
            LOG.error("No parameters provided for DataCleanupJob");
            return false;
        }

        // Validate required parameters
        String cleanupType = (String) params.get("cleanupType");
        Integer retentionDays = (Integer) params.get("retentionDays");

        if (cleanupType == null || cleanupType.trim().isEmpty()) {
            LOG.error("Cleanup type is required");
            return false;
        }

        if (retentionDays == null || retentionDays <= 0 || retentionDays > 3650) {
            LOG.error("Invalid retention days: {}", retentionDays);
            return false;
        }

        // Validate batch size
        Integer batchSize = (Integer) params.getOrDefault("batchSize", 1000);
        if (batchSize < 100 || batchSize > 10000) {
            LOG.error("Invalid batch size: {}", batchSize);
            return false;
        }

        return true;
    }

    @NotNull
    @Override
    public JobResult execute(@NotNull CronJobModel cronJobModel) {
        LOG.info("Starting DataCleanupJob execution for CronJob: {}", cronJobModel.getCode());
        
        // Extract parameters from parameters JSON
        Map<String, Object> params = cronJobModel.getParameters();
        String cleanupType = (String) params.get("cleanupType");
        Integer retentionDays = (Integer) params.get("retentionDays");
        Boolean dryRun = (Boolean) params.getOrDefault("dryRun", true);
        Integer batchSize = (Integer) params.getOrDefault("batchSize", 1000);
        Boolean backupBeforeDelete = (Boolean) params.getOrDefault("backupBeforeDelete", false);
        String backupLocation = (String) params.getOrDefault("backupLocation", "/backups/cleanup");
        String notificationEmail = (String) params.get("notificationEmail");
        String cleanupSchedule = (String) params.get("cleanupSchedule");
        String excludeTables = (String) params.get("excludeTables");
        
        LOG.info("Data cleanup parameters:");
        LOG.info("  - Cleanup Type: {}", cleanupType);
        LOG.info("  - Retention Days: {}", retentionDays);
        LOG.info("  - Dry Run: {}", dryRun);
        LOG.info("  - Batch Size: {}", batchSize);
        LOG.info("  - Backup Before Delete: {}", backupBeforeDelete);
        LOG.info("  - Backup Location: {}", backupLocation);
        LOG.info("  - Notification Email: {}", notificationEmail);
        LOG.info("  - Exclude Tables: {}", excludeTables);

        try {
            int totalRecordsFound = 0;
            int totalRecordsProcessed = 0;
            int totalRecordsDeleted = 0;
            int totalBatches = 0;
            
            // Simulate cleanup process based on type
            switch (cleanupType) {
                case "LOGS":
                    totalRecordsFound = simulateLogCleanup(retentionDays, dryRun, batchSize);
                    break;
                case "ORDERS":
                    totalRecordsFound = simulateOrderCleanup(retentionDays, dryRun, batchSize);
                    break;
                case "PRODUCTS":
                    totalRecordsFound = simulateProductCleanup(retentionDays, dryRun, batchSize);
                    break;
                case "USERS":
                    totalRecordsFound = simulateUserCleanup(retentionDays, dryRun, batchSize);
                    break;
                case "ALL":
                    totalRecordsFound = simulateAllDataCleanup(retentionDays, dryRun, batchSize);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown cleanup type: " + cleanupType);
            }
            
            totalBatches = (int) Math.ceil((double) totalRecordsFound / batchSize);
            totalRecordsProcessed = totalRecordsFound;
            
            if (!dryRun) {
                totalRecordsDeleted = (int) (totalRecordsFound * 0.95); // Simulate 95% success rate
            }
            
            String message = dryRun ? 
                String.format("Data cleanup preview completed: %d records would be deleted", totalRecordsFound) :
                String.format("Data cleanup completed: %d records processed, %d deleted", 
                    totalRecordsProcessed, totalRecordsDeleted);
            
            LOG.info("DataCleanupJob completed successfully: {}", message);
            
            // Simulate sending notification email
            if (notificationEmail != null && !notificationEmail.trim().isEmpty()) {
                LOG.info("Sending cleanup report to: {}", notificationEmail);
            }
            
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("cleanupType", cleanupType);
            resultData.put("retentionDays", retentionDays);
            resultData.put("dryRun", dryRun);
            resultData.put("totalRecordsFound", totalRecordsFound);
            resultData.put("totalRecordsProcessed", totalRecordsProcessed);
            resultData.put("totalRecordsDeleted", totalRecordsDeleted);
            resultData.put("totalBatches", totalBatches);
            resultData.put("batchSize", batchSize);
            resultData.put("backupBeforeDelete", backupBeforeDelete);
            resultData.put("backupLocation", backupLocation);
            resultData.put("notificationEmail", notificationEmail != null ? notificationEmail : "N/A");
            
            return new JobResult(true, message, resultData);

        } catch (Exception e) {
            String errorMessage = "Data cleanup failed: " + e.getMessage();
            LOG.error("DataCleanupJob failed: {}", errorMessage, e);
            return new JobResult(false, errorMessage, null, e);
        }
    }
    
    private int simulateLogCleanup(int retentionDays, boolean dryRun, int batchSize) throws InterruptedException {
        LOG.info("Cleaning up log records older than {} days", retentionDays);
        int totalRecords = 15000; // Simulate 15k log records
        return simulateBatchProcessing("logs", totalRecords, dryRun, batchSize);
    }
    
    private int simulateOrderCleanup(int retentionDays, boolean dryRun, int batchSize) throws InterruptedException {
        LOG.info("Cleaning up order records older than {} days", retentionDays);
        int totalRecords = 5000; // Simulate 5k order records
        return simulateBatchProcessing("orders", totalRecords, dryRun, batchSize);
    }
    
    private int simulateProductCleanup(int retentionDays, boolean dryRun, int batchSize) throws InterruptedException {
        LOG.info("Cleaning up product records older than {} days", retentionDays);
        int totalRecords = 2000; // Simulate 2k product records
        return simulateBatchProcessing("products", totalRecords, dryRun, batchSize);
    }
    
    private int simulateUserCleanup(int retentionDays, boolean dryRun, int batchSize) throws InterruptedException {
        LOG.info("Cleaning up user records older than {} days", retentionDays);
        int totalRecords = 1000; // Simulate 1k user records
        return simulateBatchProcessing("users", totalRecords, dryRun, batchSize);
    }
    
    private int simulateAllDataCleanup(int retentionDays, boolean dryRun, int batchSize) throws InterruptedException {
        LOG.info("Cleaning up all data older than {} days", retentionDays);
        int totalRecords = 23000; // Simulate 23k total records
        return simulateBatchProcessing("all", totalRecords, dryRun, batchSize);
    }
    
    private int simulateBatchProcessing(String dataType, int totalRecords, boolean dryRun, int batchSize) throws InterruptedException {
        int totalBatches = (int) Math.ceil((double) totalRecords / batchSize);
        
        for (int batch = 0; batch < totalBatches; batch++) {
            int startIndex = batch * batchSize;
            int endIndex = Math.min(startIndex + batchSize, totalRecords);
            int batchSizeActual = endIndex - startIndex;
            
            LOG.info("Processing {} batch {}/{} (records {}-{})", 
                    dataType, batch + 1, totalBatches, startIndex + 1, endIndex);
            
            // Simulate batch processing time
            Thread.sleep(500);
            
            if (dryRun) {
                LOG.info("DRY RUN: Would delete {} {} records", batchSizeActual, dataType);
            } else {
                LOG.info("Deleted {} {} records", batchSizeActual, dataType);
            }
        }
        
        return totalRecords;
    }
}
