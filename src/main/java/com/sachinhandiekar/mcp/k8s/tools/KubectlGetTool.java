package com.sachinhandiekar.mcp.k8s.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.sachinhandiekar.mcp.k8s.config.McpServerProperties;
import com.sachinhandiekar.mcp.k8s.util.SecretsMasker;
import io.fabric8.kubernetes.api.model.GenericKubernetesResourceList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/**
 * MCP Tool: Get/list Kubernetes resources (equivalent to kubectl get).
 */
@Service
public class KubectlGetTool {

    private final KubernetesClient client;
    private final McpServerProperties properties;
    private final SecretsMasker secretsMasker;
    private final ObjectMapper mapper;

    public KubectlGetTool(KubernetesClient client, McpServerProperties properties, SecretsMasker secretsMasker) {
        this.client = client;
        this.properties = properties;
        this.secretsMasker = secretsMasker;
        this.mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    }

    @Tool(name = "kubectl_get", description = "Get or list Kubernetes resources. Supports all resource types (pods, deployments, services, configmaps, secrets, nodes, namespaces, etc.). Returns JSON output. Use labelSelector to filter by labels and fieldSelector to filter by fields.")
    public String kubectlGet(
            @ToolParam(description = "Resource type (e.g., pods, deployments, services, configmaps, secrets, nodes, namespaces, pvc, ingress, jobs, cronjobs, daemonsets, statefulsets, replicasets, etc.)") String resourceType,
            @ToolParam(description = "Optional: specific resource name. If omitted, lists all resources of the type.", required = false) String name,
            @ToolParam(description = "Optional: namespace. If omitted, uses 'default'. Use '_all' for all namespaces.", required = false) String namespace,
            @ToolParam(description = "Optional: label selector (e.g., 'app=nginx,env=prod').", required = false) String labelSelector,
            @ToolParam(description = "Optional: field selector (e.g., 'status.phase=Running').", required = false) String fieldSelector
    ) {
        try {
            String ns = resolveNamespace(namespace);
            String normalizedType = normalizeResourceType(resourceType);
            String json;

            if (name != null && !name.isBlank()) {
                // Get a specific resource
                json = getSpecificResource(normalizedType, name, ns);
            } else {
                // List resources
                json = listResources(normalizedType, ns, labelSelector, fieldSelector);
            }

            // Mask secrets if enabled
            if (normalizedType.equals("secrets") || normalizedType.equals("secret")) {
                json = secretsMasker.maskIfSecret(json, properties.isSecretsMasking());
            }

            return json;
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    private String getSpecificResource(String resourceType, String name, String namespace) throws Exception {
        var resource = client.genericKubernetesResources(buildContext(resourceType))
                .inNamespace(namespace)
                .withName(name)
                .get();
        if (resource == null) {
            return String.format("Resource %s/%s not found in namespace %s", resourceType, name, namespace);
        }
        return mapper.writeValueAsString(resource);
    }

    private String listResources(String resourceType, String namespace, String labelSelector, String fieldSelector) throws Exception {
        GenericKubernetesResourceList list;

        if ("_all".equals(namespace)) {
            var op = client.genericKubernetesResources(buildContext(resourceType)).inAnyNamespace();
            if (labelSelector != null && !labelSelector.isBlank()) {
                list = op.withLabelSelector(labelSelector).list();
            } else {
                list = op.list();
            }
        } else {
            var op = client.genericKubernetesResources(buildContext(resourceType)).inNamespace(namespace);
            if (labelSelector != null && !labelSelector.isBlank()) {
                if (fieldSelector != null && !fieldSelector.isBlank() && fieldSelector.contains("=")) {
                    String[] parts = fieldSelector.split("=", 2);
                    list = op.withLabelSelector(labelSelector).withField(parts[0], parts[1]).list();
                } else {
                    list = op.withLabelSelector(labelSelector).list();
                }
            } else if (fieldSelector != null && !fieldSelector.isBlank() && fieldSelector.contains("=")) {
                String[] parts = fieldSelector.split("=", 2);
                list = op.withField(parts[0], parts[1]).list();
            } else {
                list = op.list();
            }
        }

        return mapper.writeValueAsString(list);
    }

    private CustomResourceDefinitionContext buildContext(String resourceType) {
        // Map common resource type aliases to their API group/version/kind
        return switch (resourceType.toLowerCase()) {
            case "pod", "pods" -> new CustomResourceDefinitionContext.Builder()
                    .withGroup("").withVersion("v1").withPlural("pods").withScope("Namespaced").build();
            case "service", "services", "svc" -> new CustomResourceDefinitionContext.Builder()
                    .withGroup("").withVersion("v1").withPlural("services").withScope("Namespaced").build();
            case "configmap", "configmaps", "cm" -> new CustomResourceDefinitionContext.Builder()
                    .withGroup("").withVersion("v1").withPlural("configmaps").withScope("Namespaced").build();
            case "secret", "secrets" -> new CustomResourceDefinitionContext.Builder()
                    .withGroup("").withVersion("v1").withPlural("secrets").withScope("Namespaced").build();
            case "namespace", "namespaces", "ns" -> new CustomResourceDefinitionContext.Builder()
                    .withGroup("").withVersion("v1").withPlural("namespaces").withScope("Cluster").build();
            case "node", "nodes" -> new CustomResourceDefinitionContext.Builder()
                    .withGroup("").withVersion("v1").withPlural("nodes").withScope("Cluster").build();
            case "persistentvolumeclaim", "persistentvolumeclaims", "pvc" -> new CustomResourceDefinitionContext.Builder()
                    .withGroup("").withVersion("v1").withPlural("persistentvolumeclaims").withScope("Namespaced").build();
            case "persistentvolume", "persistentvolumes", "pv" -> new CustomResourceDefinitionContext.Builder()
                    .withGroup("").withVersion("v1").withPlural("persistentvolumes").withScope("Cluster").build();
            case "serviceaccount", "serviceaccounts", "sa" -> new CustomResourceDefinitionContext.Builder()
                    .withGroup("").withVersion("v1").withPlural("serviceaccounts").withScope("Namespaced").build();
            case "event", "events" -> new CustomResourceDefinitionContext.Builder()
                    .withGroup("").withVersion("v1").withPlural("events").withScope("Namespaced").build();
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
            case "networkpolicy", "networkpolicies", "netpol" -> new CustomResourceDefinitionContext.Builder()
                    .withGroup("networking.k8s.io").withVersion("v1").withPlural("networkpolicies").withScope("Namespaced").build();
            case "job", "jobs" -> new CustomResourceDefinitionContext.Builder()
                    .withGroup("batch").withVersion("v1").withPlural("jobs").withScope("Namespaced").build();
            case "cronjob", "cronjobs", "cj" -> new CustomResourceDefinitionContext.Builder()
                    .withGroup("batch").withVersion("v1").withPlural("cronjobs").withScope("Namespaced").build();
            case "role", "roles" -> new CustomResourceDefinitionContext.Builder()
                    .withGroup("rbac.authorization.k8s.io").withVersion("v1").withPlural("roles").withScope("Namespaced").build();
            case "rolebinding", "rolebindings" -> new CustomResourceDefinitionContext.Builder()
                    .withGroup("rbac.authorization.k8s.io").withVersion("v1").withPlural("rolebindings").withScope("Namespaced").build();
            case "clusterrole", "clusterroles" -> new CustomResourceDefinitionContext.Builder()
                    .withGroup("rbac.authorization.k8s.io").withVersion("v1").withPlural("clusterroles").withScope("Cluster").build();
            case "clusterrolebinding", "clusterrolebindings" -> new CustomResourceDefinitionContext.Builder()
                    .withGroup("rbac.authorization.k8s.io").withVersion("v1").withPlural("clusterrolebindings").withScope("Cluster").build();
            case "storageclass", "storageclasses", "sc" -> new CustomResourceDefinitionContext.Builder()
                    .withGroup("storage.k8s.io").withVersion("v1").withPlural("storageclasses").withScope("Cluster").build();
            case "hpa", "horizontalpodautoscaler", "horizontalpodautoscalers" -> new CustomResourceDefinitionContext.Builder()
                    .withGroup("autoscaling").withVersion("v2").withPlural("horizontalpodautoscalers").withScope("Namespaced").build();
            default -> new CustomResourceDefinitionContext.Builder()
                    .withGroup("").withVersion("v1").withPlural(resourceType.toLowerCase()).withScope("Namespaced").build();
        };
    }

    private String normalizeResourceType(String resourceType) {
        if (resourceType == null) return "pods";
        return resourceType.trim().toLowerCase();
    }

    private String resolveNamespace(String namespace) {
        if (namespace == null || namespace.isBlank()) {
            return "default";
        }
        return namespace.trim();
    }
}
