package com.trkgrn.jobscheduler.modules.job.jobs.example;

import com.trkgrn.jobscheduler.modules.job.annotation.JobComponent;
import com.trkgrn.jobscheduler.modules.job.annotation.JobParameter;
import com.trkgrn.jobscheduler.modules.job.annotation.ParameterType;
import com.trkgrn.jobscheduler.modules.job.api.AbortableJob;
import com.trkgrn.jobscheduler.modules.job.api.CronJobResult;
import com.trkgrn.jobscheduler.modules.job.api.JobResult;
import com.trkgrn.jobscheduler.modules.job.api.PerformResult;
import com.trkgrn.jobscheduler.modules.job.model.CronJobModel;
import com.trkgrn.jobscheduler.modules.job.model.CronJobStatus;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Random;

/**
 * Test job specifically designed to generate various types of logs
 * This demonstrates different log levels and patterns
 */
@JobComponent(
    displayName = "Logging Test Job",
    description = "A test job that generates various types of logs for testing purposes",
    category = "TEST",
    parameters = {
        @JobParameter(
            name = "logLevel",
            type = ParameterType.ENUM,
            displayName = "Log Level",
            description = "Primary log level to use for this execution",
            required = true,
            defaultValue = "INFO",
            options = "TRACE,DEBUG,INFO,WARN,ERROR"
        ),
        @JobParameter(
            name = "generateErrors",
            type = ParameterType.BOOLEAN,
            displayName = "Generate Test Errors",
            description = "Whether to generate test error logs",
            required = false,
            defaultValue = "false"
        ),
        @JobParameter(
            name = "generateWarnings",
            type = ParameterType.BOOLEAN,
            displayName = "Generate Warnings",
            description = "Whether to generate warning logs",
            required = false,
            defaultValue = "true"
        ),
        @JobParameter(
            name = "executionSteps",
            type = ParameterType.INTEGER,
            displayName = "Execution Steps",
            description = "Number of execution steps to perform",
            required = false,
            defaultValue = "5",
            validation = "min:1,max:20"
        ),
        @JobParameter(
            name = "randomLogs",
            type = ParameterType.BOOLEAN,
            displayName = "Random Logs",
            description = "Whether to generate random log messages",
            required = false,
            defaultValue = "true"
        )
    }
)
@Component("loggingTestJob")
public class LoggingTestJob extends AbortableJob<CronJobModel> {

    private static final Logger LOG = LoggerFactory.getLogger(LoggingTestJob.class);
    private final Random random = new Random();

    @NotNull
    @Override
    public String getJobName() {
        return "Logging Test Job";
    }

    @Override
    public String getDescription() {
        return "A test job that generates various types of logs for testing purposes";
    }

    @Override
    public boolean validateCronJobModel(CronJobModel cronJobModel) {
        LOG.info("Validating CronJobModel for LoggingTestJob: {}", cronJobModel.getCode());
        return true;
    }

