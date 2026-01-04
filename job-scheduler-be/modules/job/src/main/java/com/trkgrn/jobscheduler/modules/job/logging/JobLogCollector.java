package com.trkgrn.jobscheduler.modules.job.logging;

import ch.qos.logback.classic.Level;
import com.trkgrn.jobscheduler.modules.job.model.JobExecutionModel;
import com.trkgrn.jobscheduler.modules.job.repository.JobExecutionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class JobLogCollector {
    
    private static final Logger logger = LoggerFactory.getLogger(JobLogCollector.class);
    
    private final JobExecutionRepository jobExecutionRepository;
    
    // Thread-safe storage for logs during job execution
    private final ConcurrentHashMap<String, List<JobExecutionModel.LogEntry>> executionLogs = new ConcurrentHashMap<>();
    
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    
    // Track active executions for log collection
    private final ConcurrentHashMap<String, Boolean> activeExecutions = new ConcurrentHashMap<>();
    
    // Track log level for each execution
    private final ConcurrentHashMap<String, Level> executionLogLevels = new ConcurrentHashMap<>();
    
    // Statistics
    private final AtomicInteger totalLogsCollected = new AtomicInteger(0);
    private final AtomicInteger filteredLogs = new AtomicInteger(0);
    
    public JobLogCollector(JobExecutionRepository jobExecutionRepository) {
        this.jobExecutionRepository = jobExecutionRepository;
    }
    
    /**
     * Start collecting logs for a job execution
     */
    public void startLogCollection(Long executionId, String correlationId) {
        startLogCollection(executionId, correlationId, Level.INFO);
    }
    
    /**
     * Start collecting logs for a job execution with specific log level
     */
    public void startLogCollection(Long executionId, String correlationId, Level logLevel) {
        String key = executionId.toString();
        executionLogs.put(key, new CopyOnWriteArrayList<>());
        activeExecutions.put(key, true);
        executionLogLevels.put(key, logLevel);
        
        // Set correlation ID in MDC for this thread
        MDC.put("correlationId", correlationId);
        MDC.put("executionId", executionId.toString());
        
        logger.info("Started log collection for execution ID: {} with correlation ID: {} and log level: {}", 
                executionId, correlationId, logLevel);
    }
    
    /**
     * Add a log entry for the current execution
     */
    public void addLog(Long executionId, String level, String message) {
        String key = executionId.toString();
        List<JobExecutionModel.LogEntry> logs = executionLogs.get(key);
        Level requiredLevel = executionLogLevels.get(key);
        
        if (logs != null && activeExecutions.getOrDefault(key, false)) {
            // Check if log level meets the requirement
            Level logLevel = Level.toLevel(level, Level.INFO);
            if (shouldLog(logLevel, requiredLevel)) {
                JobExecutionModel.LogEntry logEntry = new JobExecutionModel.LogEntry(
                    OffsetDateTime.now().format(TIMESTAMP_FORMATTER),
                    level,
                    message
                );
                
                logs.add(logEntry);
                totalLogsCollected.incrementAndGet();
                
                // Also log to console with correlation ID (only for important logs to avoid spam)
                if (Level.ERROR.levelStr.equals(level) || Level.WARN.levelStr.equals(level)) {
                    logger.info("[EXECUTION-{}] {}: {}", executionId, level, message);
                }
            } else {
                filteredLogs.incrementAndGet();
            }
        }
    }
    
    /**
     * Check if log should be recorded based on log level
     */
    private boolean shouldLog(Level logLevel, Level requiredLevel) {
        if (requiredLevel == null) {
            return true; // If no level specified, log everything
        }
        
        // Log levels hierarchy: TRACE < DEBUG < INFO < WARN < ERROR
        return logLevel.levelInt >= requiredLevel.levelInt;
    }
    
    /**
     * Add a log entry for the current execution (overloaded for convenience)
     */
    public void addLog(String level, String message) {
        String executionId = MDC.get("executionId");
        if (executionId != null) {
            try {
                addLog(Long.parseLong(executionId), level, message);
            } catch (NumberFormatException e) {
                // Ignore if executionId is not a valid number
            }
        }
    }
    
    /**
     * Add a log entry for the current execution with automatic level detection
     */
    public void addLog(String message) {
        addLog("INFO", message);
    }
    
    /**
     * Add a debug log entry for the current execution
     */
    public void addDebugLog(String message) {
        addLog("DEBUG", message);
    }
    
    /**
     * Add a warn log entry for the current execution
     */
    public void addWarnLog(String message) {
        addLog("WARN", message);
    }
    
    /**
     * Add an error log entry for the current execution
     */
    public void addErrorLog(String message) {
        addLog("ERROR", message);
    }
    
    /**
     * Stop collecting logs and persist them to database
     * This method is idempotent - can be called multiple times safely
     */
    public void stopLogCollectionAndPersist(Long executionId, JobExecutionModel execution) {
        String key = executionId.toString();
        
        // Check if log collection is still active for this execution
        boolean wasActive = activeExecutions.containsKey(key);
        
        // Get logs (remove from map only if collection was active)
        List<JobExecutionModel.LogEntry> logs = wasActive ? executionLogs.remove(key) : null;
        
        // Only remove from active executions if it was active
        if (wasActive) {
            activeExecutions.remove(key);
        }
        
        Level logLevel = executionLogLevels.remove(key);
        
        if (logs != null && !logs.isEmpty()) {
            // Set logs to execution entity (don't save here to avoid version conflict)
            // Only set if execution doesn't already have logs (to avoid overwriting)
            if (execution.getLogs() == null || execution.getLogs().isEmpty()) {
                execution.setLogs(new ArrayList<>(logs));
                logger.info("Collected {} log entries for execution ID: {} (Log level: {}, Total collected: {}, Filtered: {})", 
                        logs.size(), executionId, logLevel, totalLogsCollected.get(), filteredLogs.get());
            } else {
                // Execution already has logs (from previous call), merge them
                List<JobExecutionModel.LogEntry> existingLogs = execution.getLogs();
                List<JobExecutionModel.LogEntry> mergedLogs = new ArrayList<>(existingLogs);
                mergedLogs.addAll(logs);
                execution.setLogs(mergedLogs);
                logger.info("Merged {} new log entries with {} existing entries for execution ID: {} (Total: {})", 
                        logs.size(), existingLogs.size(), executionId, mergedLogs.size());
            }
        } else if (wasActive) {
            // Log collection was active but no logs found
            logger.warn("No logs found for execution ID: {} (Log level: {})", executionId, logLevel);
        } else {
            // Log collection was already stopped (idempotent call)
            if (execution.getLogs() == null || execution.getLogs().isEmpty()) {
                logger.debug("Log collection already stopped for execution ID: {} (no logs found)", executionId);
            } else {
                logger.debug("Log collection already stopped for execution ID: {} (execution has {} log entries)", 
                        executionId, execution.getLogs().size());
            }
        }
        
        // Clear MDC
        MDC.clear();
    }
    
    /**
     * Get logs for an execution (from memory if still collecting, from DB if persisted)
     */
    public List<JobExecutionModel.LogEntry> getLogs(Long executionId) {
        String key = executionId.toString();
        List<JobExecutionModel.LogEntry> logs = executionLogs.get(key);
        
        if (logs != null) {
            return new ArrayList<>(logs);
        }
        
        // If not in memory, get from database
        return jobExecutionRepository.findById(executionId)
                .map(JobExecutionModel::getLogs)
                .orElse(new ArrayList<>());
    }
    
    /**
     * Get statistics about log collection
     */
    public String getStatistics() {
        return String.format("Active executions: %d, Total logs collected: %d, Filtered logs: %d", 
                activeExecutions.size(), totalLogsCollected.get(), filteredLogs.get());
    }
    
    /**
     * Check if log collection is active for an execution
     */
    public boolean isLogCollectionActive(Long executionId) {
        return activeExecutions.getOrDefault(executionId.toString(), false);
    }
    
    /**
     * Get current log count for an execution
     */
    public int getLogCount(Long executionId) {
        String key = executionId.toString();
        List<JobExecutionModel.LogEntry> logs = executionLogs.get(key);
        return logs != null ? logs.size() : 0;
    }
}

