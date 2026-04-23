package com.sachinhandiekar.mcp.k8s.tools;

import io.fabric8.kubernetes.api.model.NamedContext;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * MCP Tool: Manage kubectl contexts (equivalent to kubectl config use-context / get-contexts).
 */
@Service
public class KubectlContextTool {

    private final KubernetesClient client;

    public KubectlContextTool(KubernetesClient client) {
        this.client = client;
    }

    @Tool(name = "kubectl_context", description = "Manage kubectl contexts. Actions: 'list' (list all contexts), 'current' (get current context), 'use' (switch to a named context).")
    public String kubectlContext(
            @ToolParam(description = "Action: 'list', 'current', or 'use'") String action,
            @ToolParam(description = "Optional: context name (required for 'use' action).", required = false) String contextName
    ) {
        try {
            String act = action.trim().toLowerCase();
            Config config = client.getConfiguration();

            return switch (act) {
                case "list" -> {
                    List<NamedContext> contexts = config.getContexts();
                    if (contexts == null || contexts.isEmpty()) {
                        yield "No contexts found in kubeconfig.";
                    }
                    NamedContext current = config.getCurrentContext();
                    String currentName = current != null ? current.getName() : "";

                    yield contexts.stream()
                            .map(ctx -> {
                                String marker = ctx.getName().equals(currentName) ? "* " : "  ";
                                String cluster = ctx.getContext().getCluster();
                                String user = ctx.getContext().getUser();
                                String ns = ctx.getContext().getNamespace();
                                return String.format("%s%-30s cluster: %-25s user: %-20s namespace: %s",
                                        marker, ctx.getName(), cluster, user, ns != null ? ns : "default");
                            })
                            .collect(Collectors.joining("\n"));
                }
                case "current" -> {
                    NamedContext current = config.getCurrentContext();
                    if (current == null) {
                        yield "No current context set.";
                    }
                    yield String.format("Current context: %s\n  Cluster: %s\n  User: %s\n  Namespace: %s",
                            current.getName(),
                            current.getContext().getCluster(),
                            current.getContext().getUser(),
                            current.getContext().getNamespace() != null ? current.getContext().getNamespace() : "default");
                }
                case "use" -> {
                    if (contextName == null || contextName.isBlank()) {
                        yield "Error: contextName is required for 'use' action.";
                    }
                    List<NamedContext> contexts = config.getContexts();
                    boolean found = contexts != null && contexts.stream()
                            .anyMatch(ctx -> ctx.getName().equals(contextName.trim()));
                    if (!found) {
                        yield "Error: context '" + contextName + "' not found. Use 'list' to see available contexts.";
                    }
                    config.setCurrentContext(contexts.stream()
                            .filter(ctx -> ctx.getName().equals(contextName.trim()))
                            .findFirst().orElse(null));
                    yield "Switched to context: " + contextName.trim();
                }
                default -> "Unknown action: " + action + ". Use 'list', 'current', or 'use'.";
            };
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}
