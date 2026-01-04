package com.trkgrn.jobscheduler.modules.job.logging;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
public class JobExecutionLogAppender extends AppenderBase<ILoggingEvent> {
    
    private final JobLogCollector jobLogCollector;
    
    private Logger rootLogger;

    public JobExecutionLogAppender(JobLogCollector jobLogCollector) {
        this.jobLogCollector = jobLogCollector;
    }

    @PostConstruct
    public void init() {
        // Get the current LoggerContext
        LoggerContext loggerContext = (LoggerContext) org.slf4j.LoggerFactory.getILoggerFactory();
        rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.addAppender(this);
        setContext(loggerContext);
        start();
    }
    
    @PreDestroy
    public void destroy() {
        if (rootLogger != null) {
            rootLogger.detachAppender(this);
        }
        stop();
    }
    
    @Override
    protected void append(ILoggingEvent event) {
        // Only capture logs if we're in a job execution context
        String executionId = MDC.get("executionId");
        if (executionId != null && jobLogCollector != null) {
            try {
                Long execId = Long.parseLong(executionId);
                String level = event.getLevel().levelStr;
                String message = event.getFormattedMessage();
                
                // Add log to collector (JobLogCollector will handle level filtering)
                jobLogCollector.addLog(execId, level, message);
            } catch (NumberFormatException e) {
                // Ignore if executionId is not a valid number
            }
        }
    }
}

