package ph.studytabai.ai;

import java.util.Map;

/**
 * ABSTRACTION + template method: every AI feature builds a prompt, calls the model, and parses the reply.
 * Subclasses (INHERITANCE) supply the template name, variables, content and parser (POLYMORPHISM).
 * Lesson text is sent in the USER message inside <lesson> tags, and the system prompt tells the model to
 * treat it as data, which limits prompt-injection from uploaded documents.
 */
public abstract class AiTask<R> {
    protected static final int MAX_CONTENT_CHARS = 12_000;

    protected abstract String promptName();
    protected abstract Map<String, String> variables();
    protected abstract String content();
    protected abstract R parse(String rawReply) throws AiException;

    public final R run(AiClient client) throws AiException {
        String system = PromptLibrary.render(promptName(), variables());
        String user = "<lesson>\n" + clip(content()) + "\n</lesson>";
        return parse(client.complete(system, user));
    }

    protected static String clip(String s) {
        s = s == null ? "" : s.trim();
        return s.length() > MAX_CONTENT_CHARS ? s.substring(0, MAX_CONTENT_CHARS) : s;
    }
}
