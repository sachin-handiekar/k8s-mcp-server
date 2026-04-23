package com.sachinhandiekar.mcp.k8s;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Kubernetes MCP Server — Spring Boot application.
 *
 * <p>Exposes Kubernetes management operations as MCP tools over STDIO transport,
 * allowing AI assistants (Claude, Cursor, VS Code, etc.) to interact with
 * Kubernetes clusters programmatically.</p>
 */
@SpringBootApplication
public class KubernetesMcpServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(KubernetesMcpServerApplication.class, args);
    }
}
