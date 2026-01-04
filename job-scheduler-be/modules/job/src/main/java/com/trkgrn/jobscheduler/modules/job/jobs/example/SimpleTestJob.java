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
 * Simple test job that just logs and returns success
 * Used for testing the job execution framework
 */
@JobComponent(
    displayName = "Simple Test Job",
    description = "A simple test job that logs execution and returns success",
    category = "TEST",
    parameters = {
        @JobParameter(
            name = "testMessage",
            type = ParameterType.STRING,
            displayName = "Test Message",
            description = "Message to log during job execution",
            required = false,
            defaultValue = "Hello from SimpleTestJob!"
        ),
        @JobParameter(
            name = "sleepSeconds",
            type = ParameterType.INTEGER,
            displayName = "Sleep Duration (Seconds)",
            description = "How long to sleep during execution (for testing)",
            required = false,
            defaultValue = "2",
            validation = "min:0,max:60"
        )
    }
)
@Component("simpleTestJob")
public class SimpleTestJob extends AbstractJob<CronJobModel> {

    private static final Logger LOG = LoggerFactory.getLogger(SimpleTestJob.class);

    @NotNull
    @Override
    public String getJobName() {
        return "Simple Test Job";
    }

    @Override
    public String getDescription() {
        return "A simple test job that logs execution and returns success";
    }

    @Override
    public boolean validateCronJobModel(CronJobModel cronJobModel) {
        LOG.info("Validating CronJobModel for SimpleTestJob: {}", cronJobModel.getCode());
        return true;
    }

    @NotNull
    @Override
    public JobResult execute(@NotNull CronJobModel cronJobModel) {
        // TRACE level logs - very detailed debugging
        LOG.trace("=== SimpleTestJob TRACE: Starting execution ===");
        LOG.trace("TRACE: CronJob ID: {}", cronJobModel.getId());
        LOG.trace("TRACE: CronJob Version: {}", cronJobModel.getVersion());
        LOG.trace("TRACE: CronJob Created At: {}", cronJobModel.getCreatedAt());
        
        // DEBUG level logs - debugging information
        LOG.debug("=== SimpleTestJob DEBUG: Initialization ===");
        LOG.debug("DEBUG: CronJob Code: {}", cronJobModel.getCode());
        LOG.debug("DEBUG: CronJob Name: {}", cronJobModel.getName());
        LOG.debug("DEBUG: CronJob Description: {}", cronJobModel.getDescription());
        LOG.debug("DEBUG: CronJob Status: {}", cronJobModel.getStatus());
        LOG.debug("DEBUG: CronJob Enabled: {}", cronJobModel.getEnabled());
        LOG.debug("DEBUG: CronJob Retry Count: {}/{}", cronJobModel.getRetryCount(), cronJobModel.getMaxRetryCount());
        LOG.debug("DEBUG: CronJob Log Level: {}", cronJobModel.getLogLevel());
        LOG.debug("DEBUG: CronJob Parameters: {}", cronJobModel.getParameters());

        // Extract parameters from parameters JSON
        Map<String, Object> params = cronJobModel.getParameters();
        String testMessage = (String) params.getOrDefault("testMessage", "Hello from SimpleTestJob!");
        Integer sleepSeconds = (Integer) params.getOrDefault("sleepSeconds", 2);
        
        // INFO level logs - general information
        LOG.info("=== SimpleTestJob STARTED ===");
        LOG.info("Job parameters - TestMessage: {}, SleepSeconds: {}", testMessage, sleepSeconds);
        LOG.info("SimpleTestJob is working...");
        LOG.info("Test message: {}", testMessage);
        LOG.info("Sleep duration: {} seconds", sleepSeconds);

        try {
            // WARN level logs - potential issues
            if (sleepSeconds > 10) {
                LOG.warn("WARNING: Sleep duration is quite long ({} seconds), this might impact performance", sleepSeconds);
            }
            
            if (testMessage.length() > 100) {
                LOG.warn("WARNING: Test message is very long ({} characters)", testMessage.length());
            }
            
            // Simulate some work with detailed logging
            for (int i = 1; i <= sleepSeconds; i++) {
                if (i % 2 == 0) {
                    LOG.debug("DEBUG: Working... {}/{} seconds (even number)", i, sleepSeconds);
                } else {
                    LOG.trace("TRACE: Working... {}/{} seconds (odd number)", i, sleepSeconds);
                }
                
                if (i == sleepSeconds / 2) {
                    LOG.info("INFO: Halfway through execution ({}/{} seconds)", i, sleepSeconds);
                }
                
                Thread.sleep(1000);
            }
            
            // ERROR level logs for demonstration (but not actual errors)
            LOG.error("ERROR: This is a test error log - not a real error!");
            LOG.error("ERROR: Simulating error condition for testing purposes");
            
            LOG.info("SimpleTestJob work completed!");
            LOG.info("Final statistics - Message: {}, Duration: {}s", testMessage, sleepSeconds);

            String message = String.format("SimpleTestJob completed successfully for CronJob: %s", 
                    cronJobModel.getCode());
            
            LOG.info("=== SimpleTestJob COMPLETED ===");
            
            return new JobResult(true, message, Map.of(
                "cronJobCode", cronJobModel.getCode(),
                "testMessage", testMessage,
                "sleepSeconds", sleepSeconds,
                "executionTime", sleepSeconds * 1000 + "ms",
                "status", "SUCCESS"
            ));

        } catch (Exception e) {
            String errorMessage = "SimpleTestJob failed: " + e.getMessage();
            LOG.error("=== SimpleTestJob FAILED ===");
            LOG.error("Error: {}", errorMessage, e);
            LOG.error("Exception details - Class: {}, Message: {}", e.getClass().getSimpleName(), e.getMessage());
            return new JobResult(false, errorMessage, null, e);
        }
    }
}

