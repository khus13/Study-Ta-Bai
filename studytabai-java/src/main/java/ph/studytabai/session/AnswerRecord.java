package ph.studytabai.session;

import java.io.Serializable;

/** Immutable record of one graded answer. */
public final class AnswerRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String questionId;
    private final int optionIndex;
    private final String text;
    private final boolean correct;
    private final int points;
    private final long timeMs;

    public AnswerRecord(String questionId, int optionIndex, String text, boolean correct, int points, long timeMs) {
        this.questionId = questionId;
        this.optionIndex = optionIndex;
        this.text = text;
        this.correct = correct;
        this.points = points;
        this.timeMs = timeMs;
    }

    public String getQuestionId() { return questionId; }
    public int getOptionIndex() { return optionIndex; }
    public String getText() { return text; }
    public boolean isCorrect() { return correct; }
    public int getPoints() { return points; }
    public long getTimeMs() { return timeMs; }
}
