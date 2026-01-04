package com.trkgrn.jobscheduler.platform.infra.config;

import com.trkgrn.jobscheduler.platform.common.interceptor.TraceInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class TracingConfiguration implements WebMvcConfigurer {

    private final TraceInterceptor traceInterceptor;

    public TracingConfiguration(TraceInterceptor traceInterceptor) {
        this.traceInterceptor = traceInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(traceInterceptor);
    }
}


