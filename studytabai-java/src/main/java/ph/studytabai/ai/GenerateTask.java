package ph.studytabai.ai;

import java.util.Map;

/** Topic and/or lesson -> curriculum-aligned resource (worksheet, lesson plan, study guide...). */
public final class GenerateTask extends TextTask {
    private final String resourceType, gradeLevel;

    public GenerateTask(String topicOrLesson, String resourceType, String gradeLevel) {
        super(topicOrLesson);
        this.resourceType = resourceType;
        this.gradeLevel = gradeLevel;
    }

    @Override protected String promptName() { return "generate"; }
    @Override protected Map<String, String> variables() { return Map.of("resourceType", resourceType, "gradeLevel", gradeLevel); }
}
