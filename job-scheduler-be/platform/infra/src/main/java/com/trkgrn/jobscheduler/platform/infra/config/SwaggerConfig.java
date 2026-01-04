package com.trkgrn.jobscheduler.platform.infra.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger/OpenAPI configuration for Job Scheduler API
 * No security required - all endpoints are publicly accessible
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Job Scheduler API",
                version = "v1",
                description = "Quartz-based Job Scheduler Modular Monolith API. " +
                        "This API allows you to manage cron jobs, triggers, and job executions.",
                license = @License(
                        name = "MIT License",
                        url = "https://opensource.org/licenses/MIT"
                )
        )
)
public class SwaggerConfig {
}