    @NotNull
    @Override
    public JobResult execute(@NotNull CronJobModel cronJobModel) {
        // Extract parameters
        Map<String, Object> params = cronJobModel.getParameters();
        String logLevel = (String) params.getOrDefault("logLevel", "INFO");
        Boolean generateErrors = (Boolean) params.getOrDefault("generateErrors", false);
        Boolean generateWarnings = (Boolean) params.getOrDefault("generateWarnings", true);
        Integer executionSteps = (Integer) params.getOrDefault("executionSteps", 5);
        Boolean randomLogs = (Boolean) params.getOrDefault("randomLogs", true);

        LOG.info("=== LoggingTestJob STARTED ===");
        LOG.info("Configuration - LogLevel: {}, GenerateErrors: {}, GenerateWarnings: {}, Steps: {}, RandomLogs: {}", 
                logLevel, generateErrors, generateWarnings, executionSteps, randomLogs);

        try {
            // TRACE level logs - very detailed
            LOG.trace("=== TRACE: Detailed execution tracking ===");
            LOG.trace("TRACE: CronJob ID: {}", cronJobModel.getId());
            LOG.trace("TRACE: CronJob Code: {}", cronJobModel.getCode());
            LOG.trace("TRACE: CronJob Name: {}", cronJobModel.getName());
            LOG.trace("TRACE: CronJob Status: {}", cronJobModel.getStatus());
            LOG.trace("TRACE: CronJob Enabled: {}", cronJobModel.getEnabled());
            LOG.trace("TRACE: CronJob Log Level: {}", cronJobModel.getLogLevel());
            LOG.trace("TRACE: All parameters: {}", cronJobModel.getParameters());
            LOG.trace("TRACE: Random seed: {}", random.nextInt(1000));

            // DEBUG level logs - debugging information
            LOG.debug("=== DEBUG: Debugging information ===");
            LOG.debug("DEBUG: Job execution started at: {}", System.currentTimeMillis());
            LOG.debug("DEBUG: Available memory: {} MB", Runtime.getRuntime().freeMemory() / 1024 / 1024);
            LOG.debug("DEBUG: Total memory: {} MB", Runtime.getRuntime().totalMemory() / 1024 / 1024);
            LOG.debug("DEBUG: Max memory: {} MB", Runtime.getRuntime().maxMemory() / 1024 / 1024);
            LOG.debug("DEBUG: Processor count: {}", Runtime.getRuntime().availableProcessors());

            // INFO level logs - general information
            LOG.info("Starting logging test execution...");
            LOG.info("This job will demonstrate various log levels and patterns");

            // Execute steps with different log levels
            for (int step = 1; step <= executionSteps; step++) {
                // Check and clear abort request
                if (clearAbortRequestedIfNeeded(cronJobModel)) {
                    LOG.warn("Job aborted by user at step {}/{}", step, executionSteps);
                    return new PerformResult(CronJobResult.ERROR, CronJobStatus.CANCELLED, 
                            "Job aborted by user at step " + step).toJobResult();
                }
                
                LOG.info("=== Executing Step {}/{} ===", step, executionSteps);
                
                // Step-specific logging based on log level
                switch (logLevel.toUpperCase()) {
                    case "TRACE":
                        LOG.trace("TRACE: Step {} - Very detailed trace information", step);
                        LOG.trace("TRACE: Step {} - Processing item {}", step, step * 10);
                        break;
                    case "DEBUG":
                        LOG.debug("DEBUG: Step {} - Debug information for step", step);
                        LOG.debug("DEBUG: Step {} - Processing configuration", step);
                        break;
                    case "INFO":
                        LOG.info("INFO: Step {} - General information about step", step);
                        break;
                    case "WARN":
                        LOG.warn("WARN: Step {} - Warning level information", step);
                        break;
                    case "ERROR":
                        LOG.error("ERROR: Step {} - Error level information (not actual error)", step);
                        break;
                }

                // Generate warnings if enabled
                if (generateWarnings && step % 2 == 0) {
                    LOG.warn("WARNING: Step {} - This is a test warning message", step);
                    LOG.warn("WARNING: Step {} - Simulated warning condition detected", step);
                }

                // Generate errors if enabled
                if (generateErrors && step % 3 == 0) {
                    LOG.error("ERROR: Step {} - This is a test error message", step);
                    LOG.error("ERROR: Step {} - Simulated error condition for testing", step);
                }

                // Random logs if enabled
                if (randomLogs) {
                    String[] randomMessages = {
                        "Processing data batch",
                        "Validating input parameters",
                        "Connecting to external service",
                        "Performing calculations",
                        "Updating database records",
                        "Sending notifications",
                        "Cleaning up resources",
                        "Generating reports"
                    };
                    
                    String randomMessage = randomMessages[random.nextInt(randomMessages.length)];
                    LOG.info("Random: Step {} - {}", step, randomMessage);
                }

                // Simulate work
                Thread.sleep(500);
            }

            // Final logging
            LOG.info("Logging test execution completed successfully");
            LOG.info("Total steps executed: {}", executionSteps);
            
            if (generateWarnings) {
                LOG.warn("WARNING: Test completed with warnings enabled");
            }
            
            if (generateErrors) {
                LOG.error("ERROR: Test completed with error logging enabled");
            }

            // DEBUG level final statistics
            LOG.debug("DEBUG: Final execution statistics");
            LOG.debug("DEBUG: Execution time: {} ms", System.currentTimeMillis());
            LOG.debug("DEBUG: Memory used: {} MB", (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024);

            String message = String.format("LoggingTestJob completed successfully - %d steps executed with %s logging", 
                    executionSteps, logLevel);

            return new PerformResult(CronJobResult.SUCCESS, CronJobStatus.FINISHED, message, 
                    Map.of(
                        "cronJobCode", cronJobModel.getCode(),
                        "logLevel", logLevel,
                        "generateErrors", generateErrors,
                        "generateWarnings", generateWarnings,
                        "executionSteps", executionSteps,
                        "randomLogs", randomLogs,
                        "status", "SUCCESS"
                    ), null).toJobResult();

        } catch (Exception e) {
            String errorMessage = "LoggingTestJob failed: " + e.getMessage();
            LOG.error("=== LoggingTestJob FAILED ===");
            LOG.error("Error: {}", errorMessage, e);
            LOG.error("Exception details - Class: {}, Message: {}", e.getClass().getSimpleName(), e.getMessage());
            LOG.error("Failed configuration - LogLevel: {}, Steps: {}", logLevel, executionSteps);
            return new PerformResult(CronJobResult.ERROR, CronJobStatus.FAILED, errorMessage, null, e).toJobResult();
        }
    }
}



