package com.sachinhandiekar.mcp.k8s.config;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures the Fabric8 {@link KubernetesClient} bean.
 *
 * <p>Kubeconfig resolution priority:
 * <ol>
 *   <li>{@code KUBECONFIG} environment variable (custom path)</li>
 *   <li>{@code ~/.kube/config} (default)</li>
 *   <li>In-cluster service account (when running inside K8s)</li>
 * </ol>
 */
@Configuration
public class KubernetesClientConfig {

    private static final Logger log = LoggerFactory.getLogger(KubernetesClientConfig.class);

    @Value("${k8s.mcp.kubeconfig-path:}")
    private String kubeconfigPath;

    @Bean
    public KubernetesClient kubernetesClient() {
        Config config;
        if (kubeconfigPath != null && !kubeconfigPath.isBlank()) {
            log.info("Loading kubeconfig from custom path: {}", kubeconfigPath);
            System.setProperty("kubeconfig", kubeconfigPath);
            config = new ConfigBuilder().build();
        } else {
            log.info("Loading kubeconfig from default location");
            config = new ConfigBuilder().build();
        }
        return new KubernetesClientBuilder()
                .withConfig(config)
                .build();
    }
}
