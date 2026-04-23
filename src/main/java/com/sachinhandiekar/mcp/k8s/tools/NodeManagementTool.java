package com.sachinhandiekar.mcp.k8s.tools;

import com.sachinhandiekar.mcp.k8s.util.ProcessRunner;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * MCP Tool: Node management operations — cordon, uncordon, drain.
 *
 * <p><strong>Disabled in non-destructive mode.</strong></p>
 */
@Service
@ConditionalOnProperty(name = "k8s.mcp.non-destructive-mode", havingValue = "false", matchIfMissing = true)
public class NodeManagementTool {

    private final KubernetesClient client;
    private final ProcessRunner processRunner;

    public NodeManagementTool(KubernetesClient client, ProcessRunner processRunner) {
        this.client = client;
        this.processRunner = processRunner;
    }

    @Tool(name = "node_management", description = "Manage Kubernetes nodes. Actions: 'cordon' (mark node unschedulable), 'uncordon' (mark node schedulable), 'drain' (safely evict pods from node for maintenance). WARNING: Destructive operation — disabled in non-destructive mode.")
    public String nodeManagement(
            @ToolParam(description = "Action: 'cordon', 'uncordon', or 'drain'") String action,
            @ToolParam(description = "Name of the node") String nodeName,
            @ToolParam(description = "Optional: for 'drain' — grace period in seconds.", required = false) String gracePeriodSeconds,
            @ToolParam(description = "Optional: for 'drain' — if 'true', ignore DaemonSet-managed pods.", required = false) String ignoreDaemonSets,
            @ToolParam(description = "Optional: for 'drain' — if 'true', delete local data (emptyDir volumes).", required = false) String deleteLocalData,
            @ToolParam(description = "Optional: for 'drain' — if 'true', force deletion of pods without controllers.", required = false) String force
    ) {
        try {
            String act = action.trim().toLowerCase();

            return switch (act) {
                case "cordon" -> cordonNode(nodeName, true);
                case "uncordon" -> cordonNode(nodeName, false);
                case "drain" -> drainNode(nodeName, gracePeriodSeconds, ignoreDaemonSets, deleteLocalData, force);
                default -> "Unknown action: " + action + ". Use 'cordon', 'uncordon', or 'drain'.";
            };
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    private String cordonNode(String nodeName, boolean cordon) {
        try {
            var node = client.nodes().withName(nodeName).get();
            if (node == null) {
                return "Node not found: " + nodeName;
            }

            node.getSpec().setUnschedulable(cordon);
            client.nodes().withName(nodeName).patch(node);

            return String.format("Node %s %s successfully",
                    nodeName, cordon ? "cordoned" : "uncordoned");
        } catch (Exception e) {
            return String.format("Error %s node %s: %s",
                    cordon ? "cordoning" : "uncordoning", nodeName, e.getMessage());
        }
    }

    private String drainNode(String nodeName, String gracePeriod, String ignoreDaemonSets,
                             String deleteLocalData, String force) {
        List<String> command = new ArrayList<>();
        command.add("kubectl");
        command.add("drain");
        command.add(nodeName);

        if (gracePeriod != null && !gracePeriod.isBlank()) {
            command.add("--grace-period=" + gracePeriod.trim());
        }

        if ("true".equalsIgnoreCase(ignoreDaemonSets)) {
            command.add("--ignore-daemonsets");
        }

        if ("true".equalsIgnoreCase(deleteLocalData)) {
            command.add("--delete-emptydir-data");
        }

        if ("true".equalsIgnoreCase(force)) {
            command.add("--force");
        }

        // Set context
        var currentContext = client.getConfiguration().getCurrentContext();
        if (currentContext != null) {
            command.add("--context");
            command.add(currentContext.getName());
        }

        ProcessRunner.ProcessResult result = processRunner.run(command, 120);
        return result.isSuccess()
                ? "Node drained successfully:\n" + result.stdout()
                : "Error draining node:\n" + result.output();
    }
}
