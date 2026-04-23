package com.sachinhandiekar.mcp.k8s.tools;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * MCP Tool: Clean up problematic pods.
 *
 * <p><strong>Disabled in non-destructive mode.</strong></p>
 *
 * <p>Identifies and deletes pods in error states: Evicted, ContainerStatusUnknown,
 * Completed, Error, ImagePullBackOff, CrashLoopBackOff.</p>
 */
@Service
@ConditionalOnProperty(name = "k8s.mcp.non-destructive-mode", havingValue = "false", matchIfMissing = true)
public class CleanupPodsTool {

    private static final Set<String> PROBLEM_REASONS = Set.of(
            "Evicted", "ContainerStatusUnknown", "Error", "Completed",
            "ImagePullBackOff", "CrashLoopBackOff", "ErrImagePull"
    );

    private final KubernetesClient client;

    public CleanupPodsTool(KubernetesClient client) {
        this.client = client;
    }

    @Tool(name = "cleanup_pods", description = "Clean up pods in problematic states: Evicted, ContainerStatusUnknown, Completed, Error, ImagePullBackOff, CrashLoopBackOff. WARNING: Destructive operation — disabled in non-destructive mode.")
    public String cleanupPods(
            @ToolParam(description = "Optional: namespace. If omitted, cleans across all namespaces.", required = false) String namespace,
            @ToolParam(description = "Optional: if 'true', only lists problematic pods without deleting them (dry run).", required = false) String dryRun
    ) {
        try {
            boolean isDryRun = "true".equalsIgnoreCase(dryRun);

            PodList podList;
            if (namespace == null || namespace.isBlank() || "_all".equals(namespace)) {
                podList = client.pods().inAnyNamespace().list();
            } else {
                podList = client.pods().inNamespace(namespace.trim()).list();
            }

            List<Pod> problemPods = new ArrayList<>();
            for (Pod pod : podList.getItems()) {
                if (isProblemPod(pod)) {
                    problemPods.add(pod);
                }
            }

            if (problemPods.isEmpty()) {
                return "No problematic pods found.";
            }

            StringBuilder result = new StringBuilder();
            result.append(String.format("Found %d problematic pod(s):%n", problemPods.size()));

            for (Pod pod : problemPods) {
                String podNs = pod.getMetadata().getNamespace();
                String podName = pod.getMetadata().getName();
                String reason = getPodReason(pod);

                if (isDryRun) {
                    result.append(String.format("  [DRY RUN] Would delete %s/%s (reason: %s)%n", podNs, podName, reason));
                } else {
                    client.pods().inNamespace(podNs).withName(podName).delete();
                    result.append(String.format("  Deleted %s/%s (reason: %s)%n", podNs, podName, reason));
                }
            }

            return result.toString().trim();
        } catch (Exception e) {
            return "Error cleaning up pods: " + e.getMessage();
        }
    }

    private boolean isProblemPod(Pod pod) {
        String reason = getPodReason(pod);
        return PROBLEM_REASONS.contains(reason);
    }

    private String getPodReason(Pod pod) {
        // Check pod status reason
        if (pod.getStatus() != null && pod.getStatus().getReason() != null) {
            return pod.getStatus().getReason();
        }

        // Check container statuses for waiting reasons
        if (pod.getStatus() != null && pod.getStatus().getContainerStatuses() != null) {
            for (var cs : pod.getStatus().getContainerStatuses()) {
                if (cs.getState() != null && cs.getState().getWaiting() != null) {
                    String waitReason = cs.getState().getWaiting().getReason();
                    if (waitReason != null && PROBLEM_REASONS.contains(waitReason)) {
                        return waitReason;
                    }
                }
                if (cs.getState() != null && cs.getState().getTerminated() != null) {
                    String termReason = cs.getState().getTerminated().getReason();
                    if (termReason != null && PROBLEM_REASONS.contains(termReason)) {
                        return termReason;
                    }
                }
            }
        }

        // Check phase
        if (pod.getStatus() != null) {
            String phase = pod.getStatus().getPhase();
            if ("Failed".equals(phase)) return "Error";
            if ("Succeeded".equals(phase)) return "Completed";
        }

        return "Unknown";
    }
}
