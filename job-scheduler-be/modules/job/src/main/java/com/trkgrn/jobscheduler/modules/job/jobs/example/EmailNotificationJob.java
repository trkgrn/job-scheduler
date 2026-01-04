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

import java.util.Map;

/**
 * Email notification job that sends bulk emails
 * Demonstrates complex parameter handling with JSON storage
 */
@JobComponent(
    displayName = "Email Notification Job",
    description = "Sends bulk email notifications to customers based on various criteria",
    category = "NOTIFICATION",
    parameters = {
        @JobParameter(
            name = "recipientType",
            type = ParameterType.ENUM,
            displayName = "Recipient Type",
            description = "Type of recipients to send emails to",
            required = true,
            options = "ALL_CUSTOMERS,ACTIVE_CUSTOMERS,PREMIUM_CUSTOMERS,SPECIFIC_GROUP"
        ),
        @JobParameter(
            name = "emailTemplate",
            type = ParameterType.STRING,
            displayName = "Email Template",
            description = "Template name for the email",
            required = true,
            validation = "min:1,max:100"
        ),
        @JobParameter(
            name = "subject",
            type = ParameterType.STRING,
            displayName = "Email Subject",
            description = "Subject line for the email",
            required = true,
            validation = "min:1,max:200"
        ),
        @JobParameter(
            name = "batchSize",
            type = ParameterType.INTEGER,
            displayName = "Batch Size",
            description = "Number of emails to send per batch",
            required = false,
            defaultValue = "100",
            validation = "min:1,max:1000"
        ),
        @JobParameter(
            name = "delayBetweenBatches",
            type = ParameterType.INTEGER,
            displayName = "Delay Between Batches (seconds)",
            description = "Delay between sending batches",
            required = false,
            defaultValue = "5",
            validation = "min:0,max:300"
        ),
        @JobParameter(
            name = "includeUnsubscribed",
            type = ParameterType.BOOLEAN,
            displayName = "Include Unsubscribed",
            description = "Whether to include unsubscribed users",
            required = false,
            defaultValue = "false"
        ),
        @JobParameter(
            name = "priority",
            type = ParameterType.ENUM,
            displayName = "Email Priority",
            description = "Priority level for the email",
            required = false,
            defaultValue = "NORMAL",
            options = "LOW,NORMAL,HIGH,URGENT"
        ),
        @JobParameter(
            name = "scheduleTime",
            type = ParameterType.DATE,
            displayName = "Schedule Time",
            description = "When to send the emails (optional)",
            required = false
        ),
        @JobParameter(
            name = "customVariables",
            type = ParameterType.JSON,
            displayName = "Custom Variables",
            description = "Additional variables for email personalization",
            required = false
        )
    }
)
@Component("emailNotificationJob")
public class EmailNotificationJob extends AbstractJob<CronJobModel> {

    private static final Logger LOG = LoggerFactory.getLogger(EmailNotificationJob.class);

    @NotNull
    @Override
    public String getJobName() {
        return "Email Notification Job";
    }

    @Override
    public String getDescription() {
        return "Sends bulk email notifications to customers";
    }

    @Override
    public boolean validateCronJobModel(CronJobModel cronJobModel) {
        Map<String, Object> params = cronJobModel.getParameters();
        if (params == null) {
            LOG.error("No parameters provided for EmailNotificationJob");
            return false;
        }

        // Validate required parameters
        String recipientType = (String) params.get("recipientType");
        String emailTemplate = (String) params.get("emailTemplate");
        String subject = (String) params.get("subject");

        if (recipientType == null || recipientType.trim().isEmpty()) {
            LOG.error("Recipient type is required");
            return false;
        }

        if (emailTemplate == null || emailTemplate.trim().isEmpty()) {
            LOG.error("Email template is required");
            return false;
        }

        if (subject == null || subject.trim().isEmpty()) {
            LOG.error("Email subject is required");
            return false;
        }

        // Validate batch size
        Integer batchSize = (Integer) params.getOrDefault("batchSize", 100);
        if (batchSize <= 0 || batchSize > 1000) {
            LOG.error("Invalid batch size: {}", batchSize);
            return false;
        }

        return true;
    }

