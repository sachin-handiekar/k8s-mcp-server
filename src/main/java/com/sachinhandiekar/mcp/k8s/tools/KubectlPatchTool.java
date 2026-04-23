package com.sachinhandiekar.mcp.k8s.tools;

import com.sachinhandiekar.mcp.k8s.util.ProcessRunner;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * MCP Tool: Patch a Kubernetes resource (equivalent to kubectl patch).
 */
@Service
public class KubectlPatchTool {

    private final KubernetesClient client;
    private final ProcessRunner processRunner;

    public KubectlPatchTool(KubernetesClient client, ProcessRunner processRunner) {
        this.client = client;
        this.processRunner = processRunner;
    }

    @Tool(name = "kubectl_patch", description = "Update fields of a Kubernetes resource using strategic merge patch, JSON merge patch, or JSON patch. Equivalent to 'kubectl patch'.")
    public String kubectlPatch(
            @ToolParam(description = "Resource type (e.g., deployment, service, pod)") String resourceType,
            @ToolParam(description = "Name of the resource to patch") String name,
            @ToolParam(description = "The patch content as a JSON string") String patch,
            @ToolParam(description = "Optional: patch type — 'strategic' (default), 'merge', or 'json'.", required = false) String patchType,
            @ToolParam(description = "Optional: namespace. Defaults to 'default'.", required = false) String namespace
    ) {
        try {
            String ns = (namespace == null || namespace.isBlank()) ? "default" : namespace.trim();
            String type = (patchType == null || patchType.isBlank()) ? "strategic" : patchType.trim().toLowerCase();

            List<String> command = new ArrayList<>();
            command.add("kubectl");
            command.add("patch");
            command.add(resourceType);
            command.add(name);
            command.add("-n");
            command.add(ns);
            command.add("--type");

            switch (type) {
                case "merge" -> command.add("merge");
                case "json" -> command.add("json");
                default -> command.add("strategic");
            }

            command.add("-p");
            command.add(patch);

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
                return "Error patching " + resourceType + "/" + name + ": " + result.output();
            }
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}
