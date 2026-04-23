package com.sachinhandiekar.mcp.k8s.tools;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.LocalPortForward;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MCP Tool: Port-forward to pods and services.
 *
 * <p>Manages active port-forwarding sessions and supports starting and stopping them.</p>
 */
@Service
public class PortForwardTool {

    private static final Logger log = LoggerFactory.getLogger(PortForwardTool.class);

    private final KubernetesClient client;
    private final Map<String, LocalPortForward> activeForwards = new ConcurrentHashMap<>();

    public PortForwardTool(KubernetesClient client) {
        this.client = client;
    }

    @Tool(name = "port_forward", description = "Port-forward to a Kubernetes pod or service. Actions: 'start' (begin port forwarding), 'stop' (stop a specific forward), 'list' (list active forwards).")
    public String portForward(
            @ToolParam(description = "Action: 'start', 'stop', or 'list'") String action,
            @ToolParam(description = "Optional: resource type — 'pod' or 'service'. Required for 'start'.", required = false) String resourceType,
            @ToolParam(description = "Optional: name of the pod or service. Required for 'start'.", required = false) String name,
            @ToolParam(description = "Optional: namespace. Defaults to 'default'.", required = false) String namespace,
            @ToolParam(description = "Optional: local port. If omitted, a random available port is assigned.", required = false) String localPort,
            @ToolParam(description = "Optional: remote port on the pod/service. Required for 'start'.", required = false) String remotePort,
            @ToolParam(description = "Optional: forward ID for 'stop' action.", required = false) String forwardId
    ) {
        try {
            String act = action.trim().toLowerCase();

            return switch (act) {
                case "start" -> startForward(resourceType, name, namespace, localPort, remotePort);
                case "stop" -> stopForward(forwardId);
                case "list" -> listForwards();
                default -> "Unknown action: " + action + ". Use 'start', 'stop', or 'list'.";
            };
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    private String startForward(String resourceType, String name, String namespace, String localPort, String remotePort) {
        if (name == null || name.isBlank()) {
            return "Error: 'name' is required for 'start' action.";
        }
        if (remotePort == null || remotePort.isBlank()) {
            return "Error: 'remotePort' is required for 'start' action.";
        }

        String ns = (namespace == null || namespace.isBlank()) ? "default" : namespace.trim();
        int remote = Integer.parseInt(remotePort.trim());
        String type = (resourceType == null || resourceType.isBlank()) ? "pod" : resourceType.trim().toLowerCase();

        LocalPortForward forward;
        if ("service".equals(type) || "svc".equals(type)) {
            if (localPort != null && !localPort.isBlank()) {
                forward = client.services().inNamespace(ns).withName(name)
                        .portForward(remote, Integer.parseInt(localPort.trim()));
            } else {
                forward = client.services().inNamespace(ns).withName(name)
                        .portForward(remote);
            }
        } else {
            if (localPort != null && !localPort.isBlank()) {
                forward = client.pods().inNamespace(ns).withName(name)
                        .portForward(remote, Integer.parseInt(localPort.trim()));
            } else {
                forward = client.pods().inNamespace(ns).withName(name)
                        .portForward(remote);
            }
        }

        String id = type + "/" + name + ":" + forward.getLocalPort() + "->" + remote;
        activeForwards.put(id, forward);

        return String.format("Port forwarding started: localhost:%d -> %s/%s:%d (namespace: %s)\nForward ID: %s",
                forward.getLocalPort(), type, name, remote, ns, id);
    }

    private String stopForward(String forwardId) {
        if (forwardId == null || forwardId.isBlank()) {
            return "Error: 'forwardId' is required for 'stop' action. Use 'list' to see active forwards.";
        }
        LocalPortForward forward = activeForwards.remove(forwardId.trim());
        if (forward == null) {
            return "No active forward found with ID: " + forwardId;
        }
        try {
            forward.close();
        } catch (Exception e) {
            log.warn("Error closing port forward: {}", e.getMessage());
        }
        return "Stopped port forward: " + forwardId;
    }

    private String listForwards() {
        if (activeForwards.isEmpty()) {
            return "No active port forwards.";
        }
        StringBuilder sb = new StringBuilder("Active port forwards:\n");
        activeForwards.forEach((id, forward) -> {
            sb.append(String.format("  %s (local port: %d, alive: %b)%n",
                    id, forward.getLocalPort(), forward.isAlive()));
        });
        return sb.toString().trim();
    }
}