    @NotNull
    @Override
    public JobResult execute(@NotNull CronJobModel cronJobModel) {
        LOG.info("Starting EmailNotificationJob execution for CronJob: {}", cronJobModel.getCode());
        
        // Extract parameters from parameters JSON
        Map<String, Object> params = cronJobModel.getParameters();
        String recipientType = (String) params.get("recipientType");
        String emailTemplate = (String) params.get("emailTemplate");
        String subject = (String) params.get("subject");
        Integer batchSize = (Integer) params.getOrDefault("batchSize", 100);
        Integer delayBetweenBatches = (Integer) params.getOrDefault("delayBetweenBatches", 5);
        Boolean includeUnsubscribed = (Boolean) params.getOrDefault("includeUnsubscribed", false);
        String priority = (String) params.getOrDefault("priority", "NORMAL");
        String scheduleTime = (String) params.get("scheduleTime");
        String customVariables = (String) params.get("customVariables");
        
        LOG.info("Email job parameters:");
        LOG.info("  - Recipient Type: {}", recipientType);
        LOG.info("  - Email Template: {}", emailTemplate);
        LOG.info("  - Subject: {}", subject);
        LOG.info("  - Batch Size: {}", batchSize);
        LOG.info("  - Delay Between Batches: {}s", delayBetweenBatches);
        LOG.info("  - Include Unsubscribed: {}", includeUnsubscribed);
        LOG.info("  - Priority: {}", priority);
        LOG.info("  - Schedule Time: {}", scheduleTime);
        LOG.info("  - Custom Variables: {}", customVariables);

        try {
            // Simulate email sending process
            int totalRecipients = simulateGetRecipientCount(recipientType, includeUnsubscribed);
            int totalBatches = (int) Math.ceil((double) totalRecipients / batchSize);
            
            LOG.info("Found {} recipients, will send in {} batches", totalRecipients, totalBatches);
            
            int successCount = 0;
            int failureCount = 0;
            
            for (int batch = 0; batch < totalBatches; batch++) {
                int startIndex = batch * batchSize;
                int endIndex = Math.min(startIndex + batchSize, totalRecipients);
                int batchSizeActual = endIndex - startIndex;
                
                LOG.info("Processing batch {}/{} (recipients {}-{})", 
                        batch + 1, totalBatches, startIndex + 1, endIndex);
                
                // Simulate batch processing
                Thread.sleep(1000);
                
                // Simulate some failures (5% failure rate)
                int batchSuccessCount = (int) (batchSizeActual * 0.95);
                int batchFailureCount = batchSizeActual - batchSuccessCount;
                
                successCount += batchSuccessCount;
                failureCount += batchFailureCount;
                
                LOG.info("Batch {}/{} completed: {} success, {} failures", 
                        batch + 1, totalBatches, batchSuccessCount, batchFailureCount);
                
                // Delay between batches (except for last batch)
                if (batch < totalBatches - 1 && delayBetweenBatches > 0) {
                    LOG.info("Waiting {} seconds before next batch...", delayBetweenBatches);
                    Thread.sleep(delayBetweenBatches * 1000);
                }
            }
            
            String message = String.format("Email notification completed: %d success, %d failures", 
                    successCount, failureCount);
            
            LOG.info("EmailNotificationJob completed successfully: {}", message);
            
            return new JobResult(true, message, Map.of(
                "totalRecipients", totalRecipients,
                "successCount", successCount,
                "failureCount", failureCount,
                "totalBatches", totalBatches,
                "recipientType", recipientType,
                "emailTemplate", emailTemplate,
                "priority", priority,
                "batchSize", batchSize,
                "delayBetweenBatches", delayBetweenBatches
            ));

        } catch (Exception e) {
            String errorMessage = "Email notification failed: " + e.getMessage();
            LOG.error("EmailNotificationJob failed: {}", errorMessage, e);
            return new JobResult(false, errorMessage, null, e);
        }
    }
    
    private int simulateGetRecipientCount(String recipientType, Boolean includeUnsubscribed) {
        // Simulate getting recipient count based on type
        switch (recipientType) {
            case "ALL_CUSTOMERS":
                return includeUnsubscribed ? 10000 : 8500;
            case "ACTIVE_CUSTOMERS":
                return includeUnsubscribed ? 5000 : 4200;
            case "PREMIUM_CUSTOMERS":
                return includeUnsubscribed ? 1000 : 950;
            case "SPECIFIC_GROUP":
                return includeUnsubscribed ? 500 : 450;
            default:
                return 100;
        }
    }
}
