# Kubernetes MCP Server (Spring Boot)

[![Java](https://img.shields.io/badge/Java-21+-blue?logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4-green?logo=springboot)](https://spring.io/projects/spring-boot)
[![Kubernetes](https://img.shields.io/badge/kubernetes-%23326ce5.svg?style=flat&logo=kubernetes&logoColor=white)](https://kubernetes.io/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

A **Spring Boot MCP Server** for Kubernetes cluster management — built with the Spring AI MCP SDK and Fabric8 Kubernetes Java client.

Exposes Kubernetes management operations as [Model Context Protocol (MCP)](https://modelcontextprotocol.io/) tools, allowing AI assistants (Claude, Cursor, VS Code Copilot, etc.) to interact with Kubernetes clusters programmatically.

> Inspired by [flux159/mcp-server-kubernetes](https://github.com/flux159/mcp-server-kubernetes) — reimplemented in Java/Spring Boot with native Kubernetes API client (no kubectl dependency for most operations).

---

## Prerequisites

1. **Java 21+** installed
2. **Maven** installed  
3. A valid **kubeconfig** file (`~/.kube/config` or `KUBECONFIG` env variable)
4. Access to a Kubernetes cluster (minikube, kind, GKE, EKS, AKS, etc.)
5. **Helm v3** installed (optional — only needed for Helm operations)
6. **kubectl** installed (optional — only needed for `explain_resource`, `kubectl_describe`, `kubectl_patch`, `kubectl_rollout`)

Verify your cluster connectivity:
```bash
kubectl get pods
```

---

## Quick Start

### Build
```bash
mvn clean package -DskipTests
```

### Run
```bash
java -jar target/kubernetes-mcp-server-1.0.0-SNAPSHOT.jar
```

The server starts in **STDIO mode** — it reads MCP JSON-RPC messages from stdin and writes responses to stdout.

---

## Client Configuration

### Claude Desktop
Add to your Claude Desktop config (`claude_desktop_config.json`):
```json
{
  "mcpServers": {
    "kubernetes": {
      "command": "java",
      "args": ["-jar", "/path/to/kubernetes-mcp-server-1.0.0-SNAPSHOT.jar"]
    }
  }
}
```

### Claude Code
```bash
claude mcp add kubernetes -- java -jar /path/to/kubernetes-mcp-server-1.0.0-SNAPSHOT.jar
```

### VS Code / Cursor
```json
{
  "mcpServers": {
    "kubernetes": {
      "command": "java",
      "args": ["-jar", "/path/to/kubernetes-mcp-server-1.0.0-SNAPSHOT.jar"],
      "description": "Kubernetes cluster management and operations"
    }
  }
}
```

### Non-Destructive Mode
Disable all destructive operations (delete, uninstall, drain, cleanup):
```json
{
  "mcpServers": {
    "kubernetes-readonly": {
      "command": "java",
      "args": ["-jar", "/path/to/kubernetes-mcp-server-1.0.0-SNAPSHOT.jar"],
      "env": {
        "ALLOW_ONLY_NON_DESTRUCTIVE_TOOLS": "true"
      }
    }
  }
}
```

---

## Features

### Core Tools (16 MCP Tools)

| Tool | Description | Non-Destructive |
|------|-------------|:---:|
| `ping` | Verify cluster connectivity | ✅ |
| `kubectl_get` | Get/list any K8s resource | ✅ |
| `kubectl_describe` | Describe a resource in detail | ✅ |
| `kubectl_create` | Create a resource from YAML/JSON | ✅ |
| `kubectl_apply` | Apply a YAML manifest (server-side apply) | ✅ |
| `kubectl_delete` | Delete a resource | ❌ |
| `kubectl_logs` | Get pod logs | ✅ |
| `kubectl_scale` | Scale deployments/statefulsets/replicasets | ✅ |
| `kubectl_patch` | Patch a resource | ✅ |
| `kubectl_rollout` | Rollout status/history/restart/undo | ✅ |
| `kubectl_context` | List/switch kubectl contexts | ✅ |
| `explain_resource` | Explain K8s resource types/fields | ✅ |
| `list_api_resources` | List available API resources | ✅ |
| `port_forward` | Port-forward to pods/services | ✅ |
| `helm_install` | Install a Helm chart | ✅ |
| `helm_upgrade` | Upgrade a Helm release | ✅ |
| `helm_uninstall` | Uninstall a Helm release | ❌ |
| `helm_list` | List Helm releases | ✅ |
| `cleanup_pods` | Clean up pods in error states | ❌ |
| `node_management` | Cordon/uncordon/drain nodes | ❌ |

### Security Features

- **Secrets Masking**: Sensitive data in `kubectl_get secrets` output is automatically masked (`***MASKED***`). Disable with `MASK_SECRETS=false`.
- **Non-Destructive Mode**: Set `ALLOW_ONLY_NON_DESTRUCTIVE_TOOLS=true` to disable all destructive operations.

---

## Architecture

```
┌─────────────────────────────────────────────────┐
│                 MCP Client                       │
│         (Claude / Cursor / VS Code)              │
└─────────────────┬───────────────────────────────┘
                  │ STDIO (JSON-RPC)
┌─────────────────▼───────────────────────────────┐
│           Spring Boot MCP Server                 │
│  ┌──────────────────────────────────────────┐   │
│  │     Spring AI MCP STDIO Transport         │   │
│  └──────────────────┬───────────────────────┘   │
│  ┌──────────────────▼───────────────────────┐   │
│  │         @Tool Service Beans               │   │
│  │  ┌─────────┐ ┌─────────┐ ┌───────────┐  │   │
│  │  │kubectl_ │ │  helm_  │ │  node_    │  │   │
│  │  │get/logs │ │install/ │ │management │  │   │
│  │  │scale/.. │ │upgrade  │ │/cleanup   │  │   │
│  │  └────┬────┘ └────┬────┘ └─────┬─────┘  │   │
│  └───────┼───────────┼────────────┼─────────┘   │
│  ┌───────▼───────────▼────────────▼─────────┐   │
│  │        Fabric8 K8s Client / CLI           │   │
│  └──────────────────┬───────────────────────┘   │
└─────────────────────┼───────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────┐
│            Kubernetes API Server                 │
└─────────────────────────────────────────────────┘
```

---

## Configuration

| Property | Env Variable | Default | Description |
|----------|-------------|---------|-------------|
| `k8s.mcp.non-destructive-mode` | `ALLOW_ONLY_NON_DESTRUCTIVE_TOOLS` | `false` | Disable destructive tools |
| `k8s.mcp.secrets-masking` | `MASK_SECRETS` | `true` | Mask Secret data in output |
| `k8s.mcp.kubeconfig-path` | `KUBECONFIG` | `~/.kube/config` | Custom kubeconfig path |

---

## Development

```bash
# Build
mvn clean package

# Run
mvn spring-boot:run

# Run tests
mvn test
```

---

## License

MIT
