package com.trkgrn.jobscheduler.modules.job.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Utility to identify the current pod/node where the application is running
 * Used to track which pod is executing which job
 */
@Component
public class NodeIdentifier {

    private static final Logger LOG = LoggerFactory.getLogger(NodeIdentifier.class);
    
    private final String nodeId;
    
    @Value("${KUBERNETES_POD_NAME:}")
    private String kubernetesPodName;
    
    @Value("${HOSTNAME:}")
    private String hostname;
    
    @Value("${NODE_ID:}")
    private String configuredNodeId;

    public NodeIdentifier(@Value("${KUBERNETES_POD_NAME:}") String kubernetesPodName,
                         @Value("${HOSTNAME:}") String hostname,
                         @Value("${NODE_ID:}") String configuredNodeId) {
        this.kubernetesPodName = kubernetesPodName;
        this.hostname = hostname;
        this.configuredNodeId = configuredNodeId;
        this.nodeId = determineNodeId();
        LOG.info("Node identifier initialized: {}", this.nodeId);
    }

    /**
     * Get the current node/pod identifier
     * Priority: configured NODE_ID > KUBERNETES_POD_NAME > HOSTNAME > hostname lookup
     */
    public String getNodeId() {
        return nodeId;
    }

    private String determineNodeId() {
        // 1. Check if explicitly configured
        if (configuredNodeId != null && !configuredNodeId.trim().isEmpty()) {
            LOG.debug("Using configured NODE_ID: {}", configuredNodeId);
            return configuredNodeId.trim();
        }
        
        // 2. Check Kubernetes pod name (most reliable in K8s environments)
        if (kubernetesPodName != null && !kubernetesPodName.trim().isEmpty()) {
            LOG.debug("Using KUBERNETES_POD_NAME: {}", kubernetesPodName);
            return kubernetesPodName.trim();
        }
        
        // 3. Check HOSTNAME environment variable
        if (hostname != null && !hostname.trim().isEmpty()) {
            LOG.debug("Using HOSTNAME: {}", hostname);
            return hostname.trim();
        }
        
        // 4. Fallback to hostname lookup
        try {
            String hostname = InetAddress.getLocalHost().getHostName();
            LOG.debug("Using hostname lookup: {}", hostname);
            return hostname;
        } catch (UnknownHostException e) {
            LOG.warn("Could not determine hostname, using default node identifier", e);
            return "unknown-node-" + System.currentTimeMillis();
        }
    }
}

