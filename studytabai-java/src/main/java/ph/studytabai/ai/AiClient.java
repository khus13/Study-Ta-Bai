package ph.studytabai.ai;

/** ABSTRACTION over any LLM provider. */
public interface AiClient {
    String complete(String systemPrompt, String userMessage) throws AiException;
}
