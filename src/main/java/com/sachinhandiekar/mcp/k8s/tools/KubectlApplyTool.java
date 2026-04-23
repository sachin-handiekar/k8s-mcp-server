package com.sachinhandiekar.mcp.k8s.tools;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * MCP Tool: Apply a YAML manifest (equivalent to kubectl apply — server-side apply).
 */
@Service
public class KubectlApplyTool {

    private final KubernetesClient client;

    public KubectlApplyTool(KubernetesClient client) {
        this.client = client;
    }

    @Tool(name = "kubectl_apply", description = "Apply a Kubernetes YAML or JSON manifest. Creates the resource if it doesn't exist, or updates it if it does (server-side apply semantics). Supports multi-document YAML.")
    public String kubectlApply(
            @ToolParam(description = "YAML or JSON manifest to apply") String manifest,
            @ToolParam(description = "Optional: namespace. If omitted, uses the namespace from the manifest or 'default'.", required = false) String namespace
    ) {
        try {
            var inputStream = new ByteArrayInputStream(manifest.getBytes(StandardCharsets.UTF_8));
            List<HasMetadata> resources = client.load(inputStream).items();

            if (resources.isEmpty()) {
                return "Error: No valid Kubernetes resources found in the provided manifest.";
            }

            StringBuilder result = new StringBuilder();
            for (HasMetadata resource : resources) {
                if (namespace != null && !namespace.isBlank()) {
                    resource.getMetadata().setNamespace(namespace);
                }
                var applied = client.resource(resource).serverSideApply();
                result.append(String.format("Applied %s/%s in namespace %s%n",
                        applied.getKind(),
                        applied.getMetadata().getName(),
                        applied.getMetadata().getNamespace() != null ? applied.getMetadata().getNamespace() : "cluster-scoped"));
            }
            return result.toString().trim();
        } catch (Exception e) {
            return "Error applying manifest: " + e.getMessage();
        }
    }
}
