package ph.studytabai.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * ABSTRACTION: defines what every question can do without saying how it is checked.
 * ENCAPSULATION: all state is private; setters validate.
 * Subclasses (INHERITANCE) override isCorrect / toStudentView (POLYMORPHISM).
 */
@SuppressWarnings("serial")
public abstract class Question implements Serializable, Identifiable {
    private static final long serialVersionUID = 1L;

    private final String id = UUID.randomUUID().toString();
    private String prompt;
    private int points = 1000;
    private int timeLimitSec = 20;
    private String imagePath;
    private String explanation;
    private final List<String> standardCodes = new ArrayList<>();

    protected Question(String prompt) {
        setPrompt(prompt);
    }

    // ----- behavior that differs per question type -----
    public abstract boolean isCorrect(Answer answer);
    public abstract QuestionView toStudentView();
    public abstract String getType();
    /** Human-readable correct answer, shown to the student after they answer. */
    public abstract String getCorrectAnswerText();

    // ----- shared behavior -----
    /** Score using the question's own time limit. */
    public final int score(Answer answer) {
        return score(answer, timeLimitSec);
    }

    /**
     * Wrong or missing answer = 0. Correct answers earn 50%-100% of the points,
     * depending on how fast they were (effective limit supports extended-time accommodations).
     */
    public final int score(Answer answer, int effectiveLimitSec) {
        if (answer == null || !isCorrect(answer)) return 0;
        double limitMs = Math.max(1, effectiveLimitSec) * 1000.0;
        double ratio = Math.min(1.0, answer.getTimeMs() / limitMs);
        return (int) Math.round(points * (1.0 - 0.5 * ratio));
    }

    // ----- encapsulated state -----
    @Override public String getId() { return id; }

    public String getPrompt() { return prompt; }
    public final void setPrompt(String prompt) {
        if (prompt == null || prompt.isBlank()) throw new IllegalArgumentException("Prompt is required");
        if (prompt.length() > 2000) throw new IllegalArgumentException("Prompt too long (max 2000)");
        this.prompt = prompt.trim();
    }

    public int getPoints() { return points; }
    public void setPoints(int points) {
        if (points < 0 || points > 10000) throw new IllegalArgumentException("Points must be 0-10000");
        this.points = points;
    }

    public int getTimeLimitSec() { return timeLimitSec; }
    public void setTimeLimitSec(int sec) {
        if (sec < 5 || sec > 600) throw new IllegalArgumentException("Time limit must be 5-600 seconds");
        this.timeLimitSec = sec;
    }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }

    public List<String> getStandardCodes() { return Collections.unmodifiableList(new ArrayList<>(standardCodes)); }
    public void addStandardCode(String code) {
        if (code == null || code.isBlank()) throw new IllegalArgumentException("Standard code is required");
        if (!standardCodes.contains(code.trim())) standardCodes.add(code.trim());
    }
    public void removeStandardCode(String code) { standardCodes.remove(code); }
}
