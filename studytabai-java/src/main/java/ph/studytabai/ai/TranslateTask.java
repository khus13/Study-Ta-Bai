package ph.studytabai.ai;

import java.util.Map;

public final class TranslateTask extends TextTask {
    private final String language;

    public TranslateTask(String text, String language) {
        super(text);
        this.language = language == null || language.isBlank() ? "English" : language.trim();
    }

    @Override protected String promptName() { return "translate"; }
    @Override protected Map<String, String> variables() { return Map.of("language", language); }
}
