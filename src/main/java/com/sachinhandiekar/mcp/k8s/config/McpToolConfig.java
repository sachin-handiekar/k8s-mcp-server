package com.sachinhandiekar.mcp.k8s.config;

import com.sachinhandiekar.mcp.k8s.tools.*;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers all MCP tool beans with the Spring AI MCP Server.
 *
 * <p>Spring AI's MCP STDIO server requires tools to be explicitly provided
 * via a {@link ToolCallbackProvider} bean. This configuration collects all
 * {@code @Tool}-annotated service beans and registers them so the MCP server
 * exposes them to clients.</p>
 */
@Configuration
public class McpToolConfig {

    @Bean
    public ToolCallbackProvider kubernetesToolCallbackProvider(
            PingTool pingTool,
            KubectlGetTool kubectlGetTool,
            KubectlDescribeTool kubectlDescribeTool,
            KubectlLogsTool kubectlLogsTool,
            KubectlApplyTool kubectlApplyTool,
            KubectlCreateTool kubectlCreateTool,
            KubectlDeleteTool kubectlDeleteTool,
            KubectlPatchTool kubectlPatchTool,
            KubectlScaleTool kubectlScaleTool,
            KubectlRolloutTool kubectlRolloutTool,
            KubectlContextTool kubectlContextTool,
            CleanupPodsTool cleanupPodsTool,
            ExplainResourceTool explainResourceTool,
            HelmTool helmTool,
            ListApiResourcesTool listApiResourcesTool,
            NodeManagementTool nodeManagementTool,
            PortForwardTool portForwardTool
    ) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(
                        pingTool,
                        kubectlGetTool,
                        kubectlDescribeTool,
                        kubectlLogsTool,
                        kubectlApplyTool,
                        kubectlCreateTool,
                        kubectlDeleteTool,
                        kubectlPatchTool,
                        kubectlScaleTool,
                        kubectlRolloutTool,
                        kubectlContextTool,
                        cleanupPodsTool,
                        explainResourceTool,
                        helmTool,
                        listApiResourcesTool,
                        nodeManagementTool,
                        portForwardTool
                )
                .build();
    }
}
