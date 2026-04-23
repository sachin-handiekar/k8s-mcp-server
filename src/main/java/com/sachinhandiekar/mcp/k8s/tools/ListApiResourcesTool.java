package com.sachinhandiekar.mcp.k8s.tools;

import io.fabric8.kubernetes.api.model.APIResource;
import io.fabric8.kubernetes.api.model.APIResourceList;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * MCP Tool: List available API resources from the cluster's API discovery.
 */
@Service
public class ListApiResourcesTool {

    private final KubernetesClient client;

    public ListApiResourcesTool(KubernetesClient client) {
        this.client = client;
    }

    @Tool(name = "list_api_resources", description = "List all available Kubernetes API resources in the cluster. Shows resource types, their API group, whether they are namespaced, and their short names. Equivalent to 'kubectl api-resources'.")
    public String listApiResources(
            @ToolParam(description = "Optional: filter by API group (e.g., 'apps', 'batch', 'networking.k8s.io').", required = false) String apiGroup
    ) {
        try {
            StringBuilder result = new StringBuilder();
            result.append(String.format("%-40s %-30s %-12s %-15s%n", "NAME", "APIGROUP", "NAMESPACED", "SHORTNAMES"));
            result.append("-".repeat(100)).append("\n");

            // Get core API resources (v1)
            if (apiGroup == null || apiGroup.isBlank() || apiGroup.equals("")) {
                try {
                    APIResourceList coreResources = client.getApiResources("v1");
                    if (coreResources != null && coreResources.getResources() != null) {
                        for (APIResource res : coreResources.getResources()) {
                            if (res.getName().contains("/")) continue; // Skip subresources
                            result.append(String.format("%-40s %-30s %-12s %-15s%n",
                                    res.getName(),
                                    "",
                                    res.getNamespaced() ? "true" : "false",
                                    res.getShortNames() != null ? String.join(",", res.getShortNames()) : ""));
                        }
                    }
                } catch (Exception ignored) {
                    // Some clusters may not support this
                }
            }

            // Get resources from API groups
            var groupList = client.getApiGroups();
            if (groupList != null && groupList.getGroups() != null) {
                for (var group : groupList.getGroups()) {
                    if (apiGroup != null && !apiGroup.isBlank() && !group.getName().equals(apiGroup)) {
                        continue;
                    }
                    try {
                        String version = group.getPreferredVersion().getGroupVersion();
                        APIResourceList resources = client.getApiResources(version);
                        if (resources != null && resources.getResources() != null) {
                            for (APIResource res : resources.getResources()) {
                                if (res.getName().contains("/")) continue; // Skip subresources
                                result.append(String.format("%-40s %-30s %-12s %-15s%n",
                                        res.getName(),
                                        group.getName(),
                                        res.getNamespaced() ? "true" : "false",
                                        res.getShortNames() != null ? String.join(",", res.getShortNames()) : ""));
                            }
                        }
                    } catch (Exception ignored) {
                        // Skip groups that can't be queried
                    }
                }
            }

            return result.toString();
        } catch (Exception e) {
            return "Error listing API resources: " + e.getMessage();
        }
    }
}
