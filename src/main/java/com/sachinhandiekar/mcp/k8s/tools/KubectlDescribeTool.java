package com.sachinhandiekar.mcp.k8s.tools;

import com.sachinhandiekar.mcp.k8s.util.ProcessRunner;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * MCP Tool: Describe a Kubernetes resource (equivalent to kubectl describe).
 *
 * <p>Uses the kubectl CLI for rich human-readable describe output,
 * which includes events, conditions, and formatted spec details.</p>
 */
@Service
public class KubectlDescribeTool {

    private final KubernetesClient client;
    private final ProcessRunner processRunner;

    public KubectlDescribeTool(KubernetesClient client, ProcessRunner processRunner) {
        this.client = client;
        this.processRunner = processRunner;
    }

    @Tool(name = "kubectl_describe", description = "Describe a Kubernetes resource in detail. Returns human-readable output including events, conditions, volumes, and spec details. Similar to 'kubectl describe'.")
    public String kubectlDescribe(
            @ToolParam(description = "Resource type (e.g., pod, deployment, service, node, pvc, ingress)") String resourceType,
            @ToolParam(description = "Name of the resource to describe") String name,
            @ToolParam(description = "Optional: namespace. Defaults to 'default'.", required = false) String namespace
    ) {
        try {
            String ns = (namespace == null || namespace.isBlank()) ? "default" : namespace.trim();

            List<String> command = new ArrayList<>();
            command.add("kubectl");
            command.add("describe");
            command.add(resourceType);
            command.add(name);
            command.add("-n");
            command.add(ns);

            // Set the kubeconfig context if available
            var currentContext = client.getConfiguration().getCurrentContext();
            if (currentContext != null) {
                command.add("--context");
                command.add(currentContext.getName());
            }

            ProcessRunner.ProcessResult result = processRunner.run(command);
            if (result.isSuccess()) {
                return result.stdout();
            } else {
                return "Error describing " + resourceType + "/" + name + ": " + result.output();
            }
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}
