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
 * MCP Tool: Create Kubernetes resources from YAML/JSON (equivalent to kubectl create).
 */
@Service
public class KubectlCreateTool {

    private final KubernetesClient client;

    public KubectlCreateTool(KubernetesClient client) {
        this.client = client;
    }

    @Tool(name = "kubectl_create", description = "Create a Kubernetes resource from a YAML or JSON manifest string. The resource must not already exist. Use kubectl_apply for create-or-update semantics.")
    public String kubectlCreate(
            @ToolParam(description = "YAML or JSON manifest of the resource to create") String manifest,
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
                var created = client.resource(resource).create();
                result.append(String.format("Created %s/%s in namespace %s%n",
                        created.getKind(),
                        created.getMetadata().getName(),
                        created.getMetadata().getNamespace() != null ? created.getMetadata().getNamespace() : "cluster-scoped"));
            }
            return result.toString().trim();
        } catch (Exception e) {
            return "Error creating resource: " + e.getMessage();
        }
    }
}
