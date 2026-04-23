package com.sachinhandiekar.mcp.k8s.tools;

import com.sachinhandiekar.mcp.k8s.config.McpServerProperties;
import com.sachinhandiekar.mcp.k8s.util.ProcessRunner;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * MCP Tool: Helm chart operations.
 *
 * <p>Shells out to the {@code helm} CLI for chart install, upgrade, uninstall, and list operations.
 * The uninstall operation is disabled in non-destructive mode.</p>
 */
@Service
public class HelmTool {

    private final KubernetesClient client;
    private final ProcessRunner processRunner;
    private final McpServerProperties properties;

    public HelmTool(KubernetesClient client, ProcessRunner processRunner, McpServerProperties properties) {
        this.client = client;
        this.processRunner = processRunner;
        this.properties = properties;
    }

    @Tool(name = "helm_install", description = "Install a Helm chart. Supports custom values, repository URLs, chart versions, and custom release names.")
    public String helmInstall(
            @ToolParam(description = "Release name for the Helm installation") String releaseName,
            @ToolParam(description = "Chart reference (e.g., 'bitnami/nginx', './my-chart', 'oci://registry/chart')") String chart,
            @ToolParam(description = "Optional: namespace. Defaults to 'default'.", required = false) String namespace,
            @ToolParam(description = "Optional: chart version (e.g., '1.2.3').", required = false) String version,
            @ToolParam(description = "Optional: values as a YAML string to override chart defaults.", required = false) String values,
            @ToolParam(description = "Optional: Helm repository URL to add before install.", required = false) String repo,
            @ToolParam(description = "Optional: if 'true', create the namespace if it doesn't exist.", required = false) String createNamespace
    ) {
        try {
            String ns = (namespace == null || namespace.isBlank()) ? "default" : namespace.trim();

            // If a repo URL is provided, add it first
            if (repo != null && !repo.isBlank()) {
                String repoName = chart.contains("/") ? chart.split("/")[0] : "temp-repo";
                addHelmRepo(repoName, repo.trim());
            }

            List<String> command = new ArrayList<>();
            command.add("helm");
            command.add("install");
            command.add(releaseName);
            command.add(chart);
            command.add("-n");
            command.add(ns);

            if (version != null && !version.isBlank()) {
                command.add("--version");
                command.add(version.trim());
            }

            if (values != null && !values.isBlank()) {
                command.add("--set-json");
                command.add(values.trim());
            }

            if ("true".equalsIgnoreCase(createNamespace)) {
                command.add("--create-namespace");
            }

            addContextFlag(command);

            ProcessRunner.ProcessResult result = processRunner.run(command, 120);
            return result.isSuccess()
                    ? "Helm install successful:\n" + result.stdout()
                    : "Helm install failed:\n" + result.output();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    @Tool(name = "helm_upgrade", description = "Upgrade an existing Helm release to a new chart version or with new values.")
    public String helmUpgrade(
            @ToolParam(description = "Release name to upgrade") String releaseName,
            @ToolParam(description = "Chart reference") String chart,
            @ToolParam(description = "Optional: namespace.", required = false) String namespace,
            @ToolParam(description = "Optional: chart version.", required = false) String version,
            @ToolParam(description = "Optional: values as a YAML string.", required = false) String values,
            @ToolParam(description = "Optional: if 'true', install the chart if it doesn't exist yet.", required = false) String install
    ) {
        try {
            String ns = (namespace == null || namespace.isBlank()) ? "default" : namespace.trim();

            List<String> command = new ArrayList<>();
            command.add("helm");
            command.add("upgrade");
            command.add(releaseName);
            command.add(chart);
            command.add("-n");
            command.add(ns);

            if (version != null && !version.isBlank()) {
                command.add("--version");
                command.add(version.trim());
            }

            if (values != null && !values.isBlank()) {
                command.add("--set-json");
                command.add(values.trim());
            }

            if ("true".equalsIgnoreCase(install)) {
                command.add("--install");
            }

            addContextFlag(command);

            ProcessRunner.ProcessResult result = processRunner.run(command, 120);
            return result.isSuccess()
                    ? "Helm upgrade successful:\n" + result.stdout()
                    : "Helm upgrade failed:\n" + result.output();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    @Tool(name = "helm_uninstall", description = "Uninstall a Helm release. WARNING: This is a destructive operation. Disabled in non-destructive mode.")
    public String helmUninstall(
            @ToolParam(description = "Release name to uninstall") String releaseName,
            @ToolParam(description = "Optional: namespace.", required = false) String namespace
    ) {
        if (properties.isNonDestructiveMode()) {
            return "Error: helm_uninstall is disabled in non-destructive mode.";
        }
        try {
            String ns = (namespace == null || namespace.isBlank()) ? "default" : namespace.trim();

            List<String> command = new ArrayList<>();
            command.add("helm");
            command.add("uninstall");
            command.add(releaseName);
            command.add("-n");
            command.add(ns);

            addContextFlag(command);

            ProcessRunner.ProcessResult result = processRunner.run(command, 60);
            return result.isSuccess()
                    ? "Helm uninstall successful:\n" + result.stdout()
                    : "Helm uninstall failed:\n" + result.output();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    @Tool(name = "helm_list", description = "List Helm releases in a namespace or all namespaces.")
    public String helmList(
            @ToolParam(description = "Optional: namespace. Use '_all' for all namespaces.", required = false) String namespace
    ) {
        try {
            List<String> command = new ArrayList<>();
            command.add("helm");
            command.add("list");

            if ("_all".equals(namespace)) {
                command.add("--all-namespaces");
            } else {
                String ns = (namespace == null || namespace.isBlank()) ? "default" : namespace.trim();
                command.add("-n");
                command.add(ns);
            }

            command.add("-o");
            command.add("json");

            addContextFlag(command);

            ProcessRunner.ProcessResult result = processRunner.run(command);
            return result.isSuccess() ? result.stdout() : "Error listing Helm releases: " + result.output();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    private void addHelmRepo(String name, String url) {
        processRunner.run(List.of("helm", "repo", "add", name, url));
        processRunner.run(List.of("helm", "repo", "update"));
    }

    private void addContextFlag(List<String> command) {
        var currentContext = client.getConfiguration().getCurrentContext();
        if (currentContext != null) {
            command.add("--kube-context");
            command.add(currentContext.getName());
        }
    }
}
