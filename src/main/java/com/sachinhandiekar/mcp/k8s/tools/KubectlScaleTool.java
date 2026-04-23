package com.sachinhandiekar.mcp.k8s.tools;

import io.fabric8.kubernetes.client.KubernetesClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/**
 * MCP Tool: Scale a deployment/statefulset/replicaset (equivalent to kubectl scale).
 */
@Service
public class KubectlScaleTool {

    private final KubernetesClient client;

    public KubectlScaleTool(KubernetesClient client) {
        this.client = client;
    }

    @Tool(name = "kubectl_scale", description = "Scale a Kubernetes deployment, statefulset, or replicaset to a specified number of replicas.")
    public String kubectlScale(
            @ToolParam(description = "Resource type: 'deployment', 'statefulset', or 'replicaset'") String resourceType,
            @ToolParam(description = "Name of the resource to scale") String name,
            @ToolParam(description = "Target number of replicas") int replicas,
            @ToolParam(description = "Optional: namespace. Defaults to 'default'.", required = false) String namespace
    ) {
        try {
            String ns = (namespace == null || namespace.isBlank()) ? "default" : namespace.trim();
            String type = resourceType.trim().toLowerCase();

            switch (type) {
                case "deployment", "deployments", "deploy" -> {
                    var deployment = client.apps().deployments().inNamespace(ns).withName(name);
                    if (deployment.get() == null) {
                        return String.format("Deployment %s not found in namespace %s", name, ns);
                    }
                    deployment.scale(replicas);
                    return String.format("Scaled deployment/%s to %d replicas in namespace %s", name, replicas, ns);
                }
                case "statefulset", "statefulsets", "sts" -> {
                    var sts = client.apps().statefulSets().inNamespace(ns).withName(name);
                    if (sts.get() == null) {
                        return String.format("StatefulSet %s not found in namespace %s", name, ns);
                    }
                    sts.scale(replicas);
                    return String.format("Scaled statefulset/%s to %d replicas in namespace %s", name, replicas, ns);
                }
                case "replicaset", "replicasets", "rs" -> {
                    var rs = client.apps().replicaSets().inNamespace(ns).withName(name);
                    if (rs.get() == null) {
                        return String.format("ReplicaSet %s not found in namespace %s", name, ns);
                    }
                    rs.scale(replicas);
                    return String.format("Scaled replicaset/%s to %d replicas in namespace %s", name, replicas, ns);
                }
                default -> {
                    return "Unsupported resource type for scaling: " + resourceType + ". Use deployment, statefulset, or replicaset.";
                }
            }
        } catch (Exception e) {
            return "Error scaling resource: " + e.getMessage();
        }
    }
}
