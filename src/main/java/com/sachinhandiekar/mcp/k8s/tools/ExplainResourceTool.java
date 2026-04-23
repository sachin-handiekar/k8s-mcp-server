package com.sachinhandiekar.mcp.k8s.tools;

import com.sachinhandiekar.mcp.k8s.util.ProcessRunner;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * MCP Tool: Explain Kubernetes resource types (equivalent to kubectl explain).
 *
 * <p>Shells out to {@code kubectl explain} as there is no Fabric8 equivalent
 * for schema introspection with field descriptions.</p>
 */
@Service
public class ExplainResourceTool {

    private final KubernetesClient client;
    private final ProcessRunner processRunner;

    public ExplainResourceTool(KubernetesClient client, ProcessRunner processRunner) {
        this.client = client;
        this.processRunner = processRunner;
    }

    @Tool(name = "explain_resource", description = "Explain a Kubernetes resource type or a specific field path. Returns documentation about the resource schema, fields, and their types. Equivalent to 'kubectl explain'.")
    public String explainResource(
            @ToolParam(description = "Resource type or field path (e.g., 'pod', 'pod.spec.containers', 'deployment.spec.strategy')") String resource,
            @ToolParam(description = "Optional: if 'true', show the full recursive schema.", required = false) String recursive
    ) {
        try {
            List<String> command = new ArrayList<>();
            command.add("kubectl");
            command.add("explain");
            command.add(resource);

            if ("true".equalsIgnoreCase(recursive)) {
                command.add("--recursive");
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
                return "Error explaining " + resource + ": " + result.output();
            }
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}
