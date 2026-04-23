package com.sachinhandiekar.mcp.k8s.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for the Kubernetes MCP Server.
 */
@Component
@ConfigurationProperties(prefix = "k8s.mcp")
public class McpServerProperties {

    /**
     * When true, destructive tools (delete, uninstall, cleanup, node drain)
     * are disabled. Only read and create/update operations are allowed.
     */
    private boolean nonDestructiveMode = false;

    /**
     * When true, sensitive data in Kubernetes Secret resources is masked
     * in tool output (data and stringData fields are replaced with "***MASKED***").
     */
    private boolean secretsMasking = true;

    /**
     * Optional custom path to the kubeconfig file.
     * If empty, defaults to standard resolution (KUBECONFIG env → ~/.kube/config → in-cluster).
     */
    private String kubeconfigPath = "";

    public boolean isNonDestructiveMode() {
        return nonDestructiveMode;
    }

    public void setNonDestructiveMode(boolean nonDestructiveMode) {
        this.nonDestructiveMode = nonDestructiveMode;
    }

    public boolean isSecretsMasking() {
        return secretsMasking;
    }

    public void setSecretsMasking(boolean secretsMasking) {
        this.secretsMasking = secretsMasking;
    }

    public String getKubeconfigPath() {
        return kubeconfigPath;
    }

    public void setKubeconfigPath(String kubeconfigPath) {
        this.kubeconfigPath = kubeconfigPath;
    }
}
