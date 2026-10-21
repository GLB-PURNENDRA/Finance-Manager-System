package com.financemanager.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.financemanager.entity.AiConversationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

/**
 * Robust Client Service for Google Gemini API (v1beta REST API).
 * Communicates with the official Generative Language API using supported models (gemini-3.8-flash).
 * Securely handles credentials, sanitizes logging, and parses fine-grained error categories.
 */
@Service
public class GeminiClientService {

    private static final Logger log = LoggerFactory.getLogger(GeminiClientService.class);

    public static final String DEFAULT_SUPPORTED_MODEL = "gemini-3.5-flash-lite";

    @Value("${gemini.api.key:}")
    private String apiKey;

    @Value("${gemini.api.model:gemini-3.5-flash-lite}")
    private String model;

    @Value("${gemini.api.timeout-seconds:30}")
    private int timeoutSeconds;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    public static class ProviderResult {
        private final boolean success;
        private final String content;
        private final int httpStatusCode;
        private final String errorCategory;
        private final String errorMessage;

        public ProviderResult(boolean success, String content, int httpStatusCode, String errorCategory, String errorMessage) {
            this.success = success;
            this.content = content;
            this.httpStatusCode = httpStatusCode;
            this.errorCategory = errorCategory;
            this.errorMessage = errorMessage;
        }

        public static ProviderResult success(String content) {
            return new ProviderResult(true, content, 200, null, null);
        }

        public static ProviderResult missingCredentials(String message) {
            return new ProviderResult(false, null, 0, "MISSING_CREDENTIALS", message);
        }

        public static ProviderResult error(int statusCode, String category, String message) {
            return new ProviderResult(false, null, statusCode, category, message);
        }

        public boolean isSuccess() { return success; }
        public String getContent() { return content; }
        public int getHttpStatusCode() { return httpStatusCode; }
        public String getErrorCategory() { return errorCategory; }
        public String getErrorMessage() { return errorMessage; }
    }

    /**
     * Resolves the active API key with fallback to OS environment variable GEMINI_API_KEY.
     * Never exposes the key string outside this service.
     */
    private String resolveApiKey() {
        if ("none".equalsIgnoreCase(apiKey)) {
            return "";
        }
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            return apiKey.trim();
        }
        String envKey = System.getenv("GEMINI_API_KEY");
        if ("none".equalsIgnoreCase(envKey)) {
            return "";
        }
        if (envKey != null && !envKey.trim().isEmpty()) {
            return envKey.trim();
        }
        String propKey = System.getProperty("GEMINI_API_KEY");
        if ("none".equalsIgnoreCase(propKey)) {
            return "";
        }
        if (propKey != null && !propKey.trim().isEmpty()) {
            return propKey.trim();
        }
        // Fallback for Windows environment: read from HKCU\Environment if not inherited
        try {
            Process process = new ProcessBuilder("cmd.exe", "/c", "reg query HKCU\\Environment /v GEMINI_API_KEY")
                    .redirectErrorStream(true)
                    .start();
            String output = new String(process.getInputStream().readAllBytes());
            process.waitFor();
            for (String line : output.split("\n")) {
                if (line.contains("GEMINI_API_KEY")) {
                    String[] tokens = line.trim().split("\\s+");
                    if (tokens.length >= 3) {
                        String key = tokens[2].trim();
                        if (!key.isEmpty() && !key.equalsIgnoreCase("none")) {
                            return key;
                        }
                    }
                }
            }
        } catch (Exception ignored) {}

