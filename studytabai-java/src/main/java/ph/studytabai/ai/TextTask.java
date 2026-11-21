package ph.studytabai.ai;

/** Base for Wayground AI tasks whose result is plain editable text. */
public abstract class TextTask extends AiTask<String> {
    protected final String source;

    protected TextTask(String source) { this.source = source == null ? "" : source; }

    @Override protected String content() { return source; }

    @Override
    protected String parse(String raw) throws AiException {
        String t = raw.trim();
        if (t.isEmpty()) throw new AiException("The AI returned an empty reply.");
        return t;
    }
}
