package com.sachinhandiekar.mcp.k8s.tools;

import io.fabric8.kubernetes.client.KubernetesClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/**
 * MCP Tool: Verify Kubernetes cluster connectivity.
 */
@Service
public class PingTool {

    private final KubernetesClient client;

    public PingTool(KubernetesClient client) {
        this.client = client;
    }

    @Tool(name = "ping", description = "Verify connectivity to the Kubernetes cluster. Returns cluster info if connected.")
    public String ping() {
        try {
            var version = client.getKubernetesVersion();
            String currentContext = client.getConfiguration().getCurrentContext() != null
                    ? client.getConfiguration().getCurrentContext().getName()
                    : "unknown";
            String masterUrl = client.getMasterUrl().toString();

            return String.format(
                    "Connected to Kubernetes cluster!\n" +
                    "  Master URL: %s\n" +
                    "  Context: %s\n" +
                    "  Server Version: %s.%s\n" +
                    "  Platform: %s",
                    masterUrl,
                    currentContext,
                    version.getMajor(), version.getMinor(),
                    version.getPlatform()
            );
        } catch (Exception e) {
            return "Failed to connect to Kubernetes cluster: " + e.getMessage();
        }
    }
}
