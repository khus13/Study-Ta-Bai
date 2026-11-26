package ph.studytabai.model;

import java.util.List;

/** Option index 0 = True, 1 = False. */
public final class TrueFalseQuestion extends Question {
    private static final long serialVersionUID = 1L;

    private boolean correctValue;

    public TrueFalseQuestion(String prompt, boolean correctValue) {
        super(prompt);
        this.correctValue = correctValue;
    }

    public boolean getCorrectValue() { return correctValue; }
    public void setCorrectValue(boolean v) { this.correctValue = v; }

    @Override
    public boolean isCorrect(Answer a) {
        return a.getOptionIndex() == (correctValue ? 0 : 1);
    }

    @Override
    public QuestionView toStudentView() {
        return new QuestionView(getId(), getType(), getPrompt(), List.of("True", "False"),
                getPoints(), getTimeLimitSec(), getImagePath());
    }

    @Override public String getType() { return "TF"; }
    @Override public String getCorrectAnswerText() { return correctValue ? "True" : "False"; }
}
