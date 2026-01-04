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
 * Report generation job that creates various business reports
 * Demonstrates complex parameter handling and file operations
 */
@JobComponent(
    displayName = "Report Generation Job",
    description = "Generates various business reports in different formats",
    category = "REPORTING",
    parameters = {
        @JobParameter(
            name = "reportType",
            type = ParameterType.ENUM,
            displayName = "Report Type",
            description = "Type of report to generate",
            required = true,
            options = "SALES_SUMMARY,PRODUCT_ANALYSIS,CUSTOMER_INSIGHTS,INVENTORY_STATUS,FINANCIAL_REPORT"
        ),
        @JobParameter(
            name = "dateRange",
            type = ParameterType.ENUM,
            displayName = "Date Range",
            description = "Time period for the report",
            required = true,
            options = "LAST_7_DAYS,LAST_30_DAYS,LAST_3_MONTHS,LAST_YEAR,CUSTOM"
        ),
        @JobParameter(
            name = "startDate",
            type = ParameterType.DATE,
            displayName = "Start Date",
            description = "Start date for custom date range",
            required = false
        ),
        @JobParameter(
            name = "endDate",
            type = ParameterType.DATE,
            displayName = "End Date",
            description = "End date for custom date range",
            required = false
        ),
        @JobParameter(
            name = "outputFormat",
            type = ParameterType.ENUM,
            displayName = "Output Format",
            description = "Format of the generated report",
            required = true,
            options = "PDF,EXCEL,CSV,JSON"
        ),
        @JobParameter(
            name = "includeCharts",
            type = ParameterType.BOOLEAN,
            displayName = "Include Charts",
            description = "Whether to include charts in the report",
            required = false,
            defaultValue = "true"
        ),
        @JobParameter(
            name = "emailRecipients",
            type = ParameterType.TEXTAREA,
            displayName = "Email Recipients",
            description = "Comma-separated list of email addresses to send the report",
            required = false
        ),
        @JobParameter(
            name = "reportTitle",
            type = ParameterType.STRING,
            displayName = "Report Title",
            description = "Custom title for the report",
            required = false,
            defaultValue = "Business Report"
        ),
        @JobParameter(
            name = "filters",
            type = ParameterType.JSON,
            displayName = "Report Filters",
            description = "Additional filters to apply to the report data",
            required = false
        ),
        @JobParameter(
            name = "compressionLevel",
            type = ParameterType.ENUM,
            displayName = "Compression Level",
            description = "Compression level for the output file",
            required = false,
            defaultValue = "MEDIUM",
            options = "NONE,LOW,MEDIUM,HIGH"
        )
    }
)
@Component("reportGenerationJob")
public class ReportGenerationJob extends AbstractJob<CronJobModel> {

    private static final Logger LOG = LoggerFactory.getLogger(ReportGenerationJob.class);

    @NotNull
    @Override
    public String getJobName() {
        return "Report Generation Job";
    }

    @Override
    public String getDescription() {
        return "Generates various business reports in different formats";
    }

    @Override
    public boolean validateCronJobModel(CronJobModel cronJobModel) {
        Map<String, Object> params = cronJobModel.getParameters();
        if (params == null) {
            LOG.error("No parameters provided for ReportGenerationJob");
            return false;
        }

        // Validate required parameters
        String reportType = (String) params.get("reportType");
        String dateRange = (String) params.get("dateRange");
        String outputFormat = (String) params.get("outputFormat");

        if (reportType == null || reportType.trim().isEmpty()) {
            LOG.error("Report type is required");
            return false;
        }

        if (dateRange == null || dateRange.trim().isEmpty()) {
            LOG.error("Date range is required");
            return false;
        }

        if (outputFormat == null || outputFormat.trim().isEmpty()) {
            LOG.error("Output format is required");
            return false;
        }

        // Validate custom date range
        if ("CUSTOM".equals(dateRange)) {
            String startDate = (String) params.get("startDate");
            String endDate = (String) params.get("endDate");
            
            if (startDate == null || startDate.trim().isEmpty()) {
                LOG.error("Start date is required for custom date range");
                return false;
            }
            
            if (endDate == null || endDate.trim().isEmpty()) {
                LOG.error("End date is required for custom date range");
                return false;
            }
        }

        return true;
    }

