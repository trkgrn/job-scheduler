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
 * Example job implementation for product indexing
 */
@JobComponent(
    displayName = "Product Indexing Job",
    description = "Indexes products to Elasticsearch for search functionality",
    category = "SEARCH",
    parameters = {
        @JobParameter(
            name = "batchSize",
            type = ParameterType.INTEGER,
            displayName = "Batch Size",
            description = "Number of products to process in each batch",
            required = true,
            defaultValue = "100",
            validation = "min:1,max:1000"
        ),
        @JobParameter(
            name = "forceReindex",
            type = ParameterType.BOOLEAN,
            displayName = "Force Reindex",
            description = "Whether to force reindexing of all products",
            required = false,
            defaultValue = "false"
        ),
        @JobParameter(
            name = "categoryFilter",
            type = ParameterType.STRING,
            displayName = "Category Filter",
            description = "Filter products by category (optional)",
            required = false,
            defaultValue = ""
        ),
        @JobParameter(
            name = "indexType",
            type = ParameterType.ENUM,
            displayName = "Index Type",
            description = "Type of indexing to perform",
            required = true,
            defaultValue = "FULL",
            options = "FULL,INCREMENTAL"
        ),
        @JobParameter(
            name = "elasticsearchIndexName",
            type = ParameterType.STRING,
            displayName = "Elasticsearch Index Name",
            description = "Name of the Elasticsearch index",
            required = true,
            defaultValue = "products"
        ),
        @JobParameter(
            name = "includeVariants",
            type = ParameterType.BOOLEAN,
            displayName = "Include Variants",
            description = "Whether to include product variants",
            required = false,
            defaultValue = "true"
        ),
        @JobParameter(
            name = "includeMedia",
            type = ParameterType.BOOLEAN,
            displayName = "Include Media",
            description = "Whether to include product media",
            required = false,
            defaultValue = "false"
        ),
        @JobParameter(
            name = "timeoutMinutes",
            type = ParameterType.INTEGER,
            displayName = "Timeout (Minutes)",
            description = "Job timeout in minutes",
            required = false,
            defaultValue = "30",
            validation = "min:1,max:1440"
        )
    }
)
@Component("productIndexingJob")
public class ProductIndexingJob extends AbstractJob<CronJobModel> {

    private static final Logger LOG = LoggerFactory.getLogger(ProductIndexingJob.class);

    @NotNull
    @Override
    public String getJobName() {
        return "Product Indexing Job";
    }

    @Override
    public String getDescription() {
        return "Indexes products to Elasticsearch for search functionality";
    }

    @Override
    public boolean validateCronJobModel(CronJobModel cronJobModel) {
        Map<String, Object> params = cronJobModel.getParameters();
        if (params != null && params.containsKey("batchSize")) {
            Integer batchSize = (Integer) params.get("batchSize");
            if (batchSize == null || batchSize <= 0 || batchSize > 1000) {
                LOG.error("Invalid batch size: {}", batchSize);
                return false;
            }
        }
        return true;
    }

