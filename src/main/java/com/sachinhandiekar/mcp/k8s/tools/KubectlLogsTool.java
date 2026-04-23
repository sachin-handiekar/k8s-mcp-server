package com.sachinhandiekar.mcp.k8s.tools;

import io.fabric8.kubernetes.client.KubernetesClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/**
 * MCP Tool: Get pod logs (equivalent to kubectl logs).
 */
@Service
public class KubectlLogsTool {

    private final KubernetesClient client;

    public KubectlLogsTool(KubernetesClient client) {
        this.client = client;
    }

    @Tool(name = "kubectl_logs", description = "Get logs from a Kubernetes pod. Supports container selection, tail lines, since time, and previous container logs.")
    public String kubectlLogs(
            @ToolParam(description = "Name of the pod") String podName,
            @ToolParam(description = "Optional: namespace. Defaults to 'default'.", required = false) String namespace,
            @ToolParam(description = "Optional: specific container name (for multi-container pods).", required = false) String container,
            @ToolParam(description = "Optional: number of tail lines to return (e.g., '100'). Returns all if omitted.", required = false) String tailLines,
            @ToolParam(description = "Optional: return logs since this relative duration (e.g., '5m', '1h', '30s').", required = false) String sinceTime,
            @ToolParam(description = "Optional: if 'true', get logs from the previous terminated container.", required = false) String previous
    ) {
        try {
            String ns = (namespace == null || namespace.isBlank()) ? "default" : namespace.trim();

            // Build log options as individual parameters
            var podResource = client.pods().inNamespace(ns).withName(podName);

            // Determine container
            var logSource = (container != null && !container.isBlank())
                    ? podResource.inContainer(container)
                    : podResource;

            // Build the log query — the Fabric8 API has a strict fluent chaining order:
            // terminated() → sinceSeconds()/sinceTime() → tailingLines() → usingTimestamps() → getLog()
            String logs;
            boolean isPrevious = "true".equalsIgnoreCase(previous);
            Integer tail = (tailLines != null && !tailLines.isBlank()) ? Integer.parseInt(tailLines.trim()) : null;
            Integer sinceSecs = (sinceTime != null && !sinceTime.isBlank()) ? parseDurationToSeconds(sinceTime.trim()) : null;

            if (isPrevious && sinceSecs != null && tail != null) {
                logs = logSource.terminated().sinceSeconds(sinceSecs).tailingLines(tail).getLog();
            } else if (isPrevious && sinceSecs != null) {
                logs = logSource.terminated().sinceSeconds(sinceSecs).getLog();
            } else if (isPrevious && tail != null) {
                logs = logSource.terminated().tailingLines(tail).getLog();
            } else if (isPrevious) {
                logs = logSource.terminated().getLog();
            } else if (sinceSecs != null && tail != null) {
                logs = logSource.sinceSeconds(sinceSecs).tailingLines(tail).getLog();
            } else if (sinceSecs != null) {
                logs = logSource.sinceSeconds(sinceSecs).getLog();
            } else if (tail != null) {
                logs = logSource.tailingLines(tail).getLog();
            } else {
                logs = logSource.getLog();
            }

            if (logs == null || logs.isBlank()) {
                return String.format("No logs available for pod %s in namespace %s", podName, ns);
            }
            return logs;
        } catch (Exception e) {
            return "Error getting logs: " + e.getMessage();
        }
    }

    /**
     * Parses a human-readable duration (e.g., "5m", "1h", "30s") to seconds.
     */
    private int parseDurationToSeconds(String duration) {
        duration = duration.trim().toLowerCase();
        if (duration.endsWith("s")) {
            return Integer.parseInt(duration.replace("s", ""));
        } else if (duration.endsWith("m")) {
            return Integer.parseInt(duration.replace("m", "")) * 60;
        } else if (duration.endsWith("h")) {
            return Integer.parseInt(duration.replace("h", "")) * 3600;
        } else if (duration.endsWith("d")) {
            return Integer.parseInt(duration.replace("d", "")) * 86400;
        }
        return Integer.parseInt(duration);
    }
}
