package com.sachinhandiekar.mcp.k8s.tools;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * MCP Tool: Delete Kubernetes resources (equivalent to kubectl delete).
 *
 * <p><strong>Disabled in non-destructive mode.</strong></p>
 */
@Service
@ConditionalOnProperty(name = "k8s.mcp.non-destructive-mode", havingValue = "false", matchIfMissing = true)
public class KubectlDeleteTool {

    private final KubernetesClient client;

    public KubectlDeleteTool(KubernetesClient client) {
        this.client = client;
    }

    @Tool(name = "kubectl_delete", description = "Delete a Kubernetes resource. WARNING: This is a destructive operation. The resource will be permanently removed. This tool is disabled in non-destructive mode.")
    public String kubectlDelete(
            @ToolParam(description = "Resource type (e.g., pod, deployment, service, configmap)") String resourceType,
            @ToolParam(description = "Name of the resource to delete") String name,
            @ToolParam(description = "Optional: namespace. Defaults to 'default'.", required = false) String namespace,
            @ToolParam(description = "Optional: grace period in seconds. Use 0 for immediate deletion.", required = false) String gracePeriodSeconds
    ) {
        try {
            String ns = (namespace == null || namespace.isBlank()) ? "default" : namespace.trim();

            var resource = client.genericKubernetesResources(buildContext(resourceType))
                    .inNamespace(ns)
                    .withName(name);

            if (gracePeriodSeconds != null && !gracePeriodSeconds.isBlank()) {
                resource.withGracePeriod(Long.parseLong(gracePeriodSeconds));
            }

            var deleted = resource.delete();
            if (deleted != null && !deleted.isEmpty()) {
                return String.format("Deleted %s/%s from namespace %s", resourceType, name, ns);
            } else {
                return String.format("Resource %s/%s not found in namespace %s", resourceType, name, ns);
            }
        } catch (Exception e) {
            return "Error deleting resource: " + e.getMessage();
        }
    }

    private CustomResourceDefinitionContext buildContext(String resourceType) {
        String plural = resourceType.toLowerCase().trim();
        // Determine API group
        return switch (plural) {
            case "deployment", "deployments", "deploy" -> new CustomResourceDefinitionContext.Builder()
                    .withGroup("apps").withVersion("v1").withPlural("deployments").withScope("Namespaced").build();
            case "statefulset", "statefulsets", "sts" -> new CustomResourceDefinitionContext.Builder()
                    .withGroup("apps").withVersion("v1").withPlural("statefulsets").withScope("Namespaced").build();
            case "daemonset", "daemonsets", "ds" -> new CustomResourceDefinitionContext.Builder()
                    .withGroup("apps").withVersion("v1").withPlural("daemonsets").withScope("Namespaced").build();
            case "replicaset", "replicasets", "rs" -> new CustomResourceDefinitionContext.Builder()
                    .withGroup("apps").withVersion("v1").withPlural("replicasets").withScope("Namespaced").build();
            case "ingress", "ingresses", "ing" -> new CustomResourceDefinitionContext.Builder()
                    .withGroup("networking.k8s.io").withVersion("v1").withPlural("ingresses").withScope("Namespaced").build();
            case "job", "jobs" -> new CustomResourceDefinitionContext.Builder()
                    .withGroup("batch").withVersion("v1").withPlural("jobs").withScope("Namespaced").build();
            case "cronjob", "cronjobs", "cj" -> new CustomResourceDefinitionContext.Builder()
                    .withGroup("batch").withVersion("v1").withPlural("cronjobs").withScope("Namespaced").build();
            default -> new CustomResourceDefinitionContext.Builder()
                    .withGroup("").withVersion("v1").withPlural(plural.endsWith("s") ? plural : plural + "s").withScope("Namespaced").build();
        };
    }
}
