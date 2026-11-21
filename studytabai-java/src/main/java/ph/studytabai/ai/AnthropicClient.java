package ph.studytabai.ai;

import ph.studytabai.util.Json;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/** Calls the Anthropic Messages API. The key is read from ANTHROPIC_API_KEY and never logged or shown. */
public final class AnthropicClient implements AiClient {
    private static final URI ENDPOINT = URI.create("https://api.anthropic.com/v1/messages");
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
    private final String apiKey;
    private final String model;

    public AnthropicClient(String apiKey, String model) {
        this.apiKey = apiKey;
        this.model = model;
    }

    public static boolean isConfigured() {
        String k = System.getenv("ANTHROPIC_API_KEY");
        return k != null && !k.isBlank();
    }

    public static AnthropicClient fromEnv() throws AiException {
        String key = System.getenv("ANTHROPIC_API_KEY");
        if (key == null || key.isBlank())
            throw new AiException("AI is not set up. Set the ANTHROPIC_API_KEY environment variable and restart the app.");
        String model = System.getenv("ANTHROPIC_MODEL");
        return new AnthropicClient(key.trim(), model == null || model.isBlank() ? "claude-sonnet-5-5" : model.trim());
    }

    @Override
    public String complete(String systemPrompt, String userMessage) throws AiException {
        String body = Json.write(Map.of(
                "model", model,
                "max_tokens", 4096,
                "system", systemPrompt,
                "messages", List.of(Map.of("role", "user", "content", userMessage))));
        HttpRequest req = HttpRequest.newBuilder(ENDPOINT)
                .timeout(Duration.ofSeconds(90))
                .header("content-type", "application/json")
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        try {
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() == 401) throw new AiException("The AI key was rejected. Check ANTHROPIC_API_KEY.");
            if (res.statusCode() == 429) throw new AiException("The AI service is busy. Please try again shortly.");
            if (res.statusCode() != 200) throw new AiException("The AI service returned an error (HTTP " + res.statusCode() + ").");
            StringBuilder out = new StringBuilder();
            for (Object block : Json.asList(Json.asMap(Json.parse(res.body())).get("content"))) {
                Map<String, Object> b = Json.asMap(block);
                if ("text".equals(b.get("type")) && b.get("text") instanceof String t) out.append(t);
            }
            if (out.length() == 0) throw new AiException("The AI returned an empty reply.");
            return out.toString();
        } catch (IOException e) {
            throw new AiException("Could not reach the AI service. Check your internet connection.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AiException("The AI request was interrupted.", e);
        } catch (IllegalArgumentException e) {
            throw new AiException("The AI reply could not be understood.", e);
        }
    }
}
