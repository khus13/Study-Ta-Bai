package ph.studytabai.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Free-text answer, matched ignoring case, extra spaces, and surrounding punctuation. */
@SuppressWarnings("serial")
public final class ShortAnswerQuestion extends Question {
    private static final long serialVersionUID = 1L;

    private final List<String> acceptedAnswers = new ArrayList<>();

    public ShortAnswerQuestion(String prompt, List<String> acceptedAnswers) {
        super(prompt);
        setAcceptedAnswers(acceptedAnswers);
    }

    public void setAcceptedAnswers(List<String> answers) {
        if (answers == null || answers.stream().noneMatch(s -> s != null && !s.isBlank()))
            throw new IllegalArgumentException("At least one accepted answer is required");
        acceptedAnswers.clear();
        for (String s : answers) if (s != null && !s.isBlank()) acceptedAnswers.add(s.trim());
    }

    public List<String> getAcceptedAnswers() { return Collections.unmodifiableList(acceptedAnswers); }

    static String normalize(String s) {
        return s.toLowerCase(Locale.ROOT).replaceAll("^[\\p{Punct}\\s]+|[\\p{Punct}\\s]+$", "").replaceAll("\\s+", " ");
    }

    @Override
    public boolean isCorrect(Answer a) {
        if (a.getText() == null) return false;
        String given = normalize(a.getText());
        return acceptedAnswers.stream().anyMatch(ok -> normalize(ok).equals(given));
    }

    @Override
    public QuestionView toStudentView() {
        return new QuestionView(getId(), getType(), getPrompt(), List.of(), getPoints(), getTimeLimitSec(), getImagePath());
    }

    @Override public String getType() { return "SHORT"; }
    @Override public String getCorrectAnswerText() { return acceptedAnswers.get(0); }
}