    @NotNull
    @Override
    public JobResult execute(@NotNull CronJobModel cronJobModel) {
        LOG.info("Starting ReportGenerationJob execution for CronJob: {}", cronJobModel.getCode());
        
        // Extract parameters from parameters JSON
        Map<String, Object> params = cronJobModel.getParameters();
        String reportType = (String) params.get("reportType");
        String dateRange = (String) params.get("dateRange");
        String startDate = (String) params.get("startDate");
        String endDate = (String) params.get("endDate");
        String outputFormat = (String) params.get("outputFormat");
        Boolean includeCharts = (Boolean) params.getOrDefault("includeCharts", true);
        String emailRecipients = (String) params.get("emailRecipients");
        String reportTitle = (String) params.getOrDefault("reportTitle", "Business Report");
        String filters = (String) params.get("filters");
        String compressionLevel = (String) params.getOrDefault("compressionLevel", "MEDIUM");
        
        LOG.info("Report generation parameters:");
        LOG.info("  - Report Type: {}", reportType);
        LOG.info("  - Date Range: {}", dateRange);
        LOG.info("  - Start Date: {}", startDate);
        LOG.info("  - End Date: {}", endDate);
        LOG.info("  - Output Format: {}", outputFormat);
        LOG.info("  - Include Charts: {}", includeCharts);
        LOG.info("  - Email Recipients: {}", emailRecipients);
        LOG.info("  - Report Title: {}", reportTitle);
        LOG.info("  - Filters: {}", filters);
        LOG.info("  - Compression Level: {}", compressionLevel);

        try {
            // Simulate report generation process
            String fileName = generateReportFileName(reportType, outputFormat);
            String filePath = "/reports/" + fileName;
            
            LOG.info("Generating report: {}", fileName);
            
            // Simulate data collection
            int totalRecords = simulateDataCollection(reportType, dateRange);
            LOG.info("Collected {} records for report", totalRecords);
            
            // Simulate report processing
            Thread.sleep(2000); // Simulate processing time
            
            // Simulate file generation
            long fileSize = simulateFileGeneration(outputFormat, includeCharts, compressionLevel);
            LOG.info("Generated report file: {} ({} bytes)", fileName, fileSize);
            
            // Simulate email sending if recipients provided
            if (emailRecipients != null && !emailRecipients.trim().isEmpty()) {
                simulateEmailSending(emailRecipients, fileName, filePath);
            }
            
            String message = String.format("Report generated successfully: %s (%d records, %d bytes)", 
                    fileName, totalRecords, fileSize);
            
            LOG.info("ReportGenerationJob completed successfully: {}", message);
            
            return new JobResult(true, message, Map.of(
                "reportType", reportType,
                "dateRange", dateRange,
                "outputFormat", outputFormat,
                "fileName", fileName,
                "filePath", filePath,
                "fileSize", fileSize,
                "totalRecords", totalRecords,
                "includeCharts", includeCharts,
                "compressionLevel", compressionLevel,
                "emailSent", emailRecipients != null && !emailRecipients.trim().isEmpty()
            ));

        } catch (Exception e) {
            String errorMessage = "Report generation failed: " + e.getMessage();
            LOG.error("ReportGenerationJob failed: {}", errorMessage, e);
            return new JobResult(false, errorMessage, null, e);
        }
    }
    
    private String generateReportFileName(String reportType, String outputFormat) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        return String.format("%s_%s.%s", reportType.toLowerCase(), timestamp, outputFormat.toLowerCase());
    }
    
    private int simulateDataCollection(String reportType, String dateRange) {
        // Simulate different data sizes based on report type and date range
        int baseRecords = 1000;
        
        switch (reportType) {
            case "SALES_SUMMARY":
                baseRecords = 5000;
                break;
            case "PRODUCT_ANALYSIS":
                baseRecords = 10000;
                break;
            case "CUSTOMER_INSIGHTS":
                baseRecords = 15000;
                break;
            case "INVENTORY_STATUS":
                baseRecords = 3000;
                break;
            case "FINANCIAL_REPORT":
                baseRecords = 2000;
                break;
        }
        
        switch (dateRange) {
            case "LAST_7_DAYS":
                return baseRecords / 4;
            case "LAST_30_DAYS":
                return baseRecords;
            case "LAST_3_MONTHS":
                return baseRecords * 3;
            case "LAST_YEAR":
                return baseRecords * 12;
            case "CUSTOM":
                return baseRecords * 2;
            default:
                return baseRecords;
        }
    }
    
    private long simulateFileGeneration(String outputFormat, Boolean includeCharts, String compressionLevel) {
        long baseSize = 1024 * 1024; // 1MB base size
        
        // Adjust size based on format
        switch (outputFormat) {
            case "PDF":
                baseSize *= 2;
                break;
            case "EXCEL":
                baseSize *= 3;
                break;
            case "CSV":
                baseSize *= 1;
                break;
            case "JSON":
                baseSize *= 2;
                break;
        }
        
        // Adjust size based on charts
        if (includeCharts) {
            baseSize *= 1.5;
        }
        
        // Adjust size based on compression
        switch (compressionLevel) {
            case "NONE":
                break;
            case "LOW":
                baseSize *= 0.8;
                break;
            case "MEDIUM":
                baseSize *= 0.6;
                break;
            case "HIGH":
                baseSize *= 0.4;
                break;
        }
        
        return (long) baseSize;
    }
    
    private void simulateEmailSending(String emailRecipients, String fileName, String filePath) throws InterruptedException {
        String[] recipients = emailRecipients.split(",");
        LOG.info("Sending report to {} recipients: {}", recipients.length, emailRecipients);
        
        // Simulate email sending time
        Thread.sleep(1000);
        
        LOG.info("Report email sent successfully to: {}", emailRecipients);
    }
}