        return "";
    }

    public boolean isConfigured() {
        return !resolveApiKey().isEmpty();
    }

    public synchronized void setApiKey(String key) {
        this.apiKey = key != null ? key.trim() : "";
        log.info("[AI Provider Config] API key updated in server memory. isConfigured={}", isConfigured());
    }

    public synchronized void setModel(String model) {
        if (model != null && !model.isBlank()) {
            this.model = model.trim();
            log.info("[AI Provider Config] Requested model set to '{}'", this.model);
        }
    }

    /**
     * Returns the verified supported Gemini model name.
     * Automatically maps legacy/deprecated model codes to the current supported model.
     */
    public String getModelName() {
        String m = (model != null && !model.isBlank()) ? model.trim() : DEFAULT_SUPPORTED_MODEL;

        // Upgrade deprecated or legacy identifiers
        if (m.equalsIgnoreCase("gemini-2.5-flash") ||
            m.equalsIgnoreCase("gemini-2.0-flash") ||
            m.equalsIgnoreCase("gemini-1.5-flash") ||
            m.equalsIgnoreCase("gemini-1.5-pro") ||
            m.equalsIgnoreCase("gemini-pro")) {
            log.info("[AI Provider Model] Legacy model '{}' mapped to currently supported '{}'", m, DEFAULT_SUPPORTED_MODEL);
            return DEFAULT_SUPPORTED_MODEL;
        }

        return m;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds > 0 ? timeoutSeconds : 40;
    }

    /**
     * Dispatches chat prompt and context to the Google Gemini API.
     */
    public ProviderResult generateChatResponse(String systemPrompt, List<AiConversationMessage> history, String userPrompt) {
        String activeKey = resolveApiKey();
        String requestId = "REQ-" + java.util.UUID.randomUUID().toString().substring(0, 8);
        if (activeKey.isEmpty()) {
            log.info("[AI Diagnostics] requestId={}, provider='Google Gemini', status='MISSING_CREDENTIALS', message='GEMINI_API_KEY is not configured'", requestId);
            return ProviderResult.missingCredentials(
                    "GEMINI_API_KEY environment variable is not configured on the server.");
        }

        long startTime = System.currentTimeMillis();
        String activeModel = getModelName();
        log.info("[AI Diagnostics] requestId={}, provider='Google Gemini', model='{}', promptLen={}", requestId, activeModel, userPrompt.length());

        try {
            String endpoint = String.format("https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent", activeModel);

            ObjectNode root = objectMapper.createObjectNode();

            // 1. System Instruction
            if (systemPrompt != null && !systemPrompt.isBlank()) {
                ObjectNode sysNode = root.putObject("system_instruction");
                ArrayNode sysParts = sysNode.putArray("parts");
                sysParts.addObject().put("text", systemPrompt);
            }

            // 2. Contents array (Strictly alternating multi-turn conversation history + new user prompt)
            ArrayNode contents = root.putArray("contents");

            if (history != null && !history.isEmpty()) {
                java.util.List<AiConversationMessage> validHistory = new java.util.ArrayList<>();
                String expectedRole = "user";
                for (AiConversationMessage msg : history) {
                    String role = "user".equalsIgnoreCase(msg.getRole()) ? "user" : "model";
                    if (role.equals(expectedRole)) {
                        validHistory.add(msg);
                        expectedRole = "user".equals(role) ? "model" : "user";
                    }
                }
                // Ensure history ends with a model message so next turn is user
                if (!validHistory.isEmpty() && "user".equalsIgnoreCase(validHistory.get(validHistory.size() - 1).getRole())) {
                    validHistory.remove(validHistory.size() - 1);
                }

                // Keep last 6 alternating turns (3 user-model pairs) for optimal latency & context
                int startIdx = Math.max(0, validHistory.size() - 6);
                for (int i = startIdx; i < validHistory.size(); i++) {
                    AiConversationMessage msg = validHistory.get(i);
                    String role = "user".equalsIgnoreCase(msg.getRole()) ? "user" : "model";
                    ObjectNode contentNode = contents.addObject();
                    contentNode.put("role", role);
                    ArrayNode parts = contentNode.putArray("parts");
                    String contentText = msg.getContent() != null ? msg.getContent() : "";
                    if ("model".equals(role) && contentText.length() > 600) {
                        contentText = contentText.substring(0, 600) + "...";
                    }
                    parts.addObject().put("text", contentText);
                }
            }

            // Current user turn
            ObjectNode curTurn = contents.addObject();
            curTurn.put("role", "user");
            ArrayNode curParts = curTurn.putArray("parts");
            curParts.addObject().put("text", userPrompt);

            // 3. Generation Config
            ObjectNode genConfig = root.putObject("generationConfig");
            genConfig.put("temperature", 0.4);
            genConfig.put("maxOutputTokens", 1000);

            String requestBody = objectMapper.writeValueAsString(root);
            int callTimeout = getTimeoutSeconds();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("x-goog-api-key", activeKey)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(callTimeout))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            long durationMs = System.currentTimeMillis() - startTime;
            int statusCode = response.statusCode();

            log.info("[AI Diagnostics] requestId={}, provider='Google Gemini', httpStatus={}, latencyMs={}", requestId, statusCode, durationMs);

            if (statusCode == 200) {
                JsonNode resJson = objectMapper.readTree(response.body());
                JsonNode candidates = resJson.path("candidates");
                if (candidates.isArray() && candidates.size() > 0) {
                    JsonNode firstCandidate = candidates.get(0);
                    JsonNode parts = firstCandidate.path("content").path("parts");
                    if (parts.isArray() && parts.size() > 0) {
                        String text = parts.get(0).path("text").asText();
                        if (text != null && !text.isBlank()) {
                            return ProviderResult.success(text.trim());
                        }
                    }
                }
                log.warn("[AI Diagnostics] requestId={}, provider='Google Gemini', httpStatus=200, errorCategory='EMPTY_RESPONSE'", requestId);
                return ProviderResult.error(200, "EMPTY_RESPONSE", "Provider returned 200 OK but response contained empty candidate text.");
            }

            // Extract Google error payload details
            String googleErrorMessage = "";
            String googleErrorStatus = "";
            try {
                JsonNode errJson = objectMapper.readTree(response.body());
                JsonNode errObj = errJson.path("error");
                if (!errObj.isMissingNode()) {
                    googleErrorMessage = errObj.path("message").asText("");
                    googleErrorStatus = errObj.path("status").asText("");
                }
            } catch (Exception ignored) {}

            String sanitizedErrorMsg = googleErrorMessage.replaceAll("(?i)(api[_-]?key=)[^&\\s]+", "$1***");

            if (statusCode == 400 || statusCode == 403) {
                log.warn("[AI Diagnostics] requestId={}, provider='Google Gemini', httpStatus={}, errorCategory='INVALID_KEY', latencyMs={}, message='{}'",
                        requestId, statusCode, durationMs, sanitizedErrorMsg);
                String msg = sanitizedErrorMsg.isEmpty()
                        ? "Google Gemini API rejected credentials (HTTP " + statusCode + "). Please verify your GEMINI_API_KEY validity."
                        : sanitizedErrorMsg;
                return ProviderResult.error(statusCode, "INVALID_KEY", msg);
            } else if (statusCode == 429) {
                log.warn("[AI Diagnostics] requestId={}, provider='Google Gemini', httpStatus=429, errorCategory='QUOTA_EXCEEDED', latencyMs={}", requestId, durationMs);
                String msg = sanitizedErrorMsg.isEmpty()
                        ? "Google Gemini API rate limit or quota exceeded (HTTP 429). Please wait a moment or check your quota in Google AI Studio."
                        : sanitizedErrorMsg;
                return ProviderResult.error(429, "QUOTA_EXCEEDED", msg);
            } else if (statusCode == 404) {
                log.warn("[AI Diagnostics] requestId={}, provider='Google Gemini', httpStatus=404, errorCategory='MODEL_NOT_FOUND', model='{}', latencyMs={}", requestId, activeModel, durationMs);
                return ProviderResult.error(404, "MODEL_NOT_FOUND",
                        "The model '" + activeModel + "' was not found or is unavailable. Please use '" + DEFAULT_SUPPORTED_MODEL + "'.");
            } else if (statusCode >= 500) {
                log.warn("[AI Diagnostics] requestId={}, provider='Google Gemini', httpStatus={}, errorCategory='UPSTREAM_SERVER_ERROR', latencyMs={}", requestId, statusCode, durationMs);
                return ProviderResult.error(statusCode, "UPSTREAM_SERVER_ERROR",
                        "Google Gemini upstream server error (HTTP " + statusCode + "). Please retry shortly.");
            } else {
                log.warn("[AI Diagnostics] requestId={}, provider='Google Gemini', httpStatus={}, errorCategory='UNEXPECTED_STATUS', latencyMs={}", requestId, statusCode, durationMs);
                return ProviderResult.error(statusCode, "UNEXPECTED_STATUS",
                        "Gemini API returned unexpected HTTP status: " + statusCode);
            }
        } catch (java.net.http.HttpTimeoutException ex) {
            long durationMs = System.currentTimeMillis() - startTime;
            log.warn("[AI Diagnostics] requestId={}, provider='Google Gemini', httpStatus=504, errorCategory='TIMEOUT', latencyMs={}", requestId, durationMs);
            return ProviderResult.error(504, "TIMEOUT",
                    String.format("Google Gemini API request timed out after %d seconds.", getTimeoutSeconds()));
        } catch (Exception ex) {
            long durationMs = System.currentTimeMillis() - startTime;
            log.error("[AI Diagnostics] requestId={}, provider='Google Gemini', httpStatus=500, errorCategory='CLIENT_EXCEPTION', latencyMs={}, message='{}'",
                    requestId, durationMs, ex.getMessage());
            return ProviderResult.error(500, "CLIENT_EXCEPTION", "Failed to communicate with AI provider: " + ex.getMessage());
        }
    }
}
