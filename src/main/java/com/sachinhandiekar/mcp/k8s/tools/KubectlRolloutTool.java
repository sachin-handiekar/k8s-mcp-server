package com.sachinhandiekar.mcp.k8s.tools;

import com.sachinhandiekar.mcp.k8s.util.ProcessRunner;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * MCP Tool: Manage deployment rollouts (equivalent to kubectl rollout).
 */
@Service
public class KubectlRolloutTool {

    private final KubernetesClient client;
    private final ProcessRunner processRunner;

    public KubectlRolloutTool(KubernetesClient client, ProcessRunner processRunner) {
        this.client = client;
        this.processRunner = processRunner;
    }

    @Tool(name = "kubectl_rollout", description = "Manage deployment rollouts. Supports 'status' (check rollout status), 'history' (view rollout history), 'restart' (trigger a rolling restart), and 'undo' (rollback to a previous revision).")
    public String kubectlRollout(
            @ToolParam(description = "Rollout action: 'status', 'history', 'restart', or 'undo'") String action,
            @ToolParam(description = "Resource type (e.g., deployment, daemonset, statefulset)") String resourceType,
            @ToolParam(description = "Name of the resource") String name,
            @ToolParam(description = "Optional: namespace. Defaults to 'default'.", required = false) String namespace,
            @ToolParam(description = "Optional: revision number for 'undo' (e.g., '2'). If omitted, rolls back to the previous revision.", required = false) String revision
    ) {
        try {
            String ns = (namespace == null || namespace.isBlank()) ? "default" : namespace.trim();
            String act = action.trim().toLowerCase();

            List<String> command = new ArrayList<>();
            command.add("kubectl");
            command.add("rollout");
            command.add(act);
            command.add(resourceType + "/" + name);
            command.add("-n");
            command.add(ns);

            if ("undo".equals(act) && revision != null && !revision.isBlank()) {
                command.add("--to-revision=" + revision.trim());
            }

            // Set context
            var currentContext = client.getConfiguration().getCurrentContext();
            if (currentContext != null) {
                command.add("--context");
                command.add(currentContext.getName());
            }

            ProcessRunner.ProcessResult result = processRunner.run(command);
            if (result.isSuccess()) {
                return result.stdout();
            } else {
                return "Error with rollout " + act + ": " + result.output();
            }
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}
