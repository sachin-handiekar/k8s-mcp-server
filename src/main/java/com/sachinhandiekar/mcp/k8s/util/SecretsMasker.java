package com.sachinhandiekar.mcp.k8s.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.util.Iterator;

/**
 * Masks sensitive data in Kubernetes Secret resources.
 *
 * <p>When secrets masking is enabled, the {@code data} and {@code stringData}
 * fields of Secret resources are replaced with {@code "***MASKED***"} in all
 * tool output to prevent accidental exposure of credentials.</p>
 */
@Component
public class SecretsMasker {

    private static final String MASKED_VALUE = "***MASKED***";
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Masks secret data in JSON output if the resource is a Secret.
     *
     * @param json     the JSON string output from a K8s API call
     * @param isMasked whether masking is enabled
     * @return the (potentially masked) JSON string
     */
    public String maskIfSecret(String json, boolean isMasked) {
        if (!isMasked || json == null || json.isBlank()) {
            return json;
        }
        try {
            JsonNode root = objectMapper.readTree(json);
            if (isSecret(root)) {
                maskSecretNode(root);
            } else if (isSecretList(root)) {
                JsonNode items = root.get("items");
                if (items != null && items.isArray()) {
                    for (JsonNode item : items) {
                        if (isSecret(item)) {
                            maskSecretNode(item);
                        }
                    }
                }
            }
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
        } catch (JsonProcessingException e) {
            // If we can't parse it, return the original
            return json;
        }
    }

    private boolean isSecret(JsonNode node) {
        JsonNode kind = node.get("kind");
        return kind != null && "Secret".equals(kind.asText());
    }

    private boolean isSecretList(JsonNode node) {
        JsonNode kind = node.get("kind");
        return kind != null && "SecretList".equals(kind.asText());
    }

    private void maskSecretNode(JsonNode node) {
        maskField((ObjectNode) node, "data");
        maskField((ObjectNode) node, "stringData");
    }

    private void maskField(ObjectNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        if (field != null && field.isObject()) {
            ObjectNode dataNode = (ObjectNode) field;
            Iterator<String> fieldNames = dataNode.fieldNames();
            while (fieldNames.hasNext()) {
                String key = fieldNames.next();
                dataNode.put(key, MASKED_VALUE);
            }
        }
    }
}
