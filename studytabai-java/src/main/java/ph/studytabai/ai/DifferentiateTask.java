package ph.studytabai.ai;

import java.util.Map;

/** Material -> easier / on-level / advanced versions. */
public final class DifferentiateTask extends TextTask {
    public DifferentiateTask(String material) { super(material); }
    @Override protected String promptName() { return "differentiate"; }
    @Override protected Map<String, String> variables() { return Map.of(); }
}
