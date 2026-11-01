package com.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Calls OpenRouter (https://openrouter.ai) with the server's API key, so the key never reaches the browser.
 */
@Component
public class OpenRouterClient {
    private static final Logger log = LoggerFactory.getLogger(OpenRouterClient.class);
    private static final String API = "https://openrouter.ai/api/v1";
    private static final String FREE_ROUTER = "openrouter/free";
    private static final Duration MODELS_TTL = Duration.ofMinutes(10);
    private static final Pattern SPECIALIST = Pattern.compile("safety|code|guard", Pattern.CASE_INSENSITIVE);

    public record ModelInfo(String id, String name, boolean free, boolean vision) {}

    private final RestClient http;
    private final JsonMapper json;
    private final String apiKey;
    private final String freeModel;
    private final String premiumModel;
    private final String referer;

    private volatile List<ModelInfo> cachedModels = List.of();
    private volatile Instant cachedAt = Instant.EPOCH;

    public OpenRouterClient(
            JsonMapper json,
            @Value("${openrouter.api-key:}") String apiKey,
            @Value("${openrouter.model.free:openrouter/free}") String freeModel,
            @Value("${openrouter.model.premium:${openrouter.model.free:openrouter/free}}") String premiumModel,
            @Value("${openrouter.referer:http://localhost:8080}") String referer) {
        JdkClientHttpRequestFactory requests = new JdkClientHttpRequestFactory();
        // Free models can take a while to write a batch of replies.
        requests.setReadTimeout(Duration.ofSeconds(120));
        this.http = RestClient.builder().baseUrl(API).requestFactory(requests).build();
        this.json = json;
        this.apiKey = apiKey.trim();
        this.freeModel = freeModel.trim();
        this.premiumModel = premiumModel.trim();
        this.referer = referer;
    }

    public boolean isConfigured() {
        return !apiKey.isEmpty();
    }

    /** The server picks the model, so the cost of each plan is predictable. */
    public String modelFor(Plan plan) {
        return plan == Plan.PREMIUM ? premiumModel : freeModel;
    }

    /** Text models on OpenRouter, cached for a few minutes. */
    public List<ModelInfo> models() {
        if (Duration.between(cachedAt, Instant.now()).compareTo(MODELS_TTL) > 0) {
            try {
                cachedModels = fetchModels();
                cachedAt = Instant.now();
            } catch (RuntimeException e) {
                log.warn("Could not load OpenRouter models: {}", e.getMessage());
            }
        }
        return cachedModels;
    }

    /**
     * The model to actually use. Free models come and go on OpenRouter, so fall back to one that exists.
     * Returns empty when the model list can't be loaded, in which case the requested model is tried as is.
     */
    public Optional<ModelInfo> resolve(String requested) {
        List<ModelInfo> available = models();
        if (available.isEmpty()) return Optional.empty();
        return available.stream().filter(m -> m.id().equals(requested)).findFirst()
                .or(() -> available.stream().filter(m -> m.id().equals(FREE_ROUTER)).findFirst())
                // openrouter/free routes to whichever free model is up; skip specialist models otherwise.
                .or(() -> available.stream().filter(m -> m.free() && !SPECIALIST.matcher(m.id()).find()).findFirst());
    }

    /** Sends one chat completion and returns the model's text. */
    public String chat(String model, String system, Object userContent) {
        if (!isConfigured()) {
            throw new IllegalStateException("The server has no OpenRouter API key. Set OPENROUTER_API_KEY in .env.");
        }
        Map<String, Object> body = Map.of(
                "model", model,
                "temperature", 1,
                "messages", List.of(
                        Map.of("role", "system", "content", system),
                        Map.of("role", "user", "content", userContent)));
        String raw;
        try {
            raw = http.post().uri("/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("HTTP-Referer", referer)
                    .header("X-Title", "BubbleDiary")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(json.writeValueAsString(body))
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException e) {
            String msg = errorMessage(e.getResponseBodyAsString())
                    .orElse("Request failed (" + e.getStatusCode().value() + ")");
            if (e.getStatusCode().value() == 429) {
                msg = "Rate limited by the free model. Wait a minute and retry. (" + msg + ")";
            }
            throw new IllegalStateException(msg, e);
        }
        JsonNode response = json.readTree(raw);
        Optional<String> error = errorMessage(raw);
        if (error.isPresent()) throw new IllegalStateException(error.get());
        return response.path("choices").path(0).path("message").path("content").asString("");
    }

    private Optional<String> errorMessage(String body) {
        try {
            String msg = json.readTree(body).path("error").path("message").asString("");
            return msg.isBlank() ? Optional.empty() : Optional.of(msg);
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    private List<ModelInfo> fetchModels() {
        JsonNode data = json.readTree(http.get().uri("/models").retrieve().body(String.class)).path("data");
        List<ModelInfo> models = new ArrayList<>();
        for (JsonNode m : data) {
            JsonNode arch = m.path("architecture");
            if (arch.has("output_modalities") && !contains(arch.path("output_modalities"), "text")) continue;
            String id = m.path("id").asString();
            boolean free = id.endsWith(":free")
                    || (isZero(m.path("pricing").path("prompt")) && isZero(m.path("pricing").path("completion")));
            models.add(new ModelInfo(id, m.path("name").asString(id), free,
                    contains(arch.path("input_modalities"), "image")));
        }
        models.sort(Comparator.comparing(ModelInfo::name, String.CASE_INSENSITIVE_ORDER));
        return List.copyOf(models);
    }

    private static boolean contains(JsonNode array, String value) {
        for (JsonNode n : array) {
            if (value.equals(n.asString())) return true;
        }
        return false;
    }

    private static boolean isZero(JsonNode price) {
        try {
            return Double.parseDouble(price.asString("")) == 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