    @NotNull
    @Override
    public JobResult execute(@NotNull CronJobModel cronJobModel) {
        LOG.info("Starting ProductIndexingJob execution for CronJob: {}", cronJobModel.getCode());
        
        // Extract parameters from parameters JSON
        Map<String, Object> params = cronJobModel.getParameters();
        Integer batchSize = (Integer) params.getOrDefault("batchSize", 100);
        Boolean forceReindex = (Boolean) params.getOrDefault("forceReindex", false);
        String categoryFilter = (String) params.getOrDefault("categoryFilter", "");
        String indexType = (String) params.getOrDefault("indexType", "FULL");
        String elasticsearchIndexName = (String) params.getOrDefault("elasticsearchIndexName", "products");
        Boolean includeVariants = (Boolean) params.getOrDefault("includeVariants", true);
        Boolean includeMedia = (Boolean) params.getOrDefault("includeMedia", false);
        Integer timeoutMinutes = (Integer) params.getOrDefault("timeoutMinutes", 30);
        
        LOG.info("Job parameters - BatchSize: {}, ForceReindex: {}, CategoryFilter: {}, IndexType: {}", 
                batchSize, forceReindex, categoryFilter, indexType);
        LOG.info("Additional parameters - ESIndex: {}, IncludeVariants: {}, IncludeMedia: {}, Timeout: {}min", 
                elasticsearchIndexName, includeVariants, includeMedia, timeoutMinutes);

        try {
            // TRACE level logs - very detailed debugging
            LOG.trace("=== ProductIndexingJob TRACE: Starting execution ===");
            LOG.trace("TRACE: CronJob ID: {}", cronJobModel.getId());
            LOG.trace("TRACE: CronJob Version: {}", cronJobModel.getVersion());
            LOG.trace("TRACE: All parameters: {}", cronJobModel.getParameters());
            
            // DEBUG level logs - debugging information
            LOG.debug("=== ProductIndexingJob DEBUG: Parameter validation ===");
            LOG.debug("DEBUG: Batch size: {}", batchSize);
            LOG.debug("DEBUG: Force reindex: {}", forceReindex);
            LOG.debug("DEBUG: Category filter: '{}'", categoryFilter);
            LOG.debug("DEBUG: Index type: {}", indexType);
            LOG.debug("DEBUG: Elasticsearch index: {}", elasticsearchIndexName);
            LOG.debug("DEBUG: Include variants: {}", includeVariants);
            LOG.debug("DEBUG: Include media: {}", includeMedia);
            LOG.debug("DEBUG: Timeout: {} minutes", timeoutMinutes);
            
            // INFO level logs - general information
            LOG.info("Starting product indexing process...");
            LOG.info("Index configuration - Type: {}, Index: {}, Batch: {}", indexType, elasticsearchIndexName, batchSize);
            
            // Simulate job execution with more detailed logging
            LOG.info("Connecting to Elasticsearch...");
            LOG.debug("DEBUG: Elasticsearch connection parameters - Index: {}, Timeout: {}ms", 
                    elasticsearchIndexName, timeoutMinutes * 60 * 1000);
            Thread.sleep(500);
            
            LOG.info("Fetching products from database (batch size: {})", batchSize);
            LOG.debug("DEBUG: Database query - Category filter: '{}', Include variants: {}", categoryFilter, includeVariants);
            Thread.sleep(1000);
            
            LOG.info("Processing products for indexing...");
            LOG.debug("DEBUG: Processing configuration - Include media: {}, Index type: {}", includeMedia, indexType);
            Thread.sleep(1000);
            
            LOG.info("Indexing products to Elasticsearch...");
            LOG.debug("DEBUG: Indexing to Elasticsearch index: {}", elasticsearchIndexName);
            Thread.sleep(500);

            // WARN level logs - potential issues
            if (batchSize > 500) {
                LOG.warn("WARNING: Large batch size ({}), this might cause memory issues", batchSize);
            }
            
            if (timeoutMinutes < 5) {
                LOG.warn("WARNING: Short timeout ({} minutes), operation might not complete", timeoutMinutes);
            }
            
            if (forceReindex && batchSize > 100) {
                LOG.warn("WARNING: Force reindex with large batch size might take a long time");
            }
            
            // ERROR level logs for demonstration (but not actual errors)
            LOG.error("ERROR: This is a test error log for ProductIndexingJob - not a real error!");
            LOG.error("ERROR: Simulating error condition for testing purposes");

            // Simulate success
            int processedCount = 150;
            String message = String.format("Successfully indexed %d products with batch size %d", 
                    processedCount, batchSize);

            LOG.info("ProductIndexingJob completed successfully: {}", message);
            LOG.info("Index statistics - Processed: {}, BatchSize: {}, ForceReindex: {}", 
                    processedCount, batchSize, forceReindex);
            
            // DEBUG level final statistics
            LOG.debug("DEBUG: Final execution details - Processed: {}, Category: '{}', IndexType: {}", 
                    processedCount, categoryFilter, indexType);

            return new JobResult(true, message, Map.of(
                "processedCount", processedCount,
                "batchSize", batchSize,
                "forceReindex", forceReindex,
                "categoryFilter", categoryFilter,
                "indexType", indexType,
                "elasticsearchIndexName", elasticsearchIndexName,
                "includeVariants", includeVariants,
                "includeMedia", includeMedia,
                "timeoutMinutes", timeoutMinutes
            ));

        } catch (Exception e) {
            String errorMessage = "Product indexing failed: " + e.getMessage();
            LOG.error("=== ProductIndexingJob FAILED ===");
            LOG.error("ProductIndexingJob failed: {}", errorMessage, e);
            LOG.error("Exception details - Class: {}, Message: {}", e.getClass().getSimpleName(), e.getMessage());
            LOG.error("Failed parameters - BatchSize: {}, IndexType: {}, CategoryFilter: '{}'", 
                    batchSize, indexType, categoryFilter);
            return new JobResult(false, errorMessage, null, e);
        }
    }
}

