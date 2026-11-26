package ph.studytabai.ai;

import java.util.Map;

public final class ReadingLevelTask extends TextTask {
    private final String level;

    public ReadingLevelTask(String text, String level) {
        super(text);
        this.level = level;
    }

    @Override protected String promptName() { return "reading_level"; }
    @Override protected Map<String, String> variables() { return Map.of("level", level); }
}
